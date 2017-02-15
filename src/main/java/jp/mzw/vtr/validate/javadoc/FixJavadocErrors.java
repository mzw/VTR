package jp.mzw.vtr.validate.javadoc;

import java.io.*;
import java.util.*;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.JavadocUtils.JavadocErrorMessage;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

import jp.mzw.vtr.validate.ValidationResult;
import jp.mzw.vtr.validate.ValidatorBase;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.tidy.Tidy;

public class FixJavadocErrors extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(FixJavadocErrors.class);

	public FixJavadocErrors(Project project) {
		super(project);
	}

	protected Map<String, String> modifyMap = new HashMap<>();

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		List<JavadocErrorMessage> messages = results.getJavadocErrorMessages(projectDir, tc.getTestFile());
		if (messages == null) {
			return ret;
		}
		if (!messages.isEmpty()) {
			boolean detect = false;
			for (JavadocErrorMessage message : messages) {
				if (tc.getStartLineNumber() <= message.getLineno() && message.getLineno() <= tc.getEndLineNumber()) {
					detect = true;
					break;
				}
			}
			if (detect) {
				ret.add(tc.getMethodDeclaration());
			}
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> detects = detect(commit, tc, results);
		if (detects.isEmpty()) {
			return origin;
		}
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		// get method declarations
		final List<MethodDeclaration> methods = new ArrayList<>();
		tc.getCompilationUnit().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				methods.add(node);
				return super.visit(node);
			}
		});
		// relate JavaDoc error messages to method declarations
		final List<JavadocErrorMessage> messages = results.getJavadocErrorMessages(projectDir, tc.getTestFile());
		for (MethodDeclaration method : methods) {
			if (method.getJavadoc() == null) {
				continue;
			}
			int start = tc.getCompilationUnit().getLineNumber(method.getStartPosition());
			int end = tc.getCompilationUnit().getLineNumber(method.getStartPosition() + method.getLength());
			for (JavadocErrorMessage message : messages) {
				if (start <= message.getLineno() && message.getLineno() <= end) {
					message.setMethod(method);
				}
			}
		}
		// rewrite
		for (JavadocErrorMessage message : messages) {
			ASTRewrite rewrite = ASTRewrite.create(ast);
			if (message.getMethod() == null) {
				continue;
			}
			if (!message.getMethod().equals(tc.getMethodDeclaration())) {
				continue;
			}
			if (message.getDescription().startsWith("no @throws for ")) {
				String exception = message.getDescription().replace("no @throws for ", "");
				final List<ITypeBinding> exceptions = new ArrayList<>();
				for (Object object : message.getMethod().thrownExceptionTypes()) {
					if (object instanceof SimpleType) {
						SimpleType type = (SimpleType) object;
						exceptions.add(type.resolveBinding());
					} else {
						System.out.println("TODO: implement for thrown exception type, " + object.getClass());
					}
				}
				TagElement tag = null;
				for (ITypeBinding type : exceptions) {
					if (exception.equals(type.getQualifiedName())) {
						tag = ast.newTagElement();
						tag.setTagName(TagElement.TAG_THROWS);
						SimpleName name = ast.newSimpleName(type.getName());
						tag.fragments().add(name);
						TextElement text = ast.newTextElement();
						text.setText("May occur in some failure modes");
						tag.fragments().add(text);
						break;
					}
				}
				// can't resolve type bindings of imported exceptions,
				// so tag remains null
				if (tag == null) {
					String[] packages = exception.split("\\.");
					String exceptionName = packages[packages.length - 1];
					tag = ast.newTagElement();
					tag.setTagName(TagElement.TAG_THROWS);
					SimpleName name = ast.newSimpleName(exceptionName);
					tag.fragments().add(name);
					TextElement text = ast.newTextElement();
					text.setText("TODO: Add description for this exception");
					tag.fragments().add(text);
				}
				Javadoc javadoc = message.getMethod().getJavadoc();
				Javadoc copy = (Javadoc) ASTNode.copySubtree(ast, javadoc);
				copy.tags().add(tag);
				rewrite.replace(javadoc, copy, null);
				String modified = getModified(origin, rewrite);
				modifyMap.put("NoAtThrowsFor", modified);
			} else if (message.getDescription().startsWith("no description for @throws")) {
				Javadoc javadoc = message.getMethod().getJavadoc();
				Javadoc copy = (Javadoc) ASTNode.copySubtree(ast, javadoc);
				for (Object content : copy.tags()) {
					TagElement tag = (TagElement) content;
					if (tag.getTagName() == null) {
						continue;
					}
					if (!tag.getTagName().equals("@throws")) {
						continue;
					}
					TextElement text = ast.newTextElement();
					text.setText("TODO: add description for @throws");
					tag.fragments().add(text);
				}
				rewrite.replace(javadoc, copy, null);
				String modified = getModified(origin, rewrite);
				modifyMap.put("NoDescriptionForAtThrows", modified);
			} else if (message.getDescription().startsWith("unknown tag")) {
				Javadoc javadoc = message.getMethod().getJavadoc();
				for (Object content : javadoc.tags()) {
					TagElement tag = (TagElement) content;
					if (tag.getTagName() == null) {
						continue;
					}
					if (officialTag(tag.getTagName())) {
						continue;
					} else if (todoTag(tag.getTagName())) {
						// we've already dealt with todo tag in ReplaceAtTodoWithTODO
						continue;
					} else {
						System.out.println("TODO: " + "unknown tag " + tag.getTagName());
					}
				}
			} else if (message.getDescription().startsWith("element not closed")) {
				Javadoc javadoc = message.getMethod().getJavadoc();
				Javadoc copy = (Javadoc) ASTNode.copySubtree(ast, javadoc);
				copy.tags().clear();
				for (Object comment : javadoc.tags()) {
					if (!comment.toString().contains("<") || !comment.toString().contains(">")) {
						TagElement element = (TagElement) ASTNode.copySubtree(ast, (TagElement) comment);
						copy.tags().add(element);
						continue;
					}
					TagElement tag = ast.newTagElement();
					TextElement text = ast.newTextElement();
					String content = getTidyModify(comment.toString());
					text.setText(content);
					tag.fragments().add(text);
					copy.tags().add(tag);
				}
				rewrite.replace(javadoc, copy, null);
				String modified = getModified(origin, rewrite);
				modifyMap.put("ElementNotClosed", modified);
			} else if (message.getDescription().startsWith("bad use of")) {
				Javadoc javadoc = message.getMethod().getJavadoc();
				Javadoc copy = (Javadoc) ASTNode.copySubtree(ast, javadoc);
				copy.tags().clear();
				for (Object comment : javadoc.tags()) {
					if (!hasSpecialCharacter(comment.toString())) {
						TagElement element = (TagElement) ASTNode.copySubtree(ast, (TagElement) comment);
						copy.tags().add(element);
						continue;
					}
					TagElement tag = ast.newTagElement();
					TextElement text = ast.newTextElement();
					String content = getTidyModify(comment.toString());
					text.setText(content);
					tag.fragments().add(text);
					copy.tags().add(tag);
				}
				rewrite.replace(javadoc, copy, null);
				String modified = getModified(origin, rewrite);
				modifyMap.put("BadUseOf", modified);
			} else if (message.getDescription().startsWith("nested tag not allowed")) {
				Javadoc javadoc = message.getMethod().getJavadoc();
				Javadoc copy = (Javadoc) ASTNode.copySubtree(ast, javadoc);
				copy.tags().clear();
				for (Object comment : javadoc.tags()) {
					TagElement tag = ast.newTagElement();
					TextElement text = ast.newTextElement();
					String content = getTidyModify(comment.toString());
					text.setText(content);
					tag.fragments().add(text);
					copy.tags().add(tag);
				}
				rewrite.replace(javadoc, copy, null);
				String modified = getModified(origin, rewrite);
				modifyMap.put("NestedTagNotAllowed", modified);
			} else if (message.getDescription().startsWith("exception not thrown")) {
				Javadoc javadoc = message.getMethod().getJavadoc();
				Javadoc copy = (Javadoc) ASTNode.copySubtree(ast, javadoc);
				Iterator itr = copy.tags().iterator();
				while(itr.hasNext()) {
					TagElement tag = (TagElement) itr.next();
					if (targetTagElement(cu, tag, message)) {
						itr.remove();
					}
				}
				rewrite.replace(javadoc, copy, null);
				String modified = getModified(origin, rewrite);
				modifyMap.put("ExceptionNotThrown", modified);
			} else if (message.getDescription().startsWith("bad HTML entity")) {
					Javadoc javadoc = message.getMethod().getJavadoc();
					Javadoc copy = (Javadoc) ASTNode.copySubtree(ast, javadoc);
					copy.tags().clear();
					for (Object comment : javadoc.tags()) {
						TagElement tag = ast.newTagElement();
						TextElement text = ast.newTextElement();
						String content = getTidyModify(comment.toString());
						text.setText(content);
						tag.fragments().add(text);
						copy.tags().add(tag);
					}
					rewrite.replace(javadoc, copy, null);
					String modified = getModified(origin, rewrite);
					modifyMap.put("BadHTMLEntity", modified);
			} else {
				System.out.println("TODO: " + message.toString());
			}
		}
		// modify
		Document document = new Document(origin);
		return document.get();
	}

	public String getModified(String origin, ASTRewrite rewrite) throws BadLocationException {
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

	protected String getTidyModify(String comment) throws IOException {
		Tidy tidy = new Tidy();
		tidy.setQuiet(true);
		tidy.setShowErrors(0);
		tidy.setShowWarnings(false);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		tidy.parse(new ByteArrayInputStream(comment.getBytes("utf-8")), output);
		String content = getBody(output.toString()).replace("* ", "");
		output.close();
		content = content.replace("\n", "\n* ");
		return content;
	}

	private boolean hasSpecialCharacter(String comment) {
		return (comment.contains("<") && !comment.contains(">")) ||
				(comment.contains(">") && !comment.contains("<"));
	}

	protected boolean targetTagElement(CompilationUnit cu, TagElement tag, JavadocErrorMessage message) {
		return (cu.getLineNumber(tag.getStartPosition()) <= message.getLineno()) &&
				(message.getLineno() <= cu.getLineNumber(tag.getStartPosition() + tag.getLength()));
	}

	@Override
	public void generate(ValidationResult result) {
		try {
			// Read
			Commit commit = new Commit(result.getCommitId(), null);
			TestCase testcase = getTestCase(result, projectDir);
			Results results = Results.parse(outputDir, projectId, commit);
			// Generate
			String origin = FileUtils.readFileToString(testcase.getTestFile());
			getModified(origin, commit, testcase, results);
			for (Map.Entry<String, String> map : this.modifyMap.entrySet()) {
				List<String> patch = genPatch(origin, map.getValue(), testcase.getTestFile(), testcase.getTestFile());
				// No modification
				if (NoModification(patch)) {
					return;
				}
				output(result, testcase, patch, map.getKey());
			}
		} catch (IOException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}

	protected void output(ValidationResult result, TestCase tc, List<String> patch, String modifyType) throws IOException {
		File projectDir = new File(this.outputDir, this.projectId);
		File validateDir = new File(projectDir, ValidatorBase.VALIDATOR_DIRNAME);
		File commitDir = new File(validateDir, result.getCommitId());
		File patternDir = new File(commitDir, result.getValidatorName());
		if (!patternDir.exists()) {
			patternDir.mkdirs();
		}
		File patchFile = new File(patternDir, tc.getFullName() + "#" + modifyType + ".patch");
		FileUtils.writeLines(patchFile, patch);
	}

	private boolean officialTag(String tagName) {
		return tagName.equals("@author") || tagName.equals("@code") || tagName.equals("@deprecated") ||
				tagName.equals("@docRoot") || tagName.equals("exception") || tagName.equals("inheritDoc") ||
				tagName.equals("@link") || tagName.equals("@linkplain") || tagName.equals("@literal") ||
				tagName.equals("@param") || tagName.equals("@return") || tagName.equals("@see") ||
				tagName.equals("@throws");
	}

	private boolean todoTag(String tagName) {
		return tagName.equals("@todo") || tagName.equals("@TODO");
	}

	private String getBody(String html) {
		return html.split("<body>")[1].split("</body>")[0];
	}
}

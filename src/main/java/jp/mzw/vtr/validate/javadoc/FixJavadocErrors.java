package jp.mzw.vtr.validate.javadoc;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.JavadocUtils.JavadocErrorMessage;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

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

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
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
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> detects = detect(commit, tc, results);
		if (detects.isEmpty()) {
			return origin;
		}
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		MethodDeclaration method = tc.getMethodDeclaration();
		// relate JavaDoc error messages to this method declarations
		final List<JavadocErrorMessage> messages = results.getJavadocErrorMessages(projectDir, tc.getTestFile());
		if (method.getJavadoc() == null) {
			return origin;
		}
		int start = tc.getCompilationUnit().getLineNumber(method.getStartPosition());
		int end = tc.getCompilationUnit().getLineNumber(method.getStartPosition() + method.getLength());
		for (JavadocErrorMessage message : messages) {
			if (start <= message.getLineno() && message.getLineno() <= end) {
				message.setMethod(method);
			}
		}

		List<JavadocErrorMessage> firstProcessMessages = new ArrayList<>();
		List<JavadocErrorMessage> secondProcessMessages = new ArrayList<>();
		List<JavadocErrorMessage> thirdProcessMessages = new ArrayList<>();
		for (JavadocErrorMessage message : messages) {
			if (message.getMethod() == null) {
				continue;
			}
			if (limitationMessage(message)) {
				if (compileError(results)) {
					return origin;
				}
			} else if (firstProcessMessages(message)) {
				firstProcessMessages.add(message);
			} else if (secondProcessMessages(message)) {
				secondProcessMessages.add(message);
			} else if (thirdProcessMessages(message)) {
				thirdProcessMessages.add(message);
			} else {
				printTODO(commit, message);
			}
		}
		List<TagElement> removed = new ArrayList<>();
		List<TagElement> modified = new ArrayList<>();

		// rewrite
		Javadoc javadoc = method.getJavadoc();
		for (JavadocErrorMessage message : firstProcessMessages) {
			// add or delete javadoc tags
			// 既存のASTに操作
			if (message.getDescription().startsWith("no @throws for ")) {
				TagElement target = NoAtThrowsFor(ast, message);
				javadoc.tags().add(target);
			} else if (message.getDescription().startsWith("exception not thrown")) {
				@SuppressWarnings("rawtypes")
				Iterator itr = javadoc.tags().iterator();
				while (itr.hasNext()) {
					TagElement target = (TagElement) itr.next();
					if (targetTagElement(cu, target, message)) {
						removed.add(target);
						itr.remove();
					}
				}
			} else if (message.getDescription().startsWith("no @param for ")) {
				TagElement tag = NotAtParamFor(ast, message);
				javadoc.tags().add(tag);
			}
		}
		List<TagElement> secondProcessTargets = new ArrayList<>();
		List<TagElement> thirdProcessTargets = new ArrayList<>();
		for (Object obj : javadoc.tags()) {
			TagElement tag = (TagElement) obj;
			for (JavadocErrorMessage message : secondProcessMessages) {
				if (targetTagElement(cu, tag, message)) {
					secondProcessTargets.add(tag);
				}
			}
			for (JavadocErrorMessage message : thirdProcessMessages) {
				if (targetTagElement(cu, tag, message)) {
					thirdProcessTargets.add(tag);
				}
			}
		}
		Javadoc copy = (Javadoc) ASTNode.copySubtree(ast, javadoc);
		copy.tags().clear();
		for (Object obj : javadoc.tags()) {
			TagElement tag = (TagElement) obj;
			if (secondProcessTargets.contains(tag)) {
				for (JavadocErrorMessage message : secondProcessMessages) {
					// modify @see and @link
					TagElement target = targetTagElement(cu, message);
					if (removed.contains(target)) {
						continue;
					}
					if (modified.contains(target)) {
						continue;
					}
					if (message.getDescription().startsWith("no description for @throws") ||
							message.getDescription().startsWith("no description for @exception")) {
						TagElement replace = NoDescriptionForAtThrows(ast, cu, message);
						modified.add(target);
						copy.tags().add(replace);
					} else if (message.getDescription().startsWith("unknown tag")) {
						TagElement replace = UnknownTag(cu, message);
						if (replace == null) {
							System.out.println("TODO: Check why replace is null");
							printTODO(commit, message);
							continue;
						}
					} else if (message.getDescription().startsWith("unexpected text")) {
						TagElement replace = UnexpectedText(ast, cu, message);
						if (replace == null) {
							System.out.println("TODO: Check why replace is null");
							printTODO(commit, message);
							continue;
						}
						modified.add(target);
						copy.tags().add(replace);
					} else if (message.getDescription().startsWith("incorrect use of inline tag")) {
						TagElement replace = IncorrectUseOfInlineTag(ast, cu, message);
						if (replace == null) {
							System.out.println("TODO: Check why replace is null");
							printTODO(commit, message);
							continue;
						}
						modified.add(target);
						copy.tags().add(replace);
					}
				}
			} else if (thirdProcessTargets.contains(tag)) {
				for (JavadocErrorMessage message : thirdProcessMessages) {
					// Tidy
					TagElement target = targetTagElement(cu, message);
					if (removed.contains(target)) {
						continue;
					}
					if (modified.contains(target)) {
						continue;
					}
					TagElement replace = ModifyByTidy(ast, cu, message);
					copy.tags().add(replace);
				}
			} else {
				copy.tags().add(ASTNode.copySubtree(ast, tag));
			}
		}
		rewrite.replace(javadoc, copy, null);
		// modify
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
		return content.replace("\n", " ").trim();
	}

	protected boolean targetTagElement(CompilationUnit cu, TagElement tag, JavadocErrorMessage message) {
		return (cu.getLineNumber(tag.getStartPosition()) <= message.getLineno())
				&& (message.getLineno() <= cu.getLineNumber(tag.getStartPosition() + tag.getLength()));
	}

	protected TagElement targetTagElement(CompilationUnit cu, JavadocErrorMessage message) {
		Javadoc javadoc = message.getMethod().getJavadoc();
		for (Object obj : javadoc.tags()) {
			TagElement tag = (TagElement) obj;
			if (targetTagElement(cu, tag, message)) {
				return tag;
			}
		}
		return null;
	}

	public boolean compileError(Results results) {
		if (!results.getCompileErrors().isEmpty()) {
			return true;
		} else {
			for (String message : results.getCompileOutputs()) {
				if (message.contains("COMPILATION ERROR")) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean officialTag(String tagName) {
		return tagName.equals(TagElement.TAG_AUTHOR) || tagName.equals(TagElement.TAG_CODE) || tagName.equals(TagElement.TAG_DEPRECATED)
				|| tagName.equals(TagElement.TAG_DOCROOT) || tagName.equals(TagElement.TAG_EXCEPTION) || tagName.equals(TagElement.TAG_INHERITDOC)
				|| tagName.equals(TagElement.TAG_LINK) || tagName.equals(TagElement.TAG_LINKPLAIN) || tagName.equals(TagElement.TAG_LITERAL)
				|| tagName.equals(TagElement.TAG_PARAM) || tagName.equals(TagElement.TAG_RETURN) || tagName.equals(TagElement.TAG_SEE)
				|| tagName.equals(TagElement.TAG_SERIAL) || tagName.equals(TagElement.TAG_SERIALDATA) || tagName.equals(TagElement.TAG_SERIALFIELD)
				|| tagName.equals(TagElement.TAG_SINCE) || tagName.equals(TagElement.TAG_THROWS) || tagName.equals(TagElement.TAG_VALUE)
				|| tagName.equals(TagElement.TAG_VERSION);
	}

	private boolean todoTag(String tagName) {
		return tagName.equals("@todo") || tagName.equals("@TODO");
	}

	private String getBody(String html) {
		return html.split("<body>")[1].split("</body>")[0];
	}

	// limitation to modify automatically
	private boolean limitationMessage(JavadocErrorMessage message) {
		return message.getDescription().startsWith("cannot find symbol") || message.getDescription().startsWith("can't find");
	}

	private boolean firstProcessMessages(JavadocErrorMessage message) {
		return message.getDescription().startsWith("no @throws for ")
				|| message.getDescription().startsWith("exception not thrown")
				|| message.getDescription().startsWith("no @param for");
	}

	private boolean secondProcessMessages(JavadocErrorMessage message) {
		return message.getDescription().startsWith("no description for @throws") || message.getDescription().startsWith("unknown tag")
				|| message.getDescription().startsWith("unexpected text") || message.getDescription().startsWith("incorrect use of inline tag")
				|| message.getDescription().startsWith("no description for @exception");
	}

	private boolean thirdProcessMessages(JavadocErrorMessage message) {
		return message.getDescription().startsWith("element not closed") || message.getDescription().startsWith("nested tag not allowed")
				|| message.getDescription().startsWith("bad HTML entity") || message.getDescription().startsWith("illegal character")
				|| message.getDescription().startsWith("bad use of") || message.getDescription().startsWith("malformed HTML")
				|| message.getDescription().startsWith("semicolon missing") || message.getDescription().startsWith("self-closing element not allowed")
				|| message.getDescription().startsWith("empty <p> tag") || message.getDescription().startsWith("unmappable character for encoding UTF8")
				|| message.getDescription().startsWith("unexpected end tag");
	}

	@SuppressWarnings("unchecked")
	protected TagElement NoAtThrowsFor(AST ast, JavadocErrorMessage message) {
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
		TagElement tag = ast.newTagElement();
		tag.setTagName(TagElement.TAG_THROWS);
		boolean created = false;
		for (ITypeBinding type : exceptions) {
			if (exception.equals(type.getQualifiedName())) {
				SimpleName name = ast.newSimpleName(type.getName());
				tag.fragments().add(name);
				TextElement text = ast.newTextElement();
				text.setText("May occur in some failure modes");
				tag.fragments().add(text);
				created = true;
				break;
			}
		}
		// can't resolve type bindings of imported exceptions,
		// so tag remains null
		if (!created) {
			String[] packages = exception.split("\\.");
			String exceptionName = packages[packages.length - 1];
			SimpleName name = ast.newSimpleName(exceptionName);
			tag.fragments().add(name);
			TextElement text = ast.newTextElement();
			text.setText("TODO: Add description for this exception");
			tag.fragments().add(text);
		}
		return tag;
	}

	@SuppressWarnings("unchecked")
	protected TagElement NotAtParamFor(AST ast, JavadocErrorMessage message) {
		String param = message.getDescription().replace("no @param for ", "");
		final List<SingleVariableDeclaration> params = new ArrayList<>();
		for (Object object : message.getMethod().parameters()) {
			if (object instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration var = (SingleVariableDeclaration) object;
				params.add(var);
			} else {
				System.out.println("TODO: implement for param type, " + object.getClass());
			}
		}
		TagElement tag = ast.newTagElement();
		tag.setTagName(TagElement.TAG_PARAM);
		for (SingleVariableDeclaration var : params) {
			if (param.equals(var.getName().toString())) {
				SimpleName name = ast.newSimpleName(var.getName().toString());
				tag.fragments().add(name);
				TextElement text = ast.newTextElement();
				text.setText("TODO: Add description for this param");
				tag.fragments().add(text);
			}
		}
		return tag;
	}

	@SuppressWarnings("unchecked")
	protected TagElement NoDescriptionForAtThrows(AST ast, CompilationUnit cu, JavadocErrorMessage message) {
		TagElement target = targetTagElement(cu, message);
		if (target == null) {
			return null;
		}
		if (target.getTagName() == null) {
			return null;
		}
		if (!(target.getTagName().equals("@throws") || target.getTagName().equals("@exception"))) {
			return null;
		}
		TagElement ret = (TagElement) ASTNode.copySubtree(ast, target);
		TextElement text = ast.newTextElement();
		text.setText("TODO: add description for this");
		ret.fragments().add(text);
		return ret;
	}

	protected TagElement UnknownTag(CompilationUnit cu, JavadocErrorMessage message) {
		TagElement target = targetTagElement(cu, message);
		if (target.getTagName() == null) {
			return null;
		}
		if (officialTag(target.getTagName())) {
			return null;
		} else if (todoTag(target.getTagName())) {
			// we've already dealt with todo tag in ReplaceAtTodoWithTODO
			return target;
		} else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	protected TagElement UnexpectedText(AST ast, CompilationUnit cu, JavadocErrorMessage message) {
		TagElement target = targetTagElement(cu, message);
		if (target == null) {
			return null;
		}
		TagElement ret = ast.newTagElement();
		for (Object obj : target.fragments()) {
			if (obj.toString().contains("http")) {
				TextElement text = ast.newTextElement();
				if (obj.toString().startsWith("{@link \"")) {
					// {@link "https://issues.apache.org/jira/browse/COLLECTIONS-360 COLLECTIONS-360"}
					String url = obj.toString().replace("{@link \"", "").replace("}", "");
					String html = createLink(url.split(" ")[0].trim());
					text.setText(html);
				} else if (obj.toString().startsWith("{@link ")) {
					// * {@link https://issues.apache.org/jira/browse/DIGESTER-151}
					String url = obj.toString().replace("{@link ", "").replace("}", "");
					String html = createLink(url.trim());
					text.setText(html);
				} else {
					// * @see https://issues.apache.org/jira/browse/COMPRESS-176
					String html = createLink(obj.toString().trim());
					text.setText(html);
				}
				ret.fragments().add(text);
			} else {
				ret.fragments().add(ASTNode.copySubtree(ast, (ASTNode) obj));
			}
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	protected TagElement IncorrectUseOfInlineTag(AST ast, CompilationUnit cu, JavadocErrorMessage message) {
		TagElement target = targetTagElement(cu, message);
		if (target.getTagName() == null) {
			return null;
		}
		if (!target.getTagName().equals(TagElement.TAG_LINK)) {
			return null;
		}
		TagElement ret = ast.newTagElement();
		for (Object obj : target.fragments()) {
			if (obj.toString().contains("http")) {
				TextElement text = ast.newTextElement();
				String html = createLink(obj.toString().replace("\"", "").trim());
				text.setText(html);
				ret.fragments().add(text);
			} else {
				ret.fragments().add(ASTNode.copySubtree(ast, (ASTNode) obj));
			}
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	protected TagElement ModifyByTidy(AST ast, CompilationUnit cu, JavadocErrorMessage message) throws IOException {
		TagElement target = targetTagElement(cu, message);
		if (target == null) {
			return null;
		}
		TagElement ret = ast.newTagElement();
		for (Object obj : target.fragments()) {
			TextElement text = ast.newTextElement();
			String content = getTidyModify(obj.toString());
			if (content.equals("")) {
				content = "\n *";
			}
			text.setText(content);
			ret.fragments().add(text);
		}
		return ret;
	}

	protected String createLink(String html) {
		return "<a href=\"" + html + "\">" + _createLink(html) + "</a>";
	}

	private String _createLink(String html) {
		Scanner sc = new Scanner(html);
		sc.useDelimiter("/");
		while (sc.hasNext()) {
			String link = sc.next();
			if (!sc.hasNext()) {
				return link;
			}
		}
		return "Link";
	}

	protected void printTODO(Commit commit, JavadocErrorMessage message) {
		System.out.println("TODO: " + message.toString());
		System.out.println("See: https://github.com/apache/" + this.projectId +
				"/blob/" + commit.getId() + "/" + message.toString().split(":")[0] +
				"#L" + message.getLineno());
	}
}

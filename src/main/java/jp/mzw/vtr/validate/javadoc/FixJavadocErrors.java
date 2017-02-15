package jp.mzw.vtr.validate.javadoc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.JavadocUtils.JavadocErrorMessage;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

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
		ASTRewrite rewrite = ASTRewrite.create(ast);
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
			} else if (message.getDescription().startsWith("unknown tag")) {
				Javadoc javadoc = message.getMethod().getJavadoc();
				for (Object content : javadoc.tags()) {
					TagElement tag = (TagElement) content;
					if (tag.getTagName() == null) {
						continue;
					}
					if (officialTag(tag.getTagName())) {
						continue;
					} else if (tag.getTagName().equals("@todo")) {
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
						if (!(comment instanceof TagElement)) {
							System.out.println("TODO: check the type of javadoc comment" + comment.getClass());
							continue;
						}
						TagElement element = (TagElement) ASTNode.copySubtree(ast, (TagElement) comment);
						copy.tags().add(element);
						continue;
					}
					Tidy tidy = new Tidy();
					tidy.setQuiet(true);
					tidy.setShowErrors(0);
					tidy.setShowWarnings(false);
					ByteArrayOutputStream output = new ByteArrayOutputStream();
					tidy.parse(new ByteArrayInputStream(comment.toString().getBytes("utf-8")), output);
					String content = getBody(output.toString()).replace("* ", "");
					content = content.replace("\n", "\n* ");
					TagElement tag = ast.newTagElement();
					TextElement text = ast.newTextElement();
					text.setText(content);
					tag.fragments().add(text);
					copy.tags().add(tag);
					output.close();
				}
				rewrite.replace(javadoc, copy, null);
			} else {
				System.out.println("TODO: " + message.toString());
			}
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

	private boolean officialTag(String tagName) {
		return tagName.equals("@author") || tagName.equals("@code") || tagName.equals("@deprecated") ||
				tagName.equals("@docRoot") || tagName.equals("exception") || tagName.equals("inheritDoc") ||
				tagName.equals("@link") || tagName.equals("@linkplain") || tagName.equals("@literal") ||
				tagName.equals("@param") || tagName.equals("@return") || tagName.equals("@see") ||
				tagName.equals("@throws");
	}


	private String getBody(String html) {
		return html.split("<body>")[1].split("</body>")[0];
	}
}

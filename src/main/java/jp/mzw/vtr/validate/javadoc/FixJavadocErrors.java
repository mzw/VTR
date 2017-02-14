package jp.mzw.vtr.validate.javadoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
				if (tag != null) {
					Javadoc javadoc = message.getMethod().getJavadoc();
					Javadoc copy = (Javadoc) ASTNode.copySubtree(ast, javadoc);
					copy.tags().add(tag);
					rewrite.replace(javadoc, copy, null);
				} else {
					System.out.println("TODO: " + "tag is null");
				}
			} else if (message.getDescription().startsWith("no description for @throws")) {
				Javadoc javadoc = message.getMethod().getJavadoc();
				Javadoc copy = (Javadoc) ASTNode.copySubtree(ast, javadoc);
				for (Object content : copy.tags()) {
				    TagElement tag = (TagElement) content;
				    if (!tag.getTagName().equals("@throws")) {
						continue;
					}
					TextElement text = ast.newTextElement();
				    text.setText("TODO: add description for @throws");
				    tag.fragments().add(text);
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
}

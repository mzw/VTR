package jp.mzw.vtr.validate.javadoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.MavenUtils.JavadocErrorMessage;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
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
	protected List<ASTNode> detect(TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		List<JavadocErrorMessage> messages = results.getJavadocErrorMessages(projectDir, tc.getTestFile());
		if (!messages.isEmpty()) {
			ret.add(tc.getMethodDeclaration());
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getModified(String origin, TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> detects = detect(tc);
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
			if (message.getMessage().startsWith("no @throws for ")) {
				String exception = message.getMessage().replace("no @throws for ", "");
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

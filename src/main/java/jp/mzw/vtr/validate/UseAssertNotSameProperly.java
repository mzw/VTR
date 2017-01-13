package jp.mzw.vtr.validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.TestCase;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseAssertNotSameProperly extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(UseAssertNotSameProperly.class);

	public UseAssertNotSameProperly(Project project) {
		super(project);
	}

	@Override
	protected boolean detect(TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		final List<MethodInvocation> targets = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				targets.add(node);
				return super.visit(node);
			}
		});
		for (MethodInvocation target : targets) {
			if ("assertTrue".equals(target.getName().toString())) {
				for (Object object : target.arguments()) {
					if (object instanceof InfixExpression) {
						InfixExpression expression = (InfixExpression) object;
						if ("!=".equals(expression.getOperator().toString())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getModified(String origin, TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		final List<MethodInvocation> targets = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				targets.add(node);
				return super.visit(node);
			}
		});
		for (MethodInvocation target : targets) {
			if ("assertTrue".equals(target.getName().toString())) {
				for (Object object : target.arguments()) {
					if (object instanceof InfixExpression) {
						InfixExpression expression = (InfixExpression) object;
						if ("!=".equals(expression.getOperator().toString())) {
							MethodInvocation replace = ast.newMethodInvocation();
							replace.setName(ast.newSimpleName(target.getName().toString().replace("assertTrue", "assertNotSame")));
							replace.arguments().add(ASTNode.copySubtree(ast, expression.getLeftOperand()));
							replace.arguments().add(ASTNode.copySubtree(ast, expression.getRightOperand()));
							rewrite.replace(target, replace, null);
						}
					}
				}
			}
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

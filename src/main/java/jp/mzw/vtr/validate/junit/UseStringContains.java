package jp.mzw.vtr.validate.junit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

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

public class UseStringContains extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(UseStringContains.class);

	public UseStringContains(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(Commit commit, TestCase tc, Results results) throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> ret = new ArrayList<>();
		final List<MethodInvocation> targets = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				targets.add(node);
				return super.visit(node);
			}
		});
		for (MethodInvocation target : targets) {
			if ("indexOf".equals(target.getName().toString())) {
				if (target.getParent() instanceof InfixExpression) {
					InfixExpression expression = (InfixExpression) target.getParent();
					if ("!=".equals(expression.getOperator().toString()) && "-1".equals(expression.getRightOperand().toString())) {
						ret.add(target);
					}
				}
			}
		}
		return ret;
	}

	@Override
	protected String getModified(String origin, Commit commit, TestCase tc, Results results) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode node : detect(commit, tc, results)) {
			MethodInvocation method = (MethodInvocation) node;
			InfixExpression target = (InfixExpression) node.getParent();
			MethodInvocation replace = ast.newMethodInvocation();
			replace = (MethodInvocation) ASTNode.copySubtree(ast, method);
			replace.setName(ast.newSimpleName(method.getName().toString().replace("indexOf", "contains")));
			rewrite.replace(target, replace, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

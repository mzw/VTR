package jp.mzw.vtr.validate.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.CompileValidatorBase;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseProcessWaitfor extends CompileValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(UseProcessWaitfor.class);

	public UseProcessWaitfor(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		final List<WhileStatement> whileStatements = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(WhileStatement node) {
				whileStatements.add(node);
				return super.visit(node);
			}
		});
		for (WhileStatement whileStatement : whileStatements) {
			if (isTargetNode(whileStatement)) {
				ret.add(whileStatement);
			}
		}
		return ret;
	}

	@Override
	protected String getModified(String origin, TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode node : detect(tc)) {
			WhileStatement target = (WhileStatement) node;
			// create
			MethodInvocation isAlive = (MethodInvocation) target.getExpression();
			MethodInvocation waitfor = ast.newMethodInvocation();
			waitfor = (MethodInvocation) ASTNode.copySubtree(ast, isAlive);
			waitfor.setName(ast.newSimpleName("waitfor"));
			waitfor.arguments().clear();
			Statement replace = ast.newExpressionStatement(waitfor);
			rewrite.replace(target, replace, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

	private boolean isTargetNode(WhileStatement whileStatement) {
		Expression expression = whileStatement.getExpression();
		Statement statement = whileStatement.getBody();
		return callProcessIsAlive(expression) && callThreadSleep(statement);
	}

	private boolean callProcessIsAlive(Expression expression) {
		if (!(expression instanceof MethodInvocation)) {
			return false;
		}
		MethodInvocation mi = (MethodInvocation) expression;
		Name methodName = mi.getName();
		if (!"isAlive".equals(methodName.toString())) {
			return false;
		}
		Expression methodExpression = mi.getExpression();
		ITypeBinding binding = methodExpression.resolveTypeBinding();
		while (binding != null) {
			ITypeBinding iType = binding.getTypeDeclaration();
			if ("java.lang.Process".equals(iType.getQualifiedName())) {
				return true;
			}
			binding = binding.getSuperclass();
		}
		return false;
	}

	private boolean callThreadSleep(Statement whileStatement) {
		Block block = (Block) whileStatement;
		for (Object statement : block.statements()) {
			if (!(statement instanceof ExpressionStatement))
				continue;
			ExpressionStatement expressionStatement = (ExpressionStatement) statement;
			MethodInvocation mi = (MethodInvocation) expressionStatement.getExpression();
			Name methodName = mi.getName();
			if (!"sleep".equals(methodName.toString())) {
				return false;
			}
			Expression methodExpression = mi.getExpression();
			ITypeBinding binding = methodExpression.resolveTypeBinding();
			while (binding != null) {
				ITypeBinding iType = binding.getTypeDeclaration();
				if ("java.lang.Thread".equals(iType.getQualifiedName())) {
					return true;
				}
				binding = binding.getSuperclass();
			}
		}
		return false;
	}
}

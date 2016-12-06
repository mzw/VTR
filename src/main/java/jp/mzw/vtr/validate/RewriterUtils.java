package jp.mzw.vtr.validate;

import java.util.List;

import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class RewriterUtils {

	/**
	 * Remove catches from try statement at method
	 * 
	 * @param rewriter
	 * @param tryStatement
	 * @param catches
	 * @param method
	 */
	public static void removeCatches(ASTRewrite rewriter, TryStatement tryStatement, List<CatchClause> catches, MethodDeclaration method) {
		boolean all = true;
		for (Object object : tryStatement.catchClauses()) {
			CatchClause raw = (CatchClause) object;
			boolean equals = false;
			for (CatchClause target : catches) {
				if (raw.equals(target)) {
					equals = true;
					break;
				}
			}
			if (!equals) {
				all = false;
				break;
			}
		}
		if (all) {
			ListRewrite lr = rewriter.getListRewrite(method.getBody(), Block.STATEMENTS_PROPERTY);
			for (ASTNode statement : MavenUtils.getChildren(tryStatement.getBody())) {
				if (statement instanceof ExpressionStatement) {
					ExpressionStatement expr = (ExpressionStatement) statement;
					if (expr.getExpression() instanceof MethodInvocation) {
						MethodInvocation call = (MethodInvocation) expr.getExpression();
						if (call.getName().toString().equals("fail")) {
							break;
						}
					}
				}
				lr.insertBefore(statement, tryStatement, null);
			}
			rewriter.remove(tryStatement, null);
		} else {
			for (CatchClause target : catches) {
				rewriter.remove(target, null);
			}
		}
	}

	/**
	 * Insert new exception at thrown exception types at method
	 * 
	 * @param rewriter
	 * @param target
	 * @param exceptions
	 * @param method
	 */
	public static void insertException(ASTRewrite rewriter, CatchClause target, List<SimpleType> exceptions, MethodDeclaration method) {
		boolean exist = false;
		for (SimpleType exception : exceptions) {
			if (target.getException().getType().toString().equals(exception.toString())) {
				exist = true;
			}
		}
		if (!exist) {
			ListRewrite lr = rewriter.getListRewrite(method, MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY);
			lr.insertLast(target.getException().getType(), null);
		}
	}

	/**
	 * Get catch clause
	 * 
	 * @param tc
	 * @param start
	 * @param end
	 * @return
	 */
	public static CatchClause getCatchClause(TestCase tc, int start, int end) {
		for (ASTNode node : tc.getNodes()) {
			if (start == tc.getStartLineNumber(node) && end == tc.getEndLineNumber(node)) {
				if (node instanceof CatchClause) {
					return (CatchClause) node;
				}
			}
		}
		return null;
	}
}

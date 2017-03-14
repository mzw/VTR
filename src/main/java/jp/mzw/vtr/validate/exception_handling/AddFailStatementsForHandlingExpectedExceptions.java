package jp.mzw.vtr.validate.exception_handling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;
import jp.mzw.vtr.validate.ValidatorBase;
import jp.mzw.vtr.validate.ValidatorUtils;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddFailStatementsForHandlingExpectedExceptions extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(AddFailStatementsForHandlingExpectedExceptions.class);

	public AddFailStatementsForHandlingExpectedExceptions(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		if (Version.parse("4").compareTo(ValidatorBase.getJunitVersion(projectDir)) < 0) {
			return ret;
		}
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(TryStatement node) {
				// at body
				final List<MethodInvocation> assertsAtBody = new ArrayList<>();
				node.getBody().accept(new ASTVisitor() {
					@Override
					public boolean visit(MethodInvocation node) {
						boolean isJunitAssertMethod = false;
						for (String junit : ValidatorUtils.JUNIT_ASSERT_METHODS) {
							if (junit.equals(node.getName().toString())) {
								isJunitAssertMethod = true;
								break;
							}
						}
						if (isJunitAssertMethod) {
							assertsAtBody.add(node);
						}
						return super.visit(node);
					}
				});
				// at catches
				final List<MethodInvocation> assertsAtCatches = new ArrayList<>();
				final List<ThrowStatement> throwsAtCatches = new ArrayList<>();
				for (Object obeject : node.catchClauses()) {
					CatchClause cc = (CatchClause) obeject;
					cc.accept(new ASTVisitor() {
						@Override
						public boolean visit(MethodInvocation node) {
							boolean isJunitAssertMethod = false;
							for (String junit : ValidatorUtils.JUNIT_ASSERT_METHODS) {
								if (junit.equals(node.getName().toString())) {
									isJunitAssertMethod = true;
									break;
								}
							}
							if (isJunitAssertMethod) {
								assertsAtCatches.add(node);
							}
							return super.visit(node);
						}

						@Override
						public boolean visit(ThrowStatement node) {
							throwsAtCatches.add(node);
							return super.visit(node);
						}
					});
				}
				// at finally
				final List<MethodInvocation> assertsAtFinally = new ArrayList<>();
				if (node.getFinally() != null) {
					node.getFinally().accept(new ASTVisitor() {
						@Override
						public boolean visit(MethodInvocation node) {
							boolean isJunitAssertMethod = false;
							for (String junit : ValidatorUtils.JUNIT_ASSERT_METHODS) {
								if (junit.equals(node.getName().toString())) {
									isJunitAssertMethod = true;
									break;
								}
							}
							if (isJunitAssertMethod) {
								assertsAtFinally.add(node);
							}
							return super.visit(node);
						}
					});
				}

				// detect
				if (assertsAtBody.isEmpty() && node.catchClauses().size() != 0 && assertsAtCatches.isEmpty() && throwsAtCatches.isEmpty()
						&& assertsAtFinally.isEmpty()) {
					ret.add(node);
				}
				return super.visit(node);
			}
		});
		return ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		boolean staticImportAssert = false;
		for (ImportDeclaration id : (List<ImportDeclaration>) tc.getCompilationUnit().imports()) {
			if (id.getName().toString().contains("Assert") && id.isStatic()) {
				staticImportAssert = true;
			}
		}
		// detect
		for (ASTNode node : detect(commit, tc, results)) {
			TryStatement target = (TryStatement) node;
			if (!target(target, tc.getComments(), origin)) {
				return origin;
			}
			// create
			MethodInvocation method = ast.newMethodInvocation();
			method.setName(ast.newSimpleName("fail"));
			if (!staticImportAssert) {
				method.setExpression(ast.newSimpleName("Assert"));
			}
			StringLiteral arg = ast.newStringLiteral();
			arg.setLiteralValue("Expected exception");
			method.arguments().add(arg);
			ExpressionStatement statement = ast.newExpressionStatement(method);
			// insert
			ListRewrite listRewrite = rewrite.getListRewrite(target.getBody(), Block.STATEMENTS_PROPERTY);
			listRewrite.insertLast(statement, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

	private boolean onlyReturnCatchClause(TryStatement node) {
		if (node.catchClauses() == null) {
			return false;
		}
		for (Object object : node.catchClauses()) {
			CatchClause cc = (CatchClause) object;
			// catch clauses have only "return;"
			if (cc.getBody().statements().size() == 1 && cc.getBody().statements().get(0) instanceof ReturnStatement
					&& ((ReturnStatement) cc.getBody().statements().get(0)).getExpression() == null) {
				return true;
			}
		}
		return false;
	}

	private boolean expectExceptionComment(TryStatement target, List<Comment> comments, String source) {
		if (target.catchClauses() == null) {
			return false;
		}
		for (Object object : target.catchClauses()) {
			CatchClause cc = (CatchClause) object;
			for (Comment comment : comments) {
				// catch clauses have exception expecting comment.
				if (ValidatorUtils.thisNodeHasThisComments(cc, comment) && expectExceptionComment(ValidatorUtils.comment(comment, source))) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean target(TryStatement target, List<Comment> comments, String source) {
		return onlyReturnCatchClause(target) || expectExceptionComment(target, comments, source);
	}

	public static boolean expectExceptionComment(String comment) {
		String content = comment.toLowerCase();
		return content.contains("expect") || content.contains("ignore") || content.contains("ok") || content.contains("should happen")
				|| content.contains("do nothing") || content.contains("want") || content.contains("skip") || content.contains("should fail")
				|| content.contains("good") || content.contains("success") || content.contains("should be") || content.contains("supported");
	}
}

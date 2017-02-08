package jp.mzw.vtr.validate.exception_handling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;
import jp.mzw.vtr.validate.ValidatorUtils;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
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
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
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
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode node : detect(commit, tc, results)) {
			TryStatement target = (TryStatement) node;
			// create
			MethodInvocation method = ast.newMethodInvocation();
			method.setName(ast.newSimpleName("fail"));
			StringLiteral arg = ast.newStringLiteral();
			arg.setLiteralValue("Expected exception");
			method.arguments().add(arg);
			// insert
			ListRewrite listRewrite = rewrite.getListRewrite(target.getBody(), Block.STATEMENTS_PROPERTY);
			listRewrite.insertLast(method, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}

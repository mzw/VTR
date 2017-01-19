package jp.mzw.vtr.validate.template;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;
import jp.mzw.vtr.validate.ValidationResult;
import jp.mzw.vtr.validate.ValidatorBase;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseArithmeticAssignmentOperators extends ValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(UseArithmeticAssignmentOperators.class);

	public UseArithmeticAssignmentOperators(Project project) {
		super(project);
	}

	@Override
	public void onCheckout(Commit commit) {
		try {
			for (TestSuite ts : MavenUtils.getTestSuites(this.projectDir)) {
				for (TestCase tc : ts.getTestCases()) {
					if (this.dupulicates.contains(tc.getFullName())) {
						continue;
					}
					try {
						if (detect(tc)) {
							this.dupulicates.add(tc.getFullName());
							ValidationResult vr = new ValidationResult(this.projectId, commit, tc, tc.getStartLineNumber(), tc.getEndLineNumber(), this);
							this.validationResultList.add(vr);
						}
					} catch (IOException | MalformedTreeException | BadLocationException e) {
						LOGGER.warn("Failed to invoke Checkstyle: {}", e.getMessage());
					}

				}
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to checkout: {}", commit.getId());
		}
	}

	private boolean detect(TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		CompilationUnit cu = getCompilationUnit(tc.getTestFile());
		final List<MethodDeclaration> methods = new ArrayList<>();
		cu.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				methods.add(node);
				return super.visit(node);
			}
		});
		MethodDeclaration method = null;
		for (MethodDeclaration node : methods) {
			if (node.getStartPosition() == tc.getMethodDeclaration().getStartPosition()) {
				method = node;
				break;
			}
		}
		final List<Assignment> targets = new ArrayList<>();
		method.accept(new ASTVisitor() {
			@Override
			public boolean visit(Assignment node) {
				targets.add(node);
				return super.visit(node);
			}
		});
		for (Assignment target : targets) {
			Expression left = target.getLeftHandSide();
			Expression right = target.getRightHandSide();
			Assignment.Operator operator = target.getOperator();
			// operator
			if (!"=".equals(operator.toString())) {
				continue;
			}
			// detect
			final String string = left.toString();
			final Change change = new Change();
			if (right instanceof InfixExpression) {
				InfixExpression node = (InfixExpression) right;
				InfixExpression.Operator op = node.getOperator();
				Expression l = node.getLeftOperand();
				Expression r = node.getRightOperand();
				if (string.equals(l.toString())) {
					change.set(l, op, r);
				} else if (string.equals(r.toString())) {
					change.set(r, op, l);
				}
			}
			if (change.toChange()) {
				return true;
			}
		}
		return false;
	}

	public static class Change {
		private boolean toChange;
		private Expression target;
		private Expression rest;
		private InfixExpression.Operator operator;

		public Change() {
			toChange = false;
		}

		public void set(Expression target, InfixExpression.Operator operator, Expression rest) {
			toChange = true;
			this.target = target;
			this.operator = operator;
			this.rest = rest;
		}

		public boolean toChange() {
			return toChange;
		}

		public String getOprator() {
			return operator.toString();
		}
		
		public Expression getTarget() {
			return target;
		}

		public Expression getRest() {
			return rest;
		}
	}

	@Override
	public void generate(ValidationResult result) {
		try {
			TestCase tc = getTestCase(result);
			String origin = FileUtils.readFileToString(tc.getTestFile());
			String modified = getModified(origin.toString(), tc);
			List<String> patch = genPatch(origin, modified, tc.getTestFile(), tc.getTestFile());
			output(result, tc, patch);
		} catch (IOException | ParseException | GitAPIException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}

	private String getModified(String origin, TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = getCompilationUnit(tc.getTestFile());
		final List<MethodDeclaration> methods = new ArrayList<>();
		cu.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				methods.add(node);
				return super.visit(node);
			}
		});
		MethodDeclaration method = null;
		for (MethodDeclaration node : methods) {
			if (node.getStartPosition() == tc.getMethodDeclaration().getStartPosition()) {
				method = node;
				break;
			}
		}
		final List<Assignment> targets = new ArrayList<>();
		method.accept(new ASTVisitor() {
			@Override
			public boolean visit(Assignment node) {
				targets.add(node);
				return super.visit(node);
			}
		});
		// detect changes
		Map<Assignment, Change> changes = new HashMap<>();
		for (Assignment target : targets) {
			Assignment.Operator operator = target.getOperator();
			if (!"=".equals(operator.toString())) {
				continue;
			}
			Expression left = target.getLeftHandSide();
			Expression right = target.getRightHandSide();
			final String string = left.toString();
			final Change change = new Change();
			if (right instanceof InfixExpression) {
				InfixExpression node = (InfixExpression) right;
				InfixExpression.Operator op = node.getOperator();
				Expression l = node.getLeftOperand();
				Expression r = node.getRightOperand();
				if (string.equals(l.toString())) {
					change.set(l, op, r);
				} else if (string.equals(r.toString())) {
					change.set(r, op, l);
				}
			}
			if (change.toChange()) {
				changes.put(target, change);
			}
		}
		// rewrite
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		for (Assignment target : changes.keySet()) {
			Change change = changes.get(target);
			// node
			Assignment replace = ast.newAssignment();
			// left
			Expression left = (Expression) ASTNode.copySubtree(ast, target.getLeftHandSide());
			replace.setLeftHandSide(left);
			// operator
			Assignment.Operator operator = Assignment.Operator.toOperator(change.getOprator() + target.getOperator().toString());
			replace.setOperator(operator);
			// right
			Expression right = (Expression) ASTNode.copySubtree(ast, change.getRest());
			replace.setRightHandSide(right);
			// replace
			rewrite.replace(target, replace, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}

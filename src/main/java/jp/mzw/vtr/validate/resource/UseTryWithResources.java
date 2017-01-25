package jp.mzw.vtr.validate.resource;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;
import jp.mzw.vtr.validate.ValidationResult;
import jp.mzw.vtr.validate.ValidatorUtils;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseTryWithResources extends SimpleValidatorBase {
	protected Logger LOGGER = LoggerFactory.getLogger(UseTryWithResources.class);

	public UseTryWithResources(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		final CompilationUnit cu = tc.getCompilationUnit();
		cu.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodDeclaration node) {
				if (node.getStartPosition() == tc.getMethodDeclaration().getStartPosition() && node.getLength() == tc.getMethodDeclaration().getLength()) {
					node.accept(new ASTVisitor() {
						@Override
						public boolean visit(MethodInvocation node) {
							if (!"close".equals(node.getName().toString())) {
								return super.visit(node);
							}
							if (ValidatorUtils.isClosable(node.getExpression())) {
								ret.add(node);
							}
							return super.visit(node);
						}
					});
				}
				return super.visit(node);
			}
		});
		return ret;
	}

	public static VariableDeclarationStatement getVariableDeclarationStatement(final Expression expression) {
		final List<VariableDeclarationStatement> ret = new ArrayList<>();
		ASTNode parent = expression.getParent();
		while (parent != null) {
			parent.accept(new ASTVisitor() {
				@Override
				public boolean visit(VariableDeclarationStatement node) {
					for (Object object : node.fragments()) {
						VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
						if (fragment.getName().toString().equals(expression.toString())) {
							ret.add(node);
						}
					}
					return super.visit(node);
				}
			});
			if (!ret.isEmpty()) {
				break;
			}
			parent = parent.getParent();
		}
		if (!ret.isEmpty()) {
			return ret.get(0);
		}
		return null;
	}

	public static ClassInstanceCreation detectInitializer(final VariableDeclarationStatement var, final Expression expression) {
		for (Object object : var.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
			if (fragment.getInitializer() instanceof ClassInstanceCreation) {
				return (ClassInstanceCreation) fragment.getInitializer();
			}
		}
		// Need to parse data-flow
		Block scope = getScopalBlock(var);
		final List<ClassInstanceCreation> initializers = new ArrayList<>();
		scope.accept(new ASTVisitor() {
			@Override
			public boolean visit(Assignment node) {
				Expression left = node.getLeftHandSide();
				if (!left.toString().equals(expression.toString())) {
					return super.visit(node);
				}
				Expression right = node.getRightHandSide();
				if (right instanceof ClassInstanceCreation) {
					initializers.add((ClassInstanceCreation) right);
				}
				return super.visit(node);
			}
		});
		if (!initializers.isEmpty()) {
			return initializers.get(0);
		}
		return null;
	}

	public static Block getScopalBlock(ASTNode var) {
		ASTNode target = var;
		while (target != null) {
			if (target instanceof Block) {
				return (Block) target;
			}
			target = target.getParent();
		}
		return null;
	}

	public static VariableDeclarationExpression createVariableDeclarationExpression(ASTRewrite rewrite, Expression expression,
			VariableDeclarationStatement var, ClassInstanceCreation initializer) {
		StringBuilder string = new StringBuilder();
		string.append(Modifier.toString(var.getModifiers())).append(" ").append(var.getType()).append(" ").append(expression).append(" = ").append(initializer);
		return (VariableDeclarationExpression) rewrite.createStringPlaceholder(string.toString(), ASTNode.VARIABLE_DECLARATION_EXPRESSION);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getModified(String origin, TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> detects = detect(tc);
		if (detects.isEmpty()) {
			return origin;
		}
		AST ast = detects.get(0).getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		for (ASTNode detect : detects) {
			final MethodInvocation method = (MethodInvocation) detect; // object.close()
			final Expression expression = method.getExpression(); // object
			final TryStatement withResources = ast.newTryStatement();

			final VariableDeclarationStatement varDec = getVariableDeclarationStatement(expression);
			if (varDec == null) {
				LOGGER.warn("Failed to detect variable declaration: {}", expression);
				continue;
			}
			final ClassInstanceCreation initializer = detectInitializer(varDec, expression);
			if (initializer == null) {
				LOGGER.warn("Failed to detect variable initilizer: {}", varDec);
				continue;
			}
			final VariableDeclarationExpression resource = createVariableDeclarationExpression(rewrite, expression, varDec, initializer);
			withResources.resources().add(resource);

			Block scopeVarDec = getScopalBlock(varDec);
			Block scopeInit = getScopalBlock(initializer);
			Block scopeExpr = getScopalBlock(expression);

			if (scopeVarDec.equals(scopeInit) && scopeVarDec.equals(scopeExpr)) {
				final Detect start = new Detect();
				final Detect next = new Detect();
				final Detect end = new Detect();
				List<Statement> statements = new ArrayList<>();
				for (Object object : scopeVarDec.statements()) {
					Statement node = (Statement) object;
					node.accept(new ASTVisitor() {
						@Override
						public boolean visit(ClassInstanceCreation node) {
							if (node.equals(initializer)) {
								start.setDetect(true);
							}
							return super.visit(node);
						}
						@Override
						public boolean visit(MethodInvocation node) {
							if (node.equals(method)) {
								end.setDetect(true);
							}
							return super.visit(node);
						}
					});
					if (end.detect()) {
						rewrite.remove(node, null);
						break;
					}
					if (next.detect() && !end.detect()) {
						statements.add(node);
					}
					if (start.detect()) {
						rewrite.remove(node, null);
						next.setDetect(true);
					}
				}
				for (Statement statement : statements) {
					withResources.getBody().statements().add((Statement) ASTNode.copySubtree(ast, statement));
					rewrite.remove(statement, null);
				}
				rewrite.replace(varDec, withResources, null);
			} else {
				System.out.println("TODO: extentionally implement " + this.getClass());
			}
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return format(document.get());
	}

	@Override
	public void generate(ValidationResult result) {
		try {
			TestCase tc = getTestCase(result);
			String origin = FileUtils.readFileToString(tc.getTestFile());
			String modified = getModified(origin.toString(), tc);
			List<String> patch = genPatch(origin, modified, tc);
			output(result, tc, patch);
		} catch (IOException | ParseException | GitAPIException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}

	public static class Detect {
		private boolean detect;

		public Detect() {
			detect = false;
		}

		public void setDetect(boolean detect) {
			this.detect = detect;
		}

		public boolean detect() {
			return detect;
		}
	}

}

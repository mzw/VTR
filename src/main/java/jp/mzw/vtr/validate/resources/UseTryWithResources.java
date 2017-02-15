package jp.mzw.vtr.validate.resources;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;
import jp.mzw.vtr.validate.ValidationResult;
import jp.mzw.vtr.validate.ValidatorBase;
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
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseTryWithResources extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(UseTryWithResources.class);

	public UseTryWithResources(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		if (ValidatorBase.getJavaVersion(projectDir) < 1.7) {
			return ret;
		}
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				if (!"close".equals(node.getName().toString())) {
					return super.visit(node);
				}
				if (ValidatorUtils.isClosable(node.getExpression().resolveTypeBinding())) {
					ret.add(node);
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

	public static VariableDeclarationExpression createVariableDeclarationExpression(ASTRewrite rewrite, Expression expression, VariableDeclarationStatement var,
			ClassInstanceCreation initializer) {
		StringBuilder string = new StringBuilder();
		string.append(Modifier.toString(var.getModifiers())).append(" ").append(var.getType()).append(" ").append(expression).append(" = ").append(initializer);
		return (VariableDeclarationExpression) rewrite.createStringPlaceholder(string.toString(), ASTNode.VARIABLE_DECLARATION_EXPRESSION);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getModified(final String origin, final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		final CompilationUnit cu = tc.getCompilationUnit();
		final AST ast = cu.getAST();
		final ASTRewrite rewrite = ASTRewrite.create(ast);
		final List<ASTNode> removes = new ArrayList<>();
		for (ASTNode detect : detect(commit, tc, results)) {
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

				// Get target try-finally
				TryStatement tryStatement = getParentTryStatement(method);
				if (tryStatement == null) {
					LOGGER.info("Closable.close not in Try is limited: {} at {}", tc.getFullName(), commit.getId());
					continue;
				}
				if (tryStatement.getFinally() == null) {
					LOGGER.info("Try without finally containing Closable.close is limited: {} at {}", tc.getFullName(), commit.getId());
					continue;
				}
				// Get target variable
				if (!(method.getParent() instanceof ExpressionStatement)) {
					LOGGER.info("close() should be Closable.close(): {} at {}", tc.getFullName(), commit.getId());
					continue;
				}
				ExpressionStatement parent = (ExpressionStatement) method.getParent();
				final String varName = parent.getExpression().toString().split("\\.")[0];
				final ITypeBinding varType = method.resolveMethodBinding().getDeclaringClass();

				List<VariableDeclarationStatement> declarations = getDeclarations(tryStatement.getBody(), varName, varType);
				if (declarations.isEmpty()) {
					declarations = getDeclarations(tc.getMethodDeclaration(), varName, varType);
					if (declarations.isEmpty()) {
						declarations = getDeclarations(tc.getCompilationUnit(), varName, varType);
					}
				}
				if (declarations.isEmpty()) {
					LOGGER.info("TODO: Not found variable declaration: {} at {}", tc.getFullName(), commit.getId());
					System.out.println("Not found variable declaration: " + tc.getFullName() + " at " + commit.getId());
					System.out.println("method: " + method);
					System.out.println("-----");
					System.out.println(tc.getMethodDeclaration());
					System.out.println("=====");
					continue;
				}
				if (1 < declarations.size()) {
					LOGGER.info("TODO: Multiple variable declarations found: {} at {}", tc.getFullName(), commit.getId());
					System.out.println("Multiple variable declarations found: " + tc.getFullName() + " at " + commit.getId());
					System.out.println("method: " + method);
					System.out.println("-----");
					System.out.println(tc.getMethodDeclaration());
					System.out.println("=====");
					continue;
				}
				VariableDeclarationStatement declaration = declarations.get(0);
				ClassInstanceCreation instance = null;
				for (Object object : declaration.fragments()) {
					VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
					if (fragment.getInitializer() instanceof ClassInstanceCreation) {
						instance = (ClassInstanceCreation) fragment.getInitializer();
					}
				}

				if (instance == null) {
					List<ClassInstanceCreation> instances = getInstances(tryStatement.getBody(), varName, varType);
					if (instances.isEmpty()) {
						instances = getInstances(tc.getMethodDeclaration(), varName, varType);
						if (instances.isEmpty()) {
							instances = getInstances(tc.getCompilationUnit(), varName, varType);
						}
					}
					if (instances.isEmpty()) {
						System.out.println("TODO: Not found instantance: " + tc.getFullName() + " at " + commit.getId());
						System.out.println("method: " + method);
						System.out.println("-----");
						System.out.println(tc.getMethodDeclaration());
						System.out.println("=====");
						continue;
					}
					if (1 < instances.size()) {
						LOGGER.info("Found multiple instantiations (limitation b/c need to split this try statement): {} at {}", tc.getFullName(),
								commit.getId());
						continue;
					}
					instance = instances.get(0);
				}

				// Add resource
				boolean isFinal = Modifier.isFinal(declarations.get(0).getType().resolveBinding().getModifiers());
				StringBuilder builder = new StringBuilder();
				String string = builder.append(isFinal ? "final " + varType.getName() + " " : "").append(varName).append(" = ").append(instance).toString();
				VariableDeclarationExpression res = (VariableDeclarationExpression) rewrite.createStringPlaceholder(string,
						ASTNode.VARIABLE_DECLARATION_EXPRESSION);
				ListRewrite listRewrite = rewrite.getListRewrite(tryStatement, TryStatement.RESOURCES_PROPERTY);
				listRewrite.insertFirst(res, null);
				for (Object object : tryStatement.getBody().statements()) {
					ASTNode statement = (ASTNode) object;
					if (statement.toString().trim().startsWith(varName + "=")) {
						rewrite.remove(statement, null);
					} else if (statement.toString().trim().startsWith(varName + ".close")) { // TODO
																								// closable.closeFoo();
						rewrite.remove(statement, null);
					}
				}

				// Removes
				if (isFinal) {
					removes.add(declarations.get(0));
				}
				List<IfStatement> finallyIfCloses = getFinallyIfClose(tryStatement.getFinally(), method);
				if (finallyIfCloses.size() == 1) {
					removes.add(finallyIfCloses.get(0));
				} else {
					removes.add(method.getParent());
				}
				boolean unnecessary = true;
				for (Object object : tryStatement.getFinally().statements()) {
					if (!removes.contains(object)) {
						unnecessary = false;
					}
				}
				if (unnecessary) {
					removes.add(tryStatement.getFinally());
				}
			}
		}
		if (removes.isEmpty()) {
			return origin;
		}
		for (ASTNode remove : removes) {
			rewrite.remove(remove, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return format(document.get());
	}

	private static List<VariableDeclarationStatement> getDeclarations(final ASTNode node, final String varName, final ITypeBinding varType) {
		final List<VariableDeclarationStatement> ret = new ArrayList<>();
		if (node == null || varName == null || varType == null) {
			return ret;
		}
		node.accept(new ASTVisitor() {
			@Override
			public boolean visit(VariableDeclarationStatement node) {
				if (compareTo(node.getType().resolveBinding(), varType)) {
					for (Object object : node.fragments()) {
						VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
						if (fragment.getName().toString().equals(varName)) {
							ret.add(node);
							break;
						}
					}
				}
				return super.visit(node);
			}
		});
		return ret;
	}

	private static List<ClassInstanceCreation> getInstances(final ASTNode node, final String varName, final ITypeBinding varType) {
		final List<ClassInstanceCreation> ret = new ArrayList<>();
		if (node == null || varName == null || varType == null) {
			return ret;
		}
		node.accept(new ASTVisitor() {
			@Override
			public boolean visit(Assignment node) {
				if (node.getLeftHandSide().toString().equals(varName) && compareTo(node.getLeftHandSide().resolveTypeBinding(), varType)) {
					if (node.getRightHandSide() instanceof ClassInstanceCreation) {
						ret.add((ClassInstanceCreation) node.getRightHandSide());
					}
				}
				return super.visit(node);
			}
		});
		return ret;
	}

	private static boolean compareTo(ITypeBinding same, ITypeBinding base) {
		if (same == null || base == null) {
			return false;
		}
		if (same.equals(base)) {
			return true;
		}
		ITypeBinding parent = same.getSuperclass();
		while (parent != null) {
			if (parent.equals(base)) {
				return true;
			}
			parent = parent.getSuperclass();
		}
		return false;
	}

	private static TryStatement getParentTryStatement(ASTNode node) {
		if (node == null) {
			return null;
		}
		if (node instanceof TryStatement) {
			return (TryStatement) node;
		}
		ASTNode parent = node.getParent();
		while (parent != null) {
			if (parent instanceof TryStatement) {
				return (TryStatement) parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	private static List<IfStatement> getFinallyIfClose(final Block finallyClause, final MethodInvocation method) {
		final List<IfStatement> ret = new ArrayList<>();
		if (finallyClause == null || method == null) {
			return ret;
		}
		finallyClause.accept(new ASTVisitor() {
			@Override
			public boolean visit(final IfStatement node) {
				final List<MethodInvocation> isThisMethod = new ArrayList<>();
				node.accept(new ASTVisitor() {
					@Override
					public boolean visit(final MethodInvocation node) {
						if (node.equals(method)) {
							isThisMethod.add(node);
						}
						return super.visit(node);
					}
				});
				if (!isThisMethod.isEmpty()) {
					ret.add(node);
				}
				return super.visit(node);
			}
		});
		return ret;
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

	@Override
	public void generate(ValidationResult result) {
		try {
			// Read
			Commit commit = new Commit(result.getCommitId(), null);
			TestCase testcase = getTestCase(result, projectDir);
			Results results = Results.parse(outputDir, projectId, commit);
			// Generate
			String origin = FileUtils.readFileToString(testcase.getTestFile());
			String modified = getModified(origin.toString(), commit, testcase, results);
			List<String> _patch = genPatch(getTestCaseSource(origin, testcase.getName()), getTestCaseSource(modified, testcase.getName()),
					testcase.getTestFile(), testcase.getTestFile(), (testcase.getStartLineNumber() - 1) * -1 - 1);
			List<String> patch = new ArrayList<>();
			for (String line : _patch) {
				if (line.contains("try (") && line.contains(");")) {
					patch.add(line.replaceAll("\\);", ";"));
				} else {
					patch.add(line);
				}
			}
			// No modification
			if (patch.isEmpty()) {
				return;
			} else if (patch.size() == 1) {
				if ("".equals(patch.get(0))) {
					return;
				}
			}
			output(result, testcase, patch);
		} catch (IOException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}
}

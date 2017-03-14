package jp.mzw.vtr.validate.junit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;
import jp.mzw.vtr.validate.ValidatorBase;

public class AssertNotNullToInstances extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(AssertNotNullToInstances.class);

	public AssertNotNullToInstances(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase testcase, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		if (Version.parse("4").compareTo(ValidatorBase.getJunitVersion(projectDir)) < 0) {
			return ret;
		}
		testcase.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(SingleMemberAnnotation node) {
				String type = node.getTypeName().toString();
				String member = node.getValue().resolveConstantExpressionValue().toString();
				// Check whether unused
				// TODO from compile results
				if ("SuppressWarnings".equals(type) && "unused".equals(member)) {
					if ((node.getParent() instanceof VariableDeclarationStatement)) {
						VariableDeclarationStatement statement = (VariableDeclarationStatement) node.getParent();
						for (Object object : statement.fragments()) {
							VariableDeclarationFragment fragment = (VariableDeclarationFragment) object;
							ret.add(fragment);
						}
					} else if ((node.getParent()) instanceof MethodDeclaration) {
						MethodDeclaration method = (MethodDeclaration) node.getParent();
						for (Object object : method.getBody().statements()) {
							Statement statement = (Statement) object;
							if (statement instanceof ExpressionStatement) {
								Expression expression = ((ExpressionStatement) statement).getExpression();
								if (expression instanceof ClassInstanceCreation) {
									ClassInstanceCreation creation = (ClassInstanceCreation) expression;
									if (creation.getExpression() == null) {
										// add ClassInstanceCreation without
										// assignment, e.g. 'new PoolUtils();'
										ret.add(creation);
									}
								}

							}
						}
					}
				}
				return super.visit(node);
			}
		});
		return ret;
	}

	@Override
	protected String getModified(String origin, Commit commit, TestCase testcase, Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		final CompilationUnit cu = testcase.getCompilationUnit();
		final AST ast = cu.getAST();
		final ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode detect : detect(commit, testcase, results)) {
			if (detect instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment node = (VariableDeclarationFragment) detect;
				MethodInvocation method = (MethodInvocation) rewrite.createStringPlaceholder("Assert.assertNotNull(" + node.getInitializer().toString() + ");",
						ASTNode.METHOD_INVOCATION);
				rewrite.replace(node.getParent(), method, null);
			} else if (detect instanceof ClassInstanceCreation) {
				ClassInstanceCreation node = (ClassInstanceCreation) detect;
				MethodInvocation method = (MethodInvocation) rewrite.createStringPlaceholder("Assert.assertNotNull(" + node.toString() + ");",
						ASTNode.METHOD_INVOCATION);
				rewrite.replace(node.getParent(), method, null);
			}
		}
		// import
		final List<ImportDeclaration> imports = new ArrayList<>();
		cu.accept(new ASTVisitor() {
			@Override
			public boolean visit(ImportDeclaration node) {
				imports.add(node);
				return super.visit(node);
			}
		});
		boolean imported = false;
		Version version = ValidatorBase.getJunitVersion(projectDir);
		ImportDeclaration old = null;
		for (ImportDeclaration implemented : imports) {
			String name = implemented.getName().toString();
			if (Version.parse("4").compareTo(version) < 0 && "junit.framework.Assert".equals(name)) {
				imported = true;
			} else if (0 < Version.parse("4").compareTo(version) && "org.junit.Assert".equals(name)) {
				imported = true;
			} else if (0 < Version.parse("4").compareTo(version) && "junit.framework.Assert".equals(name)) {
				old = implemented;
			}
		}
		if (!imported) {
			ImportDeclaration add = ast.newImportDeclaration();
			if (0 < Version.parse("4").compareTo(version)) {
				add.setName(ast.newName("org.junit.Assert"));
			} else {
				add.setName(ast.newName("junit.framework.Assert"));
			}
			ListRewrite listRewrite = rewrite.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY);
			listRewrite.insertLast(add, null);
		}
		if (old != null) {
			rewrite.remove(old, null);
		}
		// suppress warning
		// TODO need to check whether remove or remain
		testcase.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(SingleMemberAnnotation node) {
				String type = node.getTypeName().toString();
				String member = node.getValue().resolveConstantExpressionValue().toString();
				if ("SuppressWarnings".equals(type) && "unused".equals(member)) {
					rewrite.remove(node, null);
				}
				return super.visit(node);
			}
		});
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}

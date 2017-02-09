package jp.mzw.vtr.validate.suppress_warnings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteUnnecessaryAssignmenedVariables extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(DeleteUnnecessaryAssignmenedVariables.class);
	
	public DeleteUnnecessaryAssignmenedVariables(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(Commit commit, TestCase tc, Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		final List<VariableDeclarationStatement> statements = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(VariableDeclarationStatement node) {
				statements.add(node);
				return super.visit(node);
			}
		});
		for (VariableDeclarationStatement statement: statements) {
			if (unnecessaryAssignment(statement)) {
				ret.add(statement);
			}
		}
		return ret;
	}

	@Override
	protected String getModified(String origin, Commit commit, TestCase tc, Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode node: detect(commit, tc, results)) {
			VariableDeclarationStatement target = (VariableDeclarationStatement) node;
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) target.fragments().get(0);
			MethodInvocation method = (MethodInvocation) ASTNode.copySubtree(ast, fragment.getInitializer());
			ExpressionStatement replace = ast.newExpressionStatement(method);
			rewrite.replace(target, replace, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
	
	private boolean unnecessaryAssignment(VariableDeclarationStatement statement) {
		// 多重代入は対象外
		if (statement.fragments().size() > 1) {
			return false;
		}
		for (Object obj: statement.fragments()) {
			VariableDeclarationFragment fragment = (VariableDeclarationFragment) obj;
			if ((fragment.getInitializer() == null) || !(fragment.getInitializer() instanceof MethodInvocation)) {
				continue;
			}
			Set<String> usedVariables = usedVariables(fragment.getName());
			if (!usedVariables.contains(fragment.getName().toString())) {
				return true;
			}
		}
		return false;
	}
	
	private Set<String> usedVariables(ASTNode node) {
		final List<SimpleName> nodes = new ArrayList<>();
		final Set<String> usedVariables = new HashSet<>();
		Block block = jp.mzw.vtr.validate.resources.UseTryWithResources.getScopalBlock(node);
		block.accept(new ASTVisitor() {
			@Override
			public boolean visit(SimpleName node) {
				nodes.add(node);
				return super.visit(node);
			}
		});
		for (SimpleName name: nodes) {
			if (node.getStartPosition() == name.getStartPosition()) {
				continue;
			}
			usedVariables.add(name.toString());
		}
		return usedVariables;
	}
}
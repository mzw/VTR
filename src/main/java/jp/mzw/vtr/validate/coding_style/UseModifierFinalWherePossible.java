package jp.mzw.vtr.validate.coding_style;

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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.VariableDeclarationFix;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseModifierFinalWherePossible extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(UseModifierFinalWherePossible.class);

	public UseModifierFinalWherePossible(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> ret = new ArrayList<>();
		VariableDeclarationFix fix = (VariableDeclarationFix) VariableDeclarationFix.createCleanUp(tc.getCompilationUnit(), true, true, true);
		if (fix == null) {
			return ret;
		}
		for (CompilationUnitRewriteOperation operation : fix.getOperations()) {
			VariableDeclarationFix.ModifierChangeOperation change = (VariableDeclarationFix.ModifierChangeOperation) operation;
			if (!change.getToChange().isEmpty()) {
				ASTNode declaration = change.getDeclaration();
				int start = tc.getCompilationUnit().getLineNumber(declaration.getStartPosition());
				int end = tc.getCompilationUnit().getLineNumber(declaration.getStartPosition() + declaration.getLength());
				if (tc.getStartLineNumber() <= start && end <= tc.getEndLineNumber()) {
					ret.add(declaration);
				}
			}
		}
		return ret;
	}

	@Override
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// Rewrite
		List<ASTNode> declarations = detect(commit, tc, results);
		for (ASTNode declaration : declarations) {
			if (declaration instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement target = (VariableDeclarationStatement) declaration;
				ListRewrite lr = rewrite.getListRewrite(target, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
				lr.insertLast(rewrite.createStringPlaceholder("final", VariableDeclarationStatement.MODIFIER), null);
			} else if (declaration instanceof FieldDeclaration) {
				FieldDeclaration target = (FieldDeclaration) declaration;
				ListRewrite lr = rewrite.getListRewrite(target, FieldDeclaration.MODIFIERS2_PROPERTY);
				lr.insertLast(rewrite.createStringPlaceholder("final", FieldDeclaration.MODIFIER), null);
			} else if (declaration instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration target = (SingleVariableDeclaration) declaration;
				ListRewrite lr = rewrite.getListRewrite(target, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
				lr.insertLast(rewrite.createStringPlaceholder("final", SingleVariableDeclaration.MODIFIER), null);
			} else if (declaration instanceof VariableDeclarationExpression) {
				VariableDeclarationExpression target = (VariableDeclarationExpression) declaration;
				ListRewrite lr = rewrite.getListRewrite(target, VariableDeclarationExpression.MODIFIERS2_PROPERTY);
				lr.insertLast(rewrite.createStringPlaceholder("final", VariableDeclarationExpression.MODIFIER), null);
			}
		}
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

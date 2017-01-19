package jp.mzw.vtr.validate.cleanup;

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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.internal.corext.fix.VariableDeclarationFix;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseModifierFinalWherePossible extends ValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(UseModifierFinalWherePossible.class);

	public UseModifierFinalWherePossible(Project project) {
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
					// ASTParser
					ASTParser parser = ASTParser.newParser(AST.JLS8);
					parser.setResolveBindings(true);
					parser.setBindingsRecovery(true);
					parser.setEnvironment(null, null, null, true);
					// CompilationUnits
					final Map<String, CompilationUnit> units = new HashMap<>();
					FileASTRequestor requestor = new FileASTRequestor() {
						@Override
						public void acceptAST(String sourceFilePath, CompilationUnit ast) {
							units.put(sourceFilePath, ast);
						}
					};
					// Parse
					parser.createASTs(getSources(), null, new String[] {}, requestor, new NullProgressMonitor());
					// Results
					CompilationUnit cu = units.get(tc.getTestFile().getCanonicalPath());
					VariableDeclarationFix fix = (VariableDeclarationFix) VariableDeclarationFix.createCleanUp(cu, true, true, true);
					List<ASTNode> declarations = detect(tc, fix, cu);
					if (!declarations.isEmpty()) {
						this.dupulicates.add(tc.getFullName());
						for (ASTNode declaration : declarations) {
							int start = cu.getLineNumber(declaration.getStartPosition());
							int end = cu.getLineNumber(declaration.getStartPosition() + declaration.getLength());
							ValidationResult vr = new ValidationResult(this.projectId, commit, tc, start, end, this);
							this.validationResultList.add(vr);
						}
					}
				}
			}
		} catch (IOException | CoreException e) {
			LOGGER.warn("Failed to checkout: {}", commit.getId());
		}
	}

	private List<ASTNode> detect(TestCase tc, VariableDeclarationFix fix, CompilationUnit cu) throws CoreException {
		List<ASTNode> ret = new ArrayList<>();
		if (fix == null) {
			return ret;
		}
		for (CompilationUnitRewriteOperation operation : fix.getOperations()) {
			VariableDeclarationFix.ModifierChangeOperation change = (VariableDeclarationFix.ModifierChangeOperation) operation;
			if (!change.getToChange().isEmpty()) {
				ASTNode declaration = change.getDeclaration();
				int start = cu.getLineNumber(declaration.getStartPosition());
				int end = cu.getLineNumber(declaration.getStartPosition() + declaration.getLength());
				if (tc.getStartLineNumber() <= start && end <= tc.getEndLineNumber()) {
					ret.add(declaration);
				}
			}
		}
		return ret;
	}

	@Override
	public void generate(ValidationResult result) {
		try {
			TestCase tc = getTestCase(result);
			String origin = FileUtils.readFileToString(tc.getTestFile());
			String modified = getModified(origin.toString(), tc);
			List<String> patch = genPatch(origin, modified, tc.getTestFile(), tc.getTestFile());
			output(result, tc, patch);
		} catch (IOException | ParseException | GitAPIException | CoreException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}

	private String getModified(String origin, TestCase tc) throws IOException, CoreException, MalformedTreeException, BadLocationException {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setEnvironment(null, null, null, true);
		// CompilationUnits
		final Map<String, CompilationUnit> units = new HashMap<>();
		FileASTRequestor requestor = new FileASTRequestor() {
			@Override
			public void acceptAST(String sourceFilePath, CompilationUnit ast) {
				units.put(sourceFilePath, ast);
			}
		};
		// Parse
		parser.createASTs(getSources(), null, new String[] {}, requestor, new NullProgressMonitor());
		// Results
		CompilationUnit cu = units.get(tc.getTestFile().getCanonicalPath());
		VariableDeclarationFix fix = (VariableDeclarationFix) VariableDeclarationFix.createCleanUp(cu, true, true, true);
		List<ASTNode> declarations = detect(tc, fix, cu);
		// Rewrite
		ASTRewrite rewriter = ASTRewrite.create(cu.getAST());
		for (ASTNode declaration : declarations) {
			if (declaration instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement target = (VariableDeclarationStatement) declaration;
				ListRewrite lr = rewriter.getListRewrite(target, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
				lr.insertLast(rewriter.createStringPlaceholder("final", VariableDeclarationStatement.MODIFIER), null);
			} else if (declaration instanceof FieldDeclaration) {
				FieldDeclaration target = (FieldDeclaration) declaration;
				ListRewrite lr = rewriter.getListRewrite(target, FieldDeclaration.MODIFIERS2_PROPERTY);
				lr.insertLast(rewriter.createStringPlaceholder("final", FieldDeclaration.MODIFIER), null);
			} else if (declaration instanceof SingleVariableDeclaration) {
				SingleVariableDeclaration target = (SingleVariableDeclaration) declaration;
				ListRewrite lr = rewriter.getListRewrite(target, SingleVariableDeclaration.MODIFIERS2_PROPERTY);
				lr.insertLast(rewriter.createStringPlaceholder("final", SingleVariableDeclaration.MODIFIER), null);
			} else if (declaration instanceof VariableDeclarationExpression) {
				VariableDeclarationExpression target = (VariableDeclarationExpression) declaration;
				ListRewrite lr = rewriter.getListRewrite(target, VariableDeclarationExpression.MODIFIERS2_PROPERTY);
				lr.insertLast(rewriter.createStringPlaceholder("final", VariableDeclarationExpression.MODIFIER), null);
			}
		}
		Document document = new Document(origin);
		TextEdit edit = rewriter.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}

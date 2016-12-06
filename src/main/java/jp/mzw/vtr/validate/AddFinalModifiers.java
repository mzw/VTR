package jp.mzw.vtr.validate;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddFinalModifiers extends ValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(AddFinalModifiers.class);

	public AddFinalModifiers(Project project) {
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
					if (!detect(commit, tc)) {
						this.dupulicates.add(tc.getFullName());
						MethodDeclaration method = tc.getMethodDeclaration();
						ValidationResult vr = new ValidationResult(this.projectId, commit, tc, tc.getStartLineNumber(method), tc.getEndLineNumber(method), this);
						this.validationResultList.add(vr);
					}
				}
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to checkout: {}", commit.getId());
		}
	}

	/**
	 * try {} catch (FooException e) {}
	 * 
	 * @param commit
	 * @param tc
	 * @return
	 */
	private boolean detect(Commit commit, TestCase tc) {
		for (ASTNode node : tc.getNodes()) {
			if (node instanceof TryStatement) {

			}
		}
		return false;
	}

	protected void validate(Commit commit, TestCase tc, TryStatement tryStatement) {
		for (Object catchClause : tryStatement.catchClauses()) {
			if (catchClause instanceof CatchClause) {
				validate(commit, tc, (CatchClause) catchClause);
			}
		}
	}

	protected void validate(Commit commit, TestCase tc, CatchClause catchClause) {
		if (ValidatorUtils.hasAssertMethodInvocation(catchClause.getBody())) {
			LOGGER.info("Find try-statement but its catch-block contains assertion(s): {} @ {}", tc.getFullName(), commit.getId());
		} else {
			// Register detection result
			ValidationResult vr = new ValidationResult(this.projectId, commit, tc, tc.getStartLineNumber(catchClause), tc.getEndLineNumber(catchClause), this);
			this.validationResultList.add(vr);
		}
	}

	@Override
	public void generate(ValidationResult result) {
		try {
			TestCase tc = getTestCase(result);
			String origin = FileUtils.readFileToString(tc.getTestFile());
			String modified = insertFinalModifier(origin, tc);
			List<String> patch = genPatch(origin, modified, tc.getTestFile(), tc.getTestFile());
			output(result, tc, patch);
		} catch (IOException | ParseException | GitAPIException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}

	/**
	 * try {} catch (final FooException e) {}
	 * 
	 * @return
	 */
	private String insertFinalModifier(String origin, TestCase tc) throws MalformedTreeException, BadLocationException {
		return "";
	}
}

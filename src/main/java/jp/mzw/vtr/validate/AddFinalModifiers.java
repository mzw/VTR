package jp.mzw.vtr.validate;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import jp.mzw.vtr.CLI;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
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

	private boolean detect(Commit commit, TestCase tc) {
		// try {} catch (FooException e) {}
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
		if (hasAssertMethodInvocation(catchClause.getBody())) {
			LOGGER.info("Find try-statement but its catch-block contains assertion(s): {} @ {}", tc.getFullName(), commit.getId());
		} else {
			// Register detection result
			ValidationResult vr = new ValidationResult(this.projectId, commit, tc, tc.getStartLineNumber(catchClause), tc.getEndLineNumber(catchClause), this);
			this.validationResultList.add(vr);
		}
	}

	@Override
	public void generate(ValidationResult result) {
		// try {} catch (final FooException e) {}
		try {
			// Pattern
			String pattern = result.getValidatorName();
			// Checkout
			String projectId = result.getProjectId();
			Project project = new Project(projectId).setConfig(CLI.CONFIG_FILENAME);
			CheckoutConductor cc = new CheckoutConductor(project);
			// Commit
			String commitId = result.getCommitId();
			Commit commit = new Commit(commitId, null);
			cc.checkout(commit);
			// Detect test case
			String clazz = result.getTestCaseClassName();
			String method = result.getTestCaseMathodName();
			List<TestSuite> testSuites = MavenUtils.getTestSuites(project.getProjectDir());
			for (TestSuite ts : testSuites) {
				TestCase tc = ts.getTestCaseBy(clazz, method);
				if (tc != null) {
					File file = ts.getTestFile();
					String origin = FileUtils.readFileToString(file);
					List<String> content = genPatch(origin, tc);
					if (content != null) {
						File projectDir = new File(project.getOutputDir(), projectId);
						File validateDir = new File(projectDir, ValidatorBase.VALIDATOR_DIRNAME);
						File commitDir = new File(validateDir, commitId);
						File patternDir = new File(commitDir, pattern);
						if (!patternDir.exists()) {
							patternDir.mkdirs();
						}
						File patchFile = new File(patternDir, tc.getFullName() + ".patch");
						FileUtils.writeLines(patchFile, content);
						LOGGER.warn("Succeeded to generate patch: {}", file.getPath());
					}
					break;
				}
			}
		} catch (IOException | ParseException | GitAPIException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}
	
	private List<String> genPatch(String origin, TestCase tc) throws MalformedTreeException, IllegalArgumentException, BadLocationException {
		return null;
	}
}

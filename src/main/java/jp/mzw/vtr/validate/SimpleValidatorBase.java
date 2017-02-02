package jp.mzw.vtr.validate;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

abstract public class SimpleValidatorBase extends ValidatorBase {
	protected Logger LOGGER = LoggerFactory.getLogger(SimpleValidatorBase.class);
	
	public SimpleValidatorBase(Project project) {
		super(project);
	}

	@Override
	public ValidationResult validate(Commit commit, TestCase testcase, Results results) {
		try {
			List<ASTNode> detects = detect(commit, testcase, results);
			if (detects == null) {
				return null;
			}
			if (!detects.isEmpty()) {
				return new ValidationResult(this.projectId, commit, testcase, testcase.getStartLineNumber(), testcase.getEndLineNumber(), this);
			}
		} catch (IOException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to invoke Checkstyle: {}", e.getMessage());
		}
		return null;
	}

	abstract protected List<ASTNode> detect(Commit commit, TestCase testcase, Results results) throws IOException, MalformedTreeException, BadLocationException;

	@Override
	public void generate(ValidationResult result) {
		try {
			// Read
			Commit commit = new Commit(result.getCommitId(), null);
			new CheckoutConductor(projectId, projectDir, outputDir).checkout(commit);
			TestCase testcase = getTestCase(result, projectDir);
			Results results = Results.parse(outputDir, projectId, commit);
			// Generate
			String origin = FileUtils.readFileToString(testcase.getTestFile());
			String modified = getModified(origin.toString(), commit, testcase, results);
			List<String> patch = genPatch(origin, modified, testcase.getTestFile(), testcase.getTestFile());
			output(result, testcase, patch);
		} catch (IOException | ParseException | GitAPIException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}
	
	protected TestCase getTestCase(ValidationResult result, File projectDir) throws IOException {
		String clazz = result.getTestCaseClassName();
		String method = result.getTestCaseMathodName();
		List<TestSuite> testSuites = MavenUtils.getTestSuitesAtLevel2(projectDir);
		for (TestSuite ts : testSuites) {
			TestCase tc = ts.getTestCaseBy(clazz, method);
			if (tc != null) {
				return tc;
			}
		}
		return null;
	}

	abstract protected String getModified(String origin, Commit commit, TestCase testcase, Results results) throws IOException, MalformedTreeException, BadLocationException;

}

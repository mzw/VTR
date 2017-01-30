package jp.mzw.vtr.validate;

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
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;

abstract public class SimpleValidatorBase extends ValidatorBase {
	protected Logger LOGGER = LoggerFactory.getLogger(SimpleValidatorBase.class);
	
	protected Commit commit;
	protected TestCase testcase;
	protected Results results;
	
	public SimpleValidatorBase(Project project) {
		super(project);
	}
	
	@Override
	public void onCheckout(Commit commit, TestCase testcase, Results results) {
		this.commit = commit;
		this.testcase = testcase;
		this.results = results;
		if (this.dupulicates.contains(testcase.getFullName())) {
			return;
		}
		try {
			List<ASTNode> detects = detect(testcase);
			if (detects == null) {
				return;
			}
			if (!detects.isEmpty()) {
				this.dupulicates.add(testcase.getFullName());
				ValidationResult vr = new ValidationResult(this.projectId, commit, testcase, testcase.getStartLineNumber(), testcase.getEndLineNumber(), this);
				this.validationResultList.add(vr);
			}
		} catch (IOException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to invoke Checkstyle: {}", e.getMessage());
		}
	}

	abstract protected List<ASTNode> detect(TestCase testcase) throws IOException, MalformedTreeException, BadLocationException;

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

	abstract protected String getModified(String origin, TestCase tc) throws IOException, MalformedTreeException, BadLocationException;

}

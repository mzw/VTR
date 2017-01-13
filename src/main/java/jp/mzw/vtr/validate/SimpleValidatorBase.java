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
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

abstract public class SimpleValidatorBase extends ValidatorBase {
	protected Logger LOGGER = LoggerFactory.getLogger(SimpleValidatorBase.class);

	public SimpleValidatorBase(Project project) {
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
					try {
						if (!detect(tc).isEmpty()) {
							this.dupulicates.add(tc.getFullName());
							ValidationResult vr = new ValidationResult(this.projectId, commit, tc, tc.getStartLineNumber(), tc.getEndLineNumber(), this);
							this.validationResultList.add(vr);
						}
					} catch (IOException | MalformedTreeException | BadLocationException e) {
						LOGGER.warn("Failed to invoke Checkstyle: {}", e.getMessage());
					}

				}
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to checkout: {}", commit.getId());
		}
	}

	abstract protected List<ASTNode> detect(TestCase tc) throws IOException, MalformedTreeException, BadLocationException;

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

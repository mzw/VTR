package jp.mzw.vtr.validate;

import java.io.IOException;

import org.eclipse.jdt.core.dom.MethodDeclaration;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

public class PreventTypeErrors extends ValidatorBase {

	public PreventTypeErrors(Project project) {
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
		// List list = new ArrayList();
		
		return false;
	}

	@Override
	public void generate(ValidationResult result) {
		// List<Object> list = new ArrayList<Object>();
	}

}

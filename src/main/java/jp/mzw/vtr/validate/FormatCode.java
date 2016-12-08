package jp.mzw.vtr.validate;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;

public class FormatCode extends ValidatorBase {

	public FormatCode(Project project) {
		super(project);
	}

	@Override
	public void onCheckout(Commit commit) {
		
	}

	@Override
	public void generate(ValidationResult result) {
		
	}

}

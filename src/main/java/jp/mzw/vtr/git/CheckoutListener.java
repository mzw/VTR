package jp.mzw.vtr.git;

import jp.mzw.vtr.core.Project;

abstract public class CheckoutListener implements CheckoutConductor.Listener {
	protected Project project;

	public CheckoutListener(Project project) {
		this.project = project;
	}
	
	public Project getProject() {
		return project;
	}
	
	@Override
	abstract public void onCheckout(Commit commit);
}

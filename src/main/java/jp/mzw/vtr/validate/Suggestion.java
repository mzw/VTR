package jp.mzw.vtr.validate;

import jp.mzw.vtr.Project;

public class Suggestion {

	protected Project project;
	protected String commit;
	
	public Suggestion(Project project, String commit) {
		this.project = project;
		this.commit = commit;
	}
	
	public Project getProject() {
		return this.project;
	}
	
	public String getCommit() {
		return this.commit;
	}
	
	public String toXml() {
		StringBuilder builder = new StringBuilder();
		builder.append("<Suggestion>");
		builder.append("<Project>").append(project.getProjectName()).append("</Project>");
		builder.append("<CommitId>").append(commit).append("</CommitId>");
		builder.append("</Suggestion>");
		return builder.toString();
	}
}

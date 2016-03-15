package jp.mzw.vtr.validate;

import java.io.File;

import jp.mzw.vtr.Project;

import com.github.javaparser.ast.body.MethodDeclaration;

public class TimeoutCandidate extends Timeout {

	public TimeoutCandidate(Project project, String commit,
			File file, MethodDeclaration method) {
		super(project, commit, file, method);
	}
	
	@Override
	public String toXml() {
		StringBuilder builder = new StringBuilder();
		builder.append("<Suggestion type=\"TimeoutCandidate\">");
		builder.append("<Project>").append(project.getProjectName()).append("</Project>");
		builder.append("<CommitId>").append(commit).append("</CommitId>");
		builder.append("<FilePath>").append(file.getPath()).append("</FilePath>");
		builder.append("<MethodName>").append(method.getName()).append("</MethodName>");
		builder.append("</Suggestion>");
		return builder.toString();
	}
}

package jp.mzw.vtr.validate;

import java.io.File;

import jp.mzw.vtr.core.Project;

import com.github.javaparser.ast.body.MethodDeclaration;

public class ExpectedException extends Suggestion {

	protected File file;
	protected MethodDeclaration method;
	protected String exception;
	public ExpectedException(Project project, String commit,
			File file, MethodDeclaration method, String exception) {
		super(project, commit);
		this.method = method;
		this.file = file;
		this.exception = exception;
	}
	
	public File getFile() {
		return this.file;
	}
	
	public MethodDeclaration getMethod() {
		return this.method;
	}
	
	@Override
	public String toXml() {
		StringBuilder builder = new StringBuilder();
		builder.append("<Suggestion type=\"ExpectedException\">");
		builder.append("<Project>").append(project.getProjectName()).append("</Project>");
		builder.append("<CommitId>").append(commit).append("</CommitId>");
		builder.append("<FilePath>").append(file.getPath()).append("</FilePath>");
		builder.append("<MethodName>").append(method.getName()).append("</MethodName>");
		builder.append("<Exception>").append(exception).append("</Exception>");
		builder.append("</Suggestion>");
		return builder.toString();
	}
	
}

package jp.mzw.vtr.validate;

import java.io.File;

import jp.mzw.vtr.core.Project;

import com.github.javaparser.ast.body.MethodDeclaration;

public class SuppressWarnings extends Suggestion {

	protected File file;
	protected MethodDeclaration method;
	protected String warning;
	protected int lineno;
	public SuppressWarnings(Project project, String commit,
			File file, int lineno, MethodDeclaration method, String warning) {
		super(project, commit);
		this.file = file;
		this.lineno = lineno;
		this.method = method;
		this.warning = warning;
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
		builder.append("<Suggestion type=\"SuppressWarning\">");
		builder.append("<Project>").append(project.getProjectId()).append("</Project>");
		builder.append("<CommitId>").append(commit).append("</CommitId>");
		builder.append("<FilePath>").append(file.getPath()).append("</FilePath>");
		builder.append("<Lineno>").append(lineno).append("</Lineno>");
		builder.append("<MethodName>").append(method.getName()).append("</MethodName>");
		builder.append("<Warning>").append(warning).append("</Warning>");
		builder.append("</Suggestion>");
		return builder.toString();
	}
	
}

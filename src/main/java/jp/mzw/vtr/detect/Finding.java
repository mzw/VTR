package jp.mzw.vtr.detect;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import jp.mzw.vtr.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.GitUtils;
import jp.mzw.vtr.git.Tag;
import jp.mzw.vtr.maven.TestCase;

import org.apache.commons.io.FileUtils;
import org.jacoco.core.analysis.IClassCoverage;

import com.hp.gagawa.java.elements.A;

public class Finding {
	
	protected Project project;
	protected Properties config;
	
	protected Commit curCommit;
	protected Commit testCommit;
	protected TestCase testCase;
	protected Tag testTag;
	
	protected Commit srcCommit;
	protected File srcFile;
	protected int srcLineno;
	protected Tag srcTag;
	
	protected List<Integer> srcLinenoList;
	
	protected boolean found;
	
	public Finding(Project project, Properties config, Commit curCommit, Commit testCommit, TestCase testCase, Tag testTag,
			Commit srcCommit, File srcFile, int srcLineno, Tag srcTag) {
		this.project = project;
		this.config = config;
		
		this.curCommit = curCommit;
		this.testCommit = testCommit;
		this.testCase = testCase;
		this.testTag = testTag;
		
		this.srcCommit = srcCommit;
		this.srcFile = srcFile;
		this.srcLineno = srcLineno;
		this.srcTag = srcTag;
		
		this.found = false;
	}
	
	public void found() {
		this.found = true;
	}
	public boolean isFound() {
		return this.found;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		try {
			builder.append(this.found ? "*" : " ").append(",")
				.append(this.srcTag.getId()).append(",")
				.append(this.srcCommit.getId()).append(",")
				.append(getSrcLineno()).append(",")
				.append(getSrcLine()).append(",");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return builder.toString();
	}

	//----------------------------------------------------------------------------------------------------
	public A getSrcBlameAnchor() {
		String url = project.getGithubUrlBlame(this.curCommit, project.getRelativePath(this.srcFile), this.srcLineno);
		return new A().setHref(url).appendText(getSrcBlameSha());
	}
	public String getUrl() {
		return project.getGithubUrlBlame(this.curCommit, project.getRelativePath(this.srcFile), this.srcLineno);
	}
	public String getSrcBlameSha() {
		return srcCommit.getId().substring(0, 6);
	}
	
	public A getSrcTagAnchor() throws IOException {
		String shortTagName = srcTag.getId().replace("refs/tags/", "");
		String url = project.getGithubUrlReleaseTag(shortTagName);
		return new A().setHref(url).appendText(shortTagName);
	}
	
	public Date getSrcDate() {
		return this.srcCommit.getDate();
	}
	
	public int getSrcLineno() {
		return this.srcLineno;
	}
	public String getSrcLine() throws IOException {
		if(srcCommit == null) return null;
		return FileUtils.readLines(srcFile).get(srcLineno-1);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static File getSrcFile(Project project, IClassCoverage cc) {
		File pkg = new File(project.getDefaultSrcDir(), cc.getPackageName());
		return new File(pkg, cc.getSourceFileName());
	}
	public static List<String> getBlameResults(Project project, Properties config, IClassCoverage cc, List<Finding> findings) throws IOException, InterruptedException {
		int lineno_min = Integer.MAX_VALUE;
		int lineno_max = Integer.MIN_VALUE;
		for(Finding finding : findings) {
			if(lineno_max < finding.srcLineno) lineno_max = finding.srcLineno;
			if(finding.srcLineno < lineno_min) lineno_min = finding.srcLineno;
		}
		File src = getSrcFile(project, cc);
		return GitUtils.blame(lineno_min, lineno_max, src, project, config, true);
	}
	public static A getSrcFileAnchor(Project project, IClassCoverage cc, Commit commit) {
		File src = getSrcFile(project, cc);
		String relativeFilePath = project.getRelativePath(src);
		String url = project.getGithubUrlBlob(commit, relativeFilePath);
		return new A().setHref(url).appendText(relativeFilePath);
	}

	public static String getSrcLine(File file, int lineno) throws IOException {
		return FileUtils.readLines(file).get(lineno-1);
	}

	public static A getTagAnchor(Project project, Tag tag) {
		if(tag == null) {
			return new A().setHref("#").appendText("latest");
		}
		String shortTagName = tag.getId().replace("refs/tags/", "");
		String url = project.getGithubUrlReleaseTag(shortTagName);
		return new A().setHref(url).appendText(shortTagName);
	}

	public static A getBlameAnchor(Project project, Commit commit, File file, int lineno) {
		String url = project.getGithubUrlBlame(commit, project.getRelativePath(file), lineno);
		return new A().setHref(url).appendText(getSrcBlameSha(commit));
	}
	public static String getSrcBlameSha(Commit commit) {
		return commit.getId().substring(0, 6);
	}
}
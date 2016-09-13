package jp.mzw.vtr.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.PrintStreamHandler;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jacoco.core.analysis.IClassCoverage;

public class Project {

	File baseDir;

	public Project(File basedir) throws IOException {
		this.baseDir = basedir;
		makeGit();
	}

	protected Git git;

	private void makeGit() throws IOException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		builder.setGitDir(new File(getBaseDir(), ".git"));
		builder.readEnvironment();
		builder.findGitDir();
		Repository repository = builder.build();
		this.git = new Git(repository);
	}

	public Git getGit() {
		return this.git;
	}

	public static Project make(Properties config) throws IOException {
		String path_to_project = config.getProperty("path_to_project") != null ? config.getProperty("path_to_project") : "path/to/project";
		String ref_to_compare = config.getProperty("ref_to_compare") != null ? config.getProperty("ref_to_compare") : "refs/heads/master";
		String github_username = config.getProperty("github_username") != null ? config.getProperty("github_username") : "GITHUB_USERNAME";
		String github_projname = config.getProperty("github_projname") != null ? config.getProperty("github_projname") : "GITHUB_PROJNAME";
		Project project = new Project(new File(path_to_project)).setGitCompareBranchName(ref_to_compare).setGithubConfig(github_username, github_projname);
		project.setConfig(config);
		return project;
	}

	protected Properties config;

	public void setConfig(Properties config) {
		this.config = config;
	}

	public Properties getConfig() {
		return this.config;
	}

	public String getRelativePath(File file) {
		String base = this.baseDir.getAbsolutePath();
		String path = file.getAbsolutePath();
		return path.replace(base + "/", "");
	}

	public String getClassName(File base, File file) {
		String base_path = base.getAbsolutePath();
		String path = file.getAbsolutePath();
		return path.replace(base_path + "/", "").replace(".class", "").replace("/", ".");
	}

	public File getBaseDir() {
		return this.baseDir;
	}

	public String getProjectName() {
		return this.baseDir.getName();
	}

	/*
	 * --------------------------------------------------------------------------
	 * -------------------------- Maven
	 * ------------------------------------------
	 * ----------------------------------------------------------
	 */
	static final String POM_FILE = "pom.xml";
	static final String SRC_DIR = "src/main/java";
	static final String TEST_DIR = "src/test/java";
	static final String TARGET_DIR = "target";
	static final String TARGET_CLASSES_DIR = "target/classes";
	static final String TARGET_TEST_CLASSES_DIR = "target/test-classes";
	static final String TARGET_DEPENDENCY_DIR = "target/dependency";

	public File getDefaultPomFile() {
		return new File(baseDir, POM_FILE);
	}

	public File getDefaultSrcDir() {
		return new File(baseDir, SRC_DIR);
	}

	public File getDefaultTestDir() {
		return new File(baseDir, TEST_DIR);
	}

	public File getDefaultTargetDir() {
		return new File(baseDir, TARGET_DIR);
	}

	public File getDefaultTargetClassesDir() {
		return new File(baseDir, TARGET_CLASSES_DIR);
	}

	public File getDefaultTargetTestClassesDir() {
		return new File(baseDir, TARGET_TEST_CLASSES_DIR);
	}

	public File getDefaultTargetDependencyDir() {
		return new File(baseDir, TARGET_DEPENDENCY_DIR);
	}

	public List<File> getTestFileList(File testDir) {
		ArrayList<File> ret = new ArrayList<File>();
		for (File file : Utils.getFiles(testDir)) {
			if (MavenUtils.isMavenTest(file)) {
				ret.add(file);
			}
		}
		return ret;
	}

	public List<File> getClassFileList(File dir) {
		ArrayList<File> ret = new ArrayList<File>();
		for (File file : Utils.getFiles(dir)) {
			if (file.getName().endsWith(".class")) {
				ret.add(file);
			}
		}
		return ret;
	}

	public InvocationResult exec(List<String> cmds) throws MavenInvocationException {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(this.getDefaultPomFile());
		request.setGoals(cmds);
		Invoker invoker = new DefaultInvoker();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		invoker.setOutputHandler(new PrintStreamHandler(ps, true));
		InvocationResult result = invoker.execute(request);

		return result;
	}

	/*
	 * JaCoCo
	 */
	public File getDefaultSrcFile(IClassCoverage cc) {
		String name = cc.getName();

		String regexInnerClass = ".*\\$.*";
		Pattern pattern = Pattern.compile(regexInnerClass);
		Matcher matcher = pattern.matcher(cc.getName());
		if (matcher.find()) {
			int index = name.indexOf("$");
			name = name.substring(0, index);
		}

		String srcFileName = name + ".java";
		return new File(this.getDefaultSrcDir(), srcFileName);
	}

	/*
	 * --------------------------------------------------------------------------
	 * -------------------------- Git
	 * --------------------------------------------
	 * --------------------------------------------------------
	 */
	protected String gitCompareBranchName;

	public Project setGitCompareBranchName(String name) {
		this.gitCompareBranchName = name;
		return this;
	}

	public String getGitCompareBranchName() {
		return this.gitCompareBranchName;
	}

	protected String githubUserName;
	protected String githubProjName;

	public Project setGithubConfig(String username, String projname) {
		this.githubUserName = username;
		this.githubProjName = projname;
		return this;
	}

	public String getGithubUserName() {
		return this.githubUserName;
	}

	public String getGithubProjName() {
		return this.githubProjName;
	}

	public String getGithubUrl() {
		StringBuilder builder = new StringBuilder();
		builder.append("https://github.com/").append(this.githubUserName).append("/").append(this.githubProjName).append("/");
		return builder.toString();
	}

	public String getGithubUrlBlame(Commit commit, File file) {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getGithubUrl()).append("blame/").append(commit.getId()).append("/").append(this.getRelativePath(file));
		return builder.toString();
	}

	public String getGithubUrlBlame(Commit commit, File file, int lineno) {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getGithubUrlBlame(commit, file)).append("#L").append(lineno);
		return builder.toString();
	}

	public String getGithubUrlBlame(Commit commit, String relative_path) {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getGithubUrl()).append("blame/").append(commit.getId()).append("/").append(relative_path);
		return builder.toString();
	}

	public String getGithubUrlBlame(Commit commit, String relative_path, int lineno) {
		StringBuilder builder = new StringBuilder();
		builder.append(getGithubUrlBlame(commit, relative_path)).append("#L").append(lineno);
		return builder.toString();
	}

	public String getGithubUrlBlob(Commit commit, String relative_path) {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getGithubUrl()).append("blob/").append(commit.getId()).append("/").append(relative_path);
		;
		return builder.toString();
	}

	public String getGithubUrlReleaseTag(String tag) {
		StringBuilder builder = new StringBuilder();
		builder.append(this.getGithubUrl()).append("releases/tag/").append(tag);
		return builder.toString();
	}

}

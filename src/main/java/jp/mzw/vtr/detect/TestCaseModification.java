package jp.mzw.vtr.detect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.CLI;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.GitUtils;
import jp.mzw.vtr.maven.TestCase;

public class TestCaseModification {
	protected static Logger LOGGER = LoggerFactory.getLogger(TestCaseModification.class);

	private Commit commit;
	private TestCase testCase;
	private List<ASTNode> newNodes;
	private List<ASTNode> oldNodes;

	public TestCaseModification(Commit commit, TestCase testCase, List<ASTNode> newNodes, List<ASTNode> oldNodes) {
		this.commit = commit;
		this.testCase = testCase;
		this.newNodes = newNodes;
		this.oldNodes = oldNodes;
	}

	public Commit getCommit() {
		return this.commit;
	}

	public TestCase getTestCase() {
		return this.testCase;
	}

	public List<ASTNode> getNewNodes() {
		return this.newNodes;
	}

	public List<ASTNode> getOldNodes() {
		return this.oldNodes;
	}

	// Above for Detector
	// --------------------------------------------------
	// Below for Cluster

	private File file;
	private List<String> revisedNodeClasses;
	private List<String> originalNodeClasses;
	
	private String projectId;
	private String commitId;
	private String clazz;
	private String method;
	

	private String commitAuthorName;
	private String commitAuthorEmailAddress;
	private String commitMessage;

	public TestCaseModification(File file, String projectId, String commitId, String clazz, String method) throws IOException {
		this.file = file;
		this.projectId = projectId;
		this.commitId = commitId;
		this.clazz = clazz;
		this.method = method;
		this.revisedNodeClasses = new ArrayList<>();
		this.originalNodeClasses = new ArrayList<>();
		parse();
	}
	
	public String getProjectId() {
		return this.projectId;
	}

	public String getCommitId() {
		return this.commitId;
	}
	
	public String getClassName() {
		return this.clazz;
	}
	
	public String getMethodName() {
		return this.method;
	}
	
	public TestCaseModification parse() throws IOException {
		String content = FileUtils.readFileToString(file);
		Document document = Jsoup.parse(content, "", Parser.xmlParser());
		for (Element element : document.select("RevisedNodes Node")) {
			String clazz = element.attr("class");
			revisedNodeClasses.add(clazz);
		}
		for (Element element : document.select("OriginalNodes Node")) {
			String clazz = element.attr("class");
			originalNodeClasses.add(clazz);
		}
		return this;
	}

	public List<String> getRevisedNodeClasses() {
		return this.revisedNodeClasses;
	}

	public List<String> getOriginalNodeClasses() {
		return this.originalNodeClasses;
	}
	
	public TestCaseModification identifyCommit() throws IOException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
		// TODO cache function
		
		Project project = new Project(this.projectId).setConfig(CLI.CONFIG_FILENAME);
		Git git = GitUtils.getGit(project.getProjectDir());
		git.checkout().setName(GitUtils.getRefToCompareBranch(git)).call();
		Iterable<RevCommit> commits = git.log().call();
		for (RevCommit commit : commits) {
			if (commit.getId().name().equals(this.commitId)) {
				this.commitAuthorName = commit.getAuthorIdent().getName();
				this.commitAuthorEmailAddress = commit.getAuthorIdent().getEmailAddress();
				this.commitMessage = commit.getFullMessage();
				break;
			}
		}
		return this;
	}
	
	public String getCsvLineHeader() {
		StringBuilder line = new StringBuilder();
		line.append(this.projectId);
		line.append(",");
		line.append(this.commitId);
		line.append(",");
		line.append(this.clazz);
		line.append(",");
		line.append(this.method);
		line.append(",");
		
		line.append(this.commitAuthorName);
		line.append(",");
		line.append(this.commitAuthorEmailAddress);
		line.append(",");
		return line.toString();
	}
	
	public ProjectCommitPair parseCommitMessage(Map<ProjectCommitPair, String> parsed) throws IOException, NoHeadException, GitAPIException {
		for (Iterator<ProjectCommitPair> it = parsed.keySet().iterator(); it.hasNext(); ) {
			ProjectCommitPair pair = it.next();
			if (pair.equals(this.projectId, this.commitId)) {
				this.commitMessage = parsed.get(pair);
				LOGGER.info("Set commit message from previous results: {} on {}", this.commitId, this.projectId);
				return null;
			}
		}
		ProjectCommitPair ret = null;
		Project project = new Project(this.projectId).setConfig(CLI.CONFIG_FILENAME);
		Git git = GitUtils.getGit(project.getProjectDir());
		git.checkout().setName(GitUtils.getRefToCompareBranch(git)).call();
		Iterable<RevCommit> commits = git.log().call();
		for (RevCommit commit : commits) {
			if (commit.getId().name().equals(this.commitId)) {
				this.commitMessage = commit.getFullMessage();
				ret = new ProjectCommitPair(this.projectId, this.commitId);
				LOGGER.info("Get commit message: {} on {}", this.commitId, this.projectId);
				break;
			}
		}
		return ret;
	}
	
	public static class ProjectCommitPair {
		private final String projectId;
		private final String commitId;
		public ProjectCommitPair(String projectId, String commitId) {
			this.projectId = projectId;
			this.commitId = commitId;
		}
		public String getProjectId() {
			return this.projectId;
		}
		public String getCommitId() {
			return this.commitId;
		}
		public boolean equals(String projectId, String commitId) {
			if (this.getProjectId().equals(projectId) && this.getCommitId().equals(commitId)) {
				return true;
			} else {
				return false;
			}
		}
	}
	
	public String getCommitMessage() {
		return this.commitMessage;
	}
	
	public List<String> getCommitMessageSplitted() {
		if (this.commitMessage == null) {
			LOGGER.warn("Commit message not found: {} on {}", this.commitId, this.projectId);
			LOGGER.warn("Git-checkout might solve this problem");
			return null;
		}
		String[] words = this.commitMessage.split("\\s+");
		return Arrays.asList(words);
	}
}

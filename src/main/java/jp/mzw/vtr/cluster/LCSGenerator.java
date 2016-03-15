package jp.mzw.vtr.cluster;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.Project;
import jp.mzw.vtr.Utils;

public class LCSGenerator extends ClusterBase {
	protected static Logger log = LoggerFactory.getLogger(LCSGenerator.class);
	
	public static void main(String[] args) throws NoHeadException, IOException, GitAPIException {
		Properties config = Utils.getConfig("vtr.properties");
		new LCSGenerator(config).generate();
	}
	
	public LCSGenerator(Properties config) throws IOException {
		super(config);
	}
	
	public LCSGenerator generate() throws IOException, NoHeadException, GitAPIException {
		File _subject_root_dir = new File(subject_root_dir);
		File _log_dir = new File(log_dir);
		
		ArrayList<String> subjects = new ArrayList<>();
		ArrayList<DetectionResult> results = new ArrayList<>();
		
		for(File file : Utils.getFiles(_log_dir)) {
			if(!isHtmlReportFile(file)) continue;
			log.info("Found detection result: " + file.getAbsolutePath());
			
			/// Instantiate induced result
			DetectionResult result = new DetectionResult(_subject_root_dir, _log_dir, file).read().parseTestMethodLines();
			/// Add subject to list
			String subjectName = result.getSubjectName();
			if(!subjects.contains(subjectName)) {
				subjects.add(subjectName);
			}
			/// Add result to list
			results.add(result);
		}
		
		/// Generate projects according to induced results
		HashMap<String, Project> projects = new HashMap<>();
		for(String subjectName : subjects) {
			projects.put(subjectName, Project.make(getConfig(subjectName)));
		}
		
		/// Parse difference between induced results and their parents
		for(DetectionResult result : results) {
			Project project = projects.get(result.getSubjectName());
			File test_file = getFile(project.getDefaultTestDir(), result.getTestClass());
			Path test_file_path = FileSystems.getDefault().getPath(test_file.getAbsolutePath());

			log.info("Analyzing: " + result.getSubjectName() + "/" + result.getCommitId() + "/" + result.getTestClass() + "#" + result.getTestMethod());
			
			RevCommit commit = getCommit(project, result.getCommitId());
			project.getGit().checkout().setName(commit.getName()).call();
			List<String> curr_src = Files.readAllLines(test_file_path, Charset.defaultCharset());
			
			RevCommit parent = getOneParent(commit);
			project.getGit().checkout().setName(parent.getName()).call();
			List<String> prev_src = null;
			if(test_file.exists()) {
				prev_src = Files.readAllLines(test_file_path, Charset.defaultCharset());
			}
			
			DiffAnalyzer diffAnalyzer = new DiffAnalyzer(prev_src, curr_src);
			diffAnalyzer.analyzeChunk(result);
			
			List<ASTNode> prev_diff_nodes = diffAnalyzer.getPrevDiffNodes();
			List<ASTNode> curr_diff_nodes = diffAnalyzer.getCurrDiffNodes();
			
			result.store(prev_diff_nodes, ".prev");
			result.store(curr_diff_nodes, ".curr");
			
			log.info("Restore Git repository head to: " + project.getGitCompareBranchName());
			project.getGit().checkout().setName(project.getGitCompareBranchName()).call();
		}

		return this;
	}
	
	private RevCommit getCommit(Project project, String commitId) throws NoHeadException, GitAPIException {
		LogCommand command = project.getGit().log();
		for(RevCommit commit : command.call()) {
			if(commitId.equals(commit.getId().getName())) {
				return commit;
			}
		}
		return null;
	}
	private RevCommit getOneParent(RevCommit commit) {
		RevCommit[] parents = commit.getParents();
		if(parents.length == 0) {
			log.error("Could not find any parents: " + commit);
			return null;
		}
		if(parents.length != 1) {
			log.warn("Found more than one parent: " + parents.length + ", " + commit);
		}
		return parents[0];
	}
	
	private Element getSubjectInfo(String subjectName) {
		for(Element e: subjects_info.select("subject name")) {
			if(e.text().equals(subjectName)) {
				return e.parent();
			}
		}
		return null;
	}
	private Properties getConfig(String subjectName) {
		Element subject_element = getSubjectInfo(subjectName);
		Properties config = new Properties();
		config.setProperty("path_to_project", subject_element.select("path").text());
		config.setProperty("ref_to_compare", subject_element.select("branch").text());
		config.setProperty("github_username", subject_element.select("github user").text());
		config.setProperty("github_projname", subject_element.select("github project").text());
		return config;
	}
	
	public static File getFile(File baseDir, String className) {
		String path = className.replace(".", "/") + ".java";
		return new File(baseDir, path);
	}
}

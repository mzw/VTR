package jp.mzw.vtr.detect;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import jp.mzw.vtr.Project;
import jp.mzw.vtr.Utils;
import jp.mzw.vtr.VtrBase;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.DictionaryMaker;
import jp.mzw.vtr.git.GitUtils;
import jp.mzw.vtr.git.Tag;
import jp.mzw.vtr.maven.JacocoRunner;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.PomModifier;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

import org.jacoco.core.analysis.CoverageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EachTestMethodRunner extends VtrBase {
	protected static Logger log = LoggerFactory.getLogger(EachTestMethodRunner.class);
	
	
	
	
	
	
	
	
	
	
	
	public static void main(String[] args) throws Exception {
		Properties config = Utils.getConfig("vtr.properties");
		Project project = Project.make(config);
		EachTestMethodRunner runner = new EachTestMethodRunner(project, config);
		runner.parseDictionary();
		runner.run();
	}
	
	List<Commit> commits;
	HashMap<Tag, ArrayList<Commit>> dict;
	public void parseDictionary() throws IOException, ParseException {
		commits = Commit.parse(DictionaryMaker.getCommits(config, project));
		Collections.sort(commits, new Comparator<Commit>() {
			@Override
			public int compare(Commit c1, Commit c2) {
				if(c1.getDate().before(c2.getDate())) return -1;
				else if(c1.getDate().after(c2.getDate())) return 1;
				return 0;
			}
		});
		
		dict = DictionaryMaker.parse(config, project);
	}
	
	public EachTestMethodRunner(Project project, Properties config) throws IOException {
		super(project, config);

		commit_id_only = project.getConfig().getProperty("commit_only");
		commit_id_until = project.getConfig().getProperty("commit_until");
		is_commit_id_until = commit_id_until != null ? true : false;
	}

	
	public void run() throws Exception {
		for(Commit commit : commits) {
			if(skip(commit.getId())) continue;
			
			GitUtils.checkout(project, config, commit.getId());
			
			if(!isMavenProject()) continue;
			
			PomModifier modifier = new PomModifier(project, config).modify();
			runEachTestMethod(commit, analyzeTestMethods());
			modifier.restore();
		}
	}


	//----------------------------------------------------------------------------------------------------
	String commit_id_only;
	String commit_id_until;
	boolean is_commit_id_until;
	private boolean skip(String commitId) {
		if(commit_id_only != null && !commitId.equals(commit_id_only)) {
			return true;
		} else if(commit_id_only != null && commitId.equals(commit_id_only)) {
			log.info("Here is given only-commit: " + commitId);
			return false;
		}
		if(is_commit_id_until) {
			if(commitId.equals(commit_id_until)) {
				log.info("Here is given until-commit: " + commitId);
				is_commit_id_until = false;
				return false;
			} else {
				return true;
			}
		}
		return false;
	}
	
	private boolean isMavenProject() {
		if(!project.getDefaultPomFile().exists()) {
			log.info("Not maven project");
			return false;
		}
		return true;
	}

	//----------------------------------------------------------------------------------------------------
	private void runEachTestMethod(Commit commit, List<TestSuite> testSuites) throws Exception {
		
		File log_dir = getLogDir(JacocoRunner.getJacocoLogBaseDir(project, config), commit);
		boolean compiled = false;
		Finder finder = new Finder(project, config, commits, dict);
		
		for(TestSuite test_suite : testSuites) {
			for(TestCase test_case : test_suite.getTestCases()) {
				// Find commits where each line of this test case was added
				List<Commit> test_commits = GitUtils.blame(
						test_case.getStartLineNumber(),
						test_case.getEndLineNumber(),
						test_suite.getTestFile(),
						project, config, commits);
				test_case.setTestCommits(test_commits);
				
				// skip if no test changed
				File exec = new File(log_dir, JacocoRunner.copyFileName(test_case));
				if(isTestNotChanged(commit, test_commits)) {
					log.info("Test not changed: " + exec.getAbsolutePath());
					
				} else {
					// If not compiled in this commit
					if(!compiled) {
						if(!MavenUtils.compile(project, config)) {
							log.error("Failed to compile: " + commit);
							return;
						}
						compiled = true;
					}
					
					// if first-time to measure coverage
					JacocoRunner runner = new JacocoRunner(project, config);
					if(isRunned(JacocoRunner.getJacocoLogBaseDir(project, config), commit, test_case)) {
						log.info("Already ran: " + exec.getAbsolutePath());
					} else {
						log.info("Running test method: " + test_case.getClassName() + "#" + test_case.getName());
						runner.maven(test_case);
						runner.copy(log_dir, test_case);
					}
					
					if(!exec.exists()) {
						log.error("Cannot measure coverage: " + exec.getAbsolutePath());
						continue;
					}
					CoverageBuilder coverage = runner.parse(exec);
					test_case.setCoverageBuilder(coverage);
					
					finder.find(commit, test_commits, test_case);
				}
			}
		}
	}

	public File getLogDir(File logBaseDir, Commit commit) {
		File logDir = new File(logBaseDir, commit.getId());
		if(!logDir.exists() && !logDir.mkdirs()) {
			log.error("Cannot make directory: " + logDir);
		}
		return logDir;
	}
	
	public boolean isTestNotChanged(Commit cur_commit, List<Commit> test_commits) {
		Date cur_commit_date = cur_commit.getDate();
		for(Commit test_commit : test_commits) {
			Date test_commit_date = test_commit.getDate();
			// Found test changed
			if(!cur_commit_date.after(test_commit_date)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isRunned(File logBaseDir, Commit commit, TestCase testCase) {
		File commitLogDir = new File(logBaseDir, commit.getId());
		if(!commitLogDir.exists()) return false;
		
		File jacocoExecFile = new File(commitLogDir, JacocoRunner.copyFileName(testCase));
		if(jacocoExecFile.exists()) return true;
		
		return false;
	}
	
	public ArrayList<TestSuite> analyzeTestMethods() throws IOException {
		ArrayList<TestSuite> testSuites = new ArrayList<TestSuite>();
		File testDir = project.getDefaultTestDir();
		for(File testFile : project.getTestFileList(testDir)) {
			TestSuite testSuite = new TestSuite(testDir, testFile).parseJuitTestCaseList();
			testSuites.add(testSuite);
		}
		return testSuites;
	}
	
}

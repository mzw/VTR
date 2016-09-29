package jp.mzw.vtr.cluster;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.detect.TestCaseModification;
import jp.mzw.vtr.dict.DictionaryParser;
import jp.mzw.vtr.git.Commit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LCSAnalyzer {
	protected static Logger LOGGER = LoggerFactory.getLogger(LCSAnalyzer.class);

	private File outputDir;
	
	private List<String> skipProjectIdList;

	public LCSAnalyzer(File outputDir) {
		this.outputDir = outputDir;
		this.skipProjectIdList = new ArrayList<>();
	}
	
	public void setSkipProjectId(List<String> projectIdList) {
		this.skipProjectIdList.addAll(projectIdList);
	}
	
	public void setSkipProjectId(String projectId) {
		this.skipProjectIdList.add(projectId);
	}

	protected List<Project> getProjects() throws IOException, ParseException {
		List<Project> ret = new ArrayList<>();
		for (File projectDir : this.outputDir.listFiles()) {
			if (projectDir.isDirectory()) {
				Project project = new Project(projectDir.getName(), projectDir);
				if (!this.skipProjectIdList.contains(project.getId())) {
					project.findTestCaseModiciations();
					ret.add(project);
				}
			}
		}
		return ret;
	}
	
	private static class Project {
		private String projectId;
		private File outputProjectDir;
		private File outputDetectDir;
		private List<Commit> commits;
		private Map<Commit, List<TestCaseModification>> results;
		private Project(String projectId, File outputProjectDir) throws IOException, ParseException {
			this.projectId = projectId;
			this.outputProjectDir = outputProjectDir;
			this.commits = DictionaryParser.parseCommits(this.outputProjectDir);
			this.outputDetectDir = new File(outputProjectDir, Detector.DETECT_DIR);
			this.results = new HashMap<>();
		}
		public String getId() {
			return this.projectId;
		}
		public Project findTestCaseModiciations() throws IOException {
			for (File dir : outputDetectDir.listFiles()) {
				if (dir.isDirectory()) {
					Commit commit = Commit.getCommitBy(dir.getName(), this.commits);
					List<TestCaseModification> tcmList = new ArrayList<>();
					for (File file : dir.listFiles()) {
						if (file.isFile()) {
							TestCaseModification tcm = new TestCaseModification(file).parse();
							if (tcm != null) {
								tcmList.add(tcm);
							}
						}
					}
					if (!tcmList.isEmpty()) {
						this.results.put(commit, tcmList);
					}
				}
			}
			return this;
		}
		public Map<Commit, List<TestCaseModification>> getTestCaseModifications() {
			return this.results;
		}
	}
	
	public void analyze() throws IOException, ParseException {
		// Set results
		List<TestCaseModification> tcmList = new ArrayList<>();
		for (Project project : this.getProjects()) {
			Map<Commit, List<TestCaseModification>> tcmMap = project.getTestCaseModifications();
			for (Commit key : tcmMap.keySet()) {
				tcmList.addAll(tcmMap.get(key));
			}
		}
		// Measure LCS
		for (int i = 0; i < tcmList.size() - 1; i++) {
			TestCaseModification result1 = tcmList.get(i);
			for (int j = i + 1; j < tcmList.size(); j++) {
				TestCaseModification result2 = tcmList.get(j);
				double sim1 = sim(result1.getRevisedNodeClasses(), result2.getRevisedNodeClasses());
				double sim2 = sim(result1.getOriginalNodeClasses(), result2.getOriginalNodeClasses());
				double dist = 1.0 - (sim1 + sim2) / 2.0;
				System.out.println(dist);
			}
		}
	}

	/**
	 * Get longest common subsequence (LCS)
	 * 
	 * @param src
	 * @param dst
	 * @return
	 */
	protected List<String> lcs(List<String> src, List<String> dst) {
		int M = src.size();
		int N = dst.size();
		int[][] opt = new int[M + 1][N + 1];
		for (int i = M - 1; i >= 0; i--) {
			for (int j = N - 1; j >= 0; j--) {
				if (src.get(i).equals(dst.get(j))) {
					opt[i][j] = opt[i + 1][j + 1] + 1;
				} else {
					opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
				}
			}
		}
		ArrayList<String> ret = new ArrayList<>();
		int i = 0, j = 0;
		while (i < M && j < N) {
			if (src.get(i).equals(dst.get(j))) {
				ret.add(src.get(i));
				i++;
				j++;
			} else if (opt[i + 1][j] >= opt[i][j + 1]) {
				i++;
			} else {
				j++;
			}
		}
		return ret;
	}

	/**
	 * Calculate LCS-based similarity
	 * @param src
	 * @param dst
	 * @return
	 */
	protected double sim(List<String> src, List<String> dst) {
		double lcs = (double) lcs(src, dst).size();
		double s = (double) src.size();
		double d = (double) dst.size();
		return lcs / (s + d - lcs);
	}

}

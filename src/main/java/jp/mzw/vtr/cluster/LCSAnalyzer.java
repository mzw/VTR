package jp.mzw.vtr.cluster;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.detect.TestCaseModification;

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
	
	protected List<TestCaseModification> parseTestCaseModifications() throws IOException {
		List<TestCaseModification> ret = new ArrayList<>();
		for (File outputProjectDir : this.outputDir.listFiles()) {
			if (!outputProjectDir.isDirectory()) {
				continue;
			}
			String projectId = outputProjectDir.getName();
			if (this.skipProjectIdList.contains(projectId)) {
				continue;
			}
			File outputDetectDir = new File(outputProjectDir, Detector.DETECT_DIR);
			if (outputDetectDir.exists()) {
				for (File outputCommitDir : outputDetectDir.listFiles()) {
					if (!outputCommitDir.isDirectory()) {
						continue;
					}
					String commitId = outputCommitDir.getName();
					for (File file : outputCommitDir.listFiles()) {
						if (!file.isFile()) {
							continue;
						}
						String fullname = file.getName().replace(".xml", "");
						String[] split = fullname.split("#");
						String clazz = split[0];
						String method = split[1];
						// new and add
						TestCaseModification tcm = new TestCaseModification(file, projectId, commitId, clazz, method); 
						ret.add(tcm);
					}
				}
			}
		}
		return ret;
	}
	
	public void analyze() throws IOException, ParseException {
		// Set results
		List<TestCaseModification> tcmList = this.parseTestCaseModifications();
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

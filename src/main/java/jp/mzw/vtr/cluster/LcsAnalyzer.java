package jp.mzw.vtr.cluster;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.detect.TestCaseModification;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LcsAnalyzer {
	protected static Logger LOGGER = LoggerFactory.getLogger(LcsAnalyzer.class);

	public static final String LCS_DIR = "lcs";
	public static final String LATEST_DIR = "latest";
	public static final String DIST_FILENAME = "dist.csv";
	public static final String HASHCODE_FILENAME = "hashcode.csv";
	
	private File outputDir;
	
	private List<String> skipProjectIdList;

	public LcsAnalyzer(File outputDir) {
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
	
	public LcsMap analyze() throws IOException, ParseException {
		// Set results
		List<TestCaseModification> tcmList = this.parseTestCaseModifications();
		// Measure LCS
		LcsMap map = new LcsMap(tcmList);
		for (int i = 0; i < tcmList.size() - 1; i++) {
			TestCaseModification result1 = tcmList.get(i);
			for (int j = i + 1; j < tcmList.size(); j++) {
				TestCaseModification result2 = tcmList.get(j);
				double sim1 = sim(result1.getRevisedNodeClasses(), result2.getRevisedNodeClasses());
				double sim2 = sim(result1.getOriginalNodeClasses(), result2.getOriginalNodeClasses());
				double dist = 1.0 - (sim1 + sim2) / 2.0;
				map.add(dist, i, j);
			}
		}
		return map;
	}
	
	public void output(LcsMap map) throws IOException {
		// root
		File rootDir = new File(this.outputDir, LCS_DIR);
		// with time-stamp
	    Calendar c = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
	    String timestamp = sdf.format(c.getTime());
	    File tsDir = new File(rootDir, timestamp);
	    // latest
	    File latestDir = new File(rootDir, LATEST_DIR);
	    // output
		output(tsDir, map);
		output(latestDir, map);
	}
	
	private void output(File dir, LcsMap map) throws IOException  {
		FileUtils.writeStringToFile(new File(dir, DIST_FILENAME), map.getCsv());
		FileUtils.writeStringToFile(new File(dir, HASHCODE_FILENAME), map.getHashcodeCsv());
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
		List<String> lcs = lcs(src, dst);
		if (lcs.isEmpty()) {
			return 0.0;
		}
		double _lcs = (double) lcs.size();
		double _s = (double) src.size();
		double _d = (double) dst.size();
		return _lcs / (_s + _d - _lcs);
	}

}

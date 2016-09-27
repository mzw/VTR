package jp.mzw.vtr.cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LCSAnalyzer {
	protected static Logger LOGGER = LoggerFactory.getLogger(LCSAnalyzer.class);

	protected File outputDir;

	public LCSAnalyzer(File outputDir) {
		this.outputDir = outputDir;
	}

	protected List<File> getDiffFiles() {
		List<File> ret = new ArrayList<>();
		for (File child : this.outputDir.listFiles()) {
			if (child.isDirectory()) {
				File diffDir = new File(child, "diff");
				if (diffDir.exists()) {
					for (File commitDir : diffDir.listFiles()) {
						for (File file : commitDir.listFiles()) {
							ret.add(file);
						}
					}
				}
			}
		}
		return ret;
	}

	protected DiffResult parse(File file) throws IOException {
		DiffResult result = new DiffResult(file);
		result.parse();
		return result;
	}

	public void analyze() throws IOException {
		// Set results
		List<DiffResult> diffResults = new ArrayList<>();
		for (File file : getDiffFiles()) {
			DiffResult result = parse(file);
			diffResults.add(result);
		}
		// Measure LCS
		for (int i = 0; i < diffResults.size() - 1; i++) {
			DiffResult diffResult1 = diffResults.get(i);
			for (int j = i + 1; j < diffResults.size(); j++) {
				DiffResult diffResult2 = diffResults.get(j);
				double sim1 = sim(diffResult1.getRevisedNodeClasses(), diffResult2.getRevisedNodeClasses());
				double sim2 = sim(diffResult1.getOriginalNodeClasses(), diffResult2.getOriginalNodeClasses());
				double dist = 1.0 - (sim1 + sim2) / 2.0;
				System.out.println(dist);
			}
		}
	}

	/**
	 * Longest common subsequence
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

	protected double sim(List<String> src, List<String> dst) {
		double lcs = (double) lcs(src, dst).size();
		double s = (double) src.size();
		double d = (double) dst.size();
		return lcs / (s + d - lcs);
	}

}

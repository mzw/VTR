package jp.mzw.vtr.cluster.similarity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LcsAnalyzer extends DistAnalyzer {
	protected static Logger LOGGER = LoggerFactory.getLogger(LcsAnalyzer.class);

	public static final String LCS = "lcs";

	public LcsAnalyzer(File outputDir) {
		super(outputDir);
	}

	@Override
	public String getMethodName() {
		return LCS;
	}

	/**
	 * Calculate LCS-based similarity
	 * @param src
	 * @param dst
	 * @return
	 */
	@Override
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

	/**
	 * Get longest common subsequence (LCS)
	 * 
	 * @param src
	 * @param dst
	 * @return
	 */
	private List<String> lcs(List<String> src, List<String> dst) {
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

}

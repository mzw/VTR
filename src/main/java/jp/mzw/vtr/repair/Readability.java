package jp.mzw.vtr.repair;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import raykernel.apps.readability.eval.Main;

public class Readability extends EvaluatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(Readability.class);

	protected Map<Repair, Result> beforeResults;
	protected Map<Repair, Result> afterResults;

	public Readability(Project project) {
		super(project);
		beforeResults = new HashMap<>();
		afterResults = new HashMap<>();
	}

	@Override
	public void evaluateBefore(Repair repair) {
		// get
		String content = repair.getOriginalPart();
		// measure
		int num = 1;
		double score = 0;
		for (int n = 1; n < content.length(); n++) {
			double sumScore = 0;
			int size = content.length() / n;
			for (int m = 1; m <= n; m++) {
				String subContent = content.substring(size * (m - 1), size * m - 1);
				double subScore = Main.getReadability(subContent);
				sumScore += subScore;
			}
			double aveScore = sumScore / n;
			if (0 < aveScore) {
				num = n;
				score = aveScore;
				break;
			}
		}
		// put
		beforeResults.put(repair, new Result(score, num));
	}

	@Override
	public void evaluateAfter(Repair repair) {
		// get
		String content = repair.getRevisedPart();
		// measure
		int num = 1;
		double score = 0;
		for (int n = 1; n < content.length(); n++) {
			double sumScore = 0;
			int size = content.length() / n;
			for (int m = 1; m <= n; m++) {
				String subContent = content.substring(size * (m - 1), size * m - 1);
				double subScore = Main.getReadability(subContent);
				sumScore += subScore;
			}
			double aveScore = sumScore / n;
			if (0 < aveScore) {
				num = n;
				score = aveScore;
				break;
			}
		}
		// put
		afterResults.put(repair, new Result(score, num));
	}

	@Override
	public void compare(Repair repair) {
		Result before = beforeResults.get(repair);
		Result after = afterResults.get(repair);

		if (before.num < after.num) {
			repair.setStatus(this, Repair.Status.Degraded);
		} else if (before.num > after.num) {
			repair.setStatus(this, Repair.Status.Improved);
		} else {
			if (before.score < after.score) {
				repair.setStatus(this, Repair.Status.Improved);
			} else if (before.score > after.score) {
				repair.setStatus(this, Repair.Status.Degraded);
			} else {
				repair.setStatus(this, Repair.Status.Stay);
			}
		}
	}

	public static final String DIRNAME = "readability";

	@Override
	public void output(List<Repair> repairs) throws IOException {
		File rootDir = EvaluatorBase.getRepairDir(outputDir, projectId);
		File dir = new File(rootDir, DIRNAME);
		if (!repairs.isEmpty() && !dir.exists()) {
			dir.mkdirs();
		}
		// csv
		StringBuilder builder = new StringBuilder();
		// header
		// common
		builder.append(EvaluatorBase.getCommonCsvHeader());
		// specific
		builder.append(",");
		builder.append("Before score").append(",");
		builder.append("Before split num").append(",");
		builder.append("After score").append(",");
		builder.append("After split num");
		// end
		builder.append("\n");
		// content
		for (Repair repair : repairs) {
			// common
			builder.append(repair.toCsv(this)).append(",");
			// specific
			Result before = beforeResults.get(repair);
			Result after = afterResults.get(repair);
			builder.append(before.getScore()).append(",");
			builder.append(before.getSplitNum()).append(",");
			builder.append(after.getScore()).append(",");
			builder.append(after.getSplitNum());
			// end
			builder.append("\n");
		}
		// write
		File csv = new File(dir, EvaluatorBase.REPAIR_FILENAME);
		FileUtils.write(csv, builder.toString());
	}

	protected static class Result {
		protected double score;
		protected int num;

		public Result(double score, int num) {
			this.score = score;
			this.num = num;
		}

		public double getScore() {
			return score;
		}

		public int getSplitNum() {
			return num;
		}
	}
}

package jp.mzw.vtr.command.eval;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import jp.mzw.vtr.repair.Repair;

public class PatchImprovement extends EvalBase {

	public static final String RESULTS_REPAIR_CONTENT_FILENAME = "results-repair-content.tex";
	public static final String RESULTS_REPAIR_CONTENT_SUMMARY_FILENAME = "results-repair-content.tex";

	/**
	 * 
	 * 
	 * @param src
	 *            path to output directory
	 * @param dst
	 *            path to directory where this method outputs results in LaTeX
	 *            format
	 * @throws IOException
	 */
	public static void evalPatchImprovement(String src, String dst) throws IOException {
		StringBuilder builder = new StringBuilder();

		int Nf = 0;
		DescriptiveStatistics A = new DescriptiveStatistics();
		DescriptiveStatistics P = new DescriptiveStatistics();
		DescriptiveStatistics O = new DescriptiveStatistics();
		DescriptiveStatistics R = new DescriptiveStatistics();

		for (Subject subject : subjects) {
			Map<Pattern, Integer> adequacyWhenFail = evalAdequacyForFailingTestCase(subject, src);
			Map<Pattern, List<Double>> adequacy = evalAdequacy(subject, src);
			Map<Pattern, List<Double>> performance = evalPerformance(subject, src);
			Map<Pattern, List<Double>> outputs = evalConciseOutputs(subject, src);
			Map<Pattern, List<Double>> readability = evalReadability(subject, src);

			builder.append(subject.getName()).append(" & ");
			for (int p = 1; p <= 16; p++) {

				// Adequacy when fail
				if (p == 1 || p == 2) {
					Integer all = new Integer(0);
					for (Pattern pattern : getPatternsBy(p)) {
						Integer elm = adequacyWhenFail.get(pattern);
						if (elm != null) {
							all += elm;
						}
					}
					if (all == 0) {
					} else {
						Nf += all;
					}
				}

				// Adequacy
				if (p == 1 || p == 2) {
					List<Double> all = new ArrayList<Double>();
					for (Pattern pattern : getPatternsBy(p)) {
						List<Double> elm = adequacy.get(pattern);
						if (elm != null) {
							all.addAll(elm);
						}
					}
					DescriptiveStatistics stat = new DescriptiveStatistics();
					for (Double elm : all) {
						stat.addValue(elm);
						A.addValue(elm);
					}
					if (stat.getN() == 0 || stat.getMean() == 0) {
						builder.append("--").append(" & ").append("--").append(" & ");
					} else {
						builder.append(stat.getN()).append(" & ").append(String.format("%.1f", stat.getMean() * 100)).append("\\% & ");
					}
				}

				// Performance
				if (p == 3 || p == 4) {
					List<Double> all = new ArrayList<Double>();
					for (Pattern pattern : getPatternsBy(p)) {
						List<Double> elm = performance.get(pattern);
						if (elm != null) {
							all.addAll(elm);
						}
					}
					DescriptiveStatistics stat = new DescriptiveStatistics();
					for (Double elm : all) {
						stat.addValue(elm);
						P.addValue(elm);
					}
					if (stat.getN() == 0 || stat.getMean() == 0) {
						builder.append("--").append(" & ").append("--").append(" & ");
					} else {
						builder.append(stat.getN()).append(" & ").append(String.format("%.1f", stat.getMean() * 100)).append("\\% & ");
					}
				}

				// Output
				if (p == 5 || p == 6 || p == 7) {
					List<Double> all = new ArrayList<Double>();
					for (Pattern pattern : getPatternsBy(p)) {
						List<Double> elm = outputs.get(pattern);
						if (elm != null) {
							all.addAll(elm);
						}
					}
					DescriptiveStatistics stat = new DescriptiveStatistics();
					for (Double elm : all) {
						stat.addValue(elm);
						O.addValue(elm);
					}
					if (stat.getN() == 0 || stat.getMean() == 0) {
						builder.append("--").append(" & ").append("--").append(" & ");
					} else {
						builder.append(stat.getN()).append(" & ").append(String.format("%.1f", stat.getMean() * 100)).append("\\% & ");
					}
				}

				// Readability
				if (8 <= p) {
					List<Double> all = new ArrayList<Double>();
					for (Pattern pattern : getPatternsBy(p)) {
						List<Double> elm = readability.get(pattern);
						if (elm != null) {
							all.addAll(elm);
						}
					}
					DescriptiveStatistics stat = new DescriptiveStatistics();
					for (Double elm : all) {
						stat.addValue(elm);
						R.addValue(elm);
					}
					if (stat.getN() == 0 || stat.getMean() == 0) {
						builder.append("--").append(" & ").append("--").append(" & ");
					} else {
						builder.append(stat.getN()).append(" & ").append(String.format("%.1f", stat.getMean() * 100)).append("\\% & ");
					}
				}

			}
			builder.append(" \\\\\n");
		}

		FileUtils.writeStringToFile(new File(dst, RESULTS_REPAIR_CONTENT_FILENAME), builder.toString());

		StringBuilder summary = new StringBuilder();
		summary.append("& \\multicolumn{4}{c|}{sum($N$) = ").append(A.getN()).append("}\n");
		summary.append("& \\multicolumn{4}{c|}{sum($N$) = ").append(P.getN()).append("}\n");
		summary.append("& \\multicolumn{6}{c|}{sum($N$) = ").append(O.getN()).append("}\n");
		summary.append("& \\multicolumn{18}{c}{sum($N$) = ").append(R.getN()).append("}\n");
		summary.append("\\\\\n");
		summary.append("& \\multicolumn{4}{c|}{ave($A$) = ").append(String.format("%.1f", A.getMean() * 100)).append("\\%}\n");
		summary.append("& \\multicolumn{4}{c|}{ave($P$) = ").append(String.format("%.1f", P.getMean() * 100)).append("\\%}\n");
		summary.append("& \\multicolumn{6}{c|}{ave($O$) = ").append(String.format("%.1f", O.getMean() * 100)).append("\\%}\n");
		summary.append("& \\multicolumn{18}{c}{ave($R$) = ").append(String.format("%.1f", R.getMean() * 100)).append("\\%}\n");
		FileUtils.writeStringToFile(new File(dst, RESULTS_REPAIR_CONTENT_SUMMARY_FILENAME), summary.toString());

		System.out.println("Nf: " + Nf);
	}

	private static Map<Pattern, List<Double>> evalAdequacy(Subject subject, String outputDir) throws IOException {
		Map<Pattern, List<Double>> ret = new HashMap<>();

		File subjectDir = new File(outputDir, subject.getId());
		File repairDir = new File(subjectDir, "repair");
		File dir = new File(repairDir, "mutation");
		File file = new File(dir, "results.csv");

		String content = FileUtils.readFileToString(file);
		CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
		List<CSVRecord> records = parser.getRecords();
		for (int i = 1; i < records.size(); i++) { // skip header
			CSVRecord record = records.get(i);
			String validator = record.get(1);
			Pattern pattern = getPatternBy(validator);
			String result = record.get(4);

			if (Repair.Status.Improved.toString().equals(result) || Repair.Status.PartiallyImproved.toString().equals(result)) {

				int before_score = Integer.parseInt(record.get(5));
				int after_score = Integer.parseInt(record.get(6));

				if (after_score == -100) { // failing test cases by patching
					// NOP
				} else {
					double improve = 1.0;
					if (before_score != 0) {
						improve = ((double) after_score - (double) before_score) / (double) before_score;
					}
					if (0 < improve) {
						List<Double> list = ret.get(pattern);
						if (list == null) {
							list = new ArrayList<>();
						}
						list.add(improve);
						ret.put(pattern, list);
					}
				}
			}
		}

		return ret;
	}

	private static Map<Pattern, Integer> evalAdequacyForFailingTestCase(Subject subject, String outputDir) throws IOException {
		Map<Pattern, Integer> ret = new HashMap<>();

		File subjectDir = new File(outputDir, subject.getId());
		File repairDir = new File(subjectDir, "repair");
		File dir = new File(repairDir, "mutation");
		File file = new File(dir, "results.csv");

		String content = FileUtils.readFileToString(file);
		CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
		List<CSVRecord> records = parser.getRecords();
		for (int i = 1; i < records.size(); i++) { // skip header
			CSVRecord record = records.get(i);
			String validator = record.get(1);
			Pattern pattern = getPatternBy(validator);
			String result = record.get(4);

			if (Repair.Status.Improved.toString().equals(result) || Repair.Status.PartiallyImproved.toString().equals(result)) {

				int after_score = Integer.parseInt(record.get(6));

				if (after_score == -100) { // failing test cases by patching
					Integer num = ret.get(pattern);
					if (num == null) {
						num = new Integer(0);
					}
					num++;
					ret.put(pattern, num);
					num++;
				} else {
					// NOP
				}
			}
		}

		return ret;
	}

	private static Map<Pattern, List<Double>> evalPerformance(Subject subject, String outputDir) throws IOException {
		Map<Pattern, List<Double>> ret = new HashMap<>();

		File subjectDir = new File(outputDir, subject.getId());
		File repairDir = new File(subjectDir, "repair");
		File dir = new File(repairDir, "performance");
		File file = new File(dir, "results.csv");

		String content = FileUtils.readFileToString(file);
		CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
		List<CSVRecord> records = parser.getRecords();
		for (int i = 1; i < records.size(); i++) { // skip header
			CSVRecord record = records.get(i);
			String validator = record.get(1);
			Pattern pattern = getPatternBy(validator);
			String result = record.get(4);

			if (Repair.Status.Improved.toString().equals(result) || Repair.Status.PartiallyImproved.toString().equals(result)) {

				long before_elapsed_time = Long.parseLong(record.get(5));
				long before_used_memory = Long.parseLong(record.get(6));
				long after_elapsed_time = Long.parseLong(record.get(7));
				long after_used_memory = Long.parseLong(record.get(8));

				if (pattern.getNum() == 3) { // CPU
					double improve = ((double) before_elapsed_time - (double) after_elapsed_time) / (double) before_elapsed_time;
					if (0 < improve) {
						List<Double> list = ret.get(pattern);
						if (list == null) {
							list = new ArrayList<>();
						}
						list.add(improve);
						ret.put(pattern, list);
					}
				} else if (pattern.getNum() == 4) { // Mem
					double improve = ((double) before_used_memory - (double) after_used_memory) / (double) before_used_memory;
					if (0 < improve) {
						List<Double> list = ret.get(pattern);
						if (list == null) {
							list = new ArrayList<>();
						}
						list.add(improve);
						ret.put(pattern, list);
					}
				}
			}
		}

		return ret;
	}

	private static Map<Pattern, List<Double>> evalConciseOutputs(Subject subject, String outputDir) throws IOException {
		Map<Pattern, List<Double>> ret = new HashMap<>();

		File subjectDir = new File(outputDir, subject.getId());
		File repairDir = new File(subjectDir, "repair");
		File dir = new File(repairDir, "output");

		{
			File file = new File(dir, "compile.csv");
			String content = FileUtils.readFileToString(file);
			CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
			List<CSVRecord> records = parser.getRecords();
			for (int i = 1; i < records.size(); i++) { // skip header
				CSVRecord record = records.get(i);
				String validator = record.get(1);
				Pattern pattern = getPatternBy(validator);
				String result = record.get(4);

				if (Repair.Status.Improved.toString().equals(result) || Repair.Status.PartiallyImproved.toString().equals(result)) {

					int before_output_lines = Integer.parseInt(record.get(5));
					int after_output_lines = Integer.parseInt(record.get(7));

					if (pattern.getNum() == 5) { // compile
						double improve = ((double) before_output_lines - (double) after_output_lines) / (double) before_output_lines;
						if (0 < improve) {
							List<Double> list = ret.get(pattern);
							if (list == null) {
								list = new ArrayList<>();
							}
							list.add(improve);
							ret.put(pattern, list);
						}
					}
				}
			}
		}
		{
			File file = new File(dir, "javadoc.csv");
			String content = FileUtils.readFileToString(file);
			CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
			List<CSVRecord> records = parser.getRecords();
			for (int i = 1; i < records.size(); i++) { // skip header
				CSVRecord record = records.get(i);
				String validator = record.get(1);
				Pattern pattern = getPatternBy(validator);
				String result = record.get(4);

				if (Repair.Status.Improved.toString().equals(result) || Repair.Status.PartiallyImproved.toString().equals(result)) {

					int before_lines = Integer.parseInt(record.get(5));
					int after_lines = Integer.parseInt(record.get(6));

					if (pattern.getNum() == 7) { // compile
						double improve = ((double) before_lines - (double) after_lines) / (double) before_lines;
						if (0 < improve) {
							List<Double> list = ret.get(pattern);
							if (list == null) {
								list = new ArrayList<>();
							}
							list.add(improve);
							ret.put(pattern, list);
						}
					}
				}
			}
		}
		{
			File file = new File(dir, "runtime.csv");
			String content = FileUtils.readFileToString(file);
			CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
			List<CSVRecord> records = parser.getRecords();
			for (int i = 1; i < records.size(); i++) { // skip header
				CSVRecord record = records.get(i);
				String validator = record.get(1);
				Pattern pattern = getPatternBy(validator);
				String result = record.get(4);

				if (Repair.Status.Improved.toString().equals(result) || Repair.Status.PartiallyImproved.toString().equals(result)) {

					int before_lines = Integer.parseInt(record.get(5));
					int after_lines = Integer.parseInt(record.get(6));

					if (pattern.getNum() == 6) { // compile
						double improve = ((double) before_lines - (double) after_lines) / (double) before_lines;
						if (0 < improve) {
							List<Double> list = ret.get(pattern);
							if (list == null) {
								list = new ArrayList<>();
							}
							list.add(improve);
							ret.put(pattern, list);
						}
					}
				}
			}
		}

		return ret;
	}

	private static Map<Pattern, List<Double>> evalReadability(Subject subject, String outputDir) throws IOException {
		Map<Pattern, List<Double>> ret = new HashMap<>();

		File subjectDir = new File(outputDir, subject.getId());
		File repairDir = new File(subjectDir, "repair");
		File dir = new File(repairDir, "readability");
		File file = new File(dir, "results.csv");

		String content = FileUtils.readFileToString(file);
		CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
		List<CSVRecord> records = parser.getRecords();
		for (int i = 1; i < records.size(); i++) { // skip header
			CSVRecord record = records.get(i);
			String validator = record.get(1);
			Pattern pattern = getPatternBy(validator);
			String result = record.get(4);
			double before_score = Double.parseDouble(record.get(5));
			int before_split_num = Integer.parseInt(record.get(6));
			double after_score = Double.parseDouble(record.get(7));
			int after_split_num = Integer.parseInt(record.get(8));

			if (Repair.Status.Improved.toString().equals(result) || Repair.Status.PartiallyImproved.toString().equals(result)) {
				double improve = 0;
				if (before_split_num == after_split_num) {
					improve = after_score - before_score;
				} else {
					improve = after_score;
				}
				List<Double> list = ret.get(pattern);
				if (list == null) {
					list = new ArrayList<>();
				}
				list.add(improve);
				ret.put(pattern, list);
			}
		}

		return ret;
	}
}

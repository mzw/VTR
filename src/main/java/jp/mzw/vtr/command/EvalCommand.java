package jp.mzw.vtr.command;

import jp.mzw.vtr.CLI;
import jp.mzw.vtr.command.eval.PatchImprovement;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.dict.Dictionary;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.CheckoutListener;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestSuite;
import jp.mzw.vtr.repair.Repair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static jp.mzw.vtr.CLI.CONFIG_FILENAME;

/**
 * evaluate detecting results and repairing results
 */
public class EvalCommand {
	public static void command(String... args) throws IOException, ParseException, GitAPIException {
		String type = args[0];
		eval(type, Arrays.copyOfRange(args, 1, args.length));
	}

	private static void eval(String type, String... args) throws IOException, ParseException, GitAPIException {
		if ("Nt".equals(type)) {
			String projectId = args[0];
			Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
			Nt(project);
		} else if ("detect".equals(type)) {
			// Parse
			String path_to_file = args[0];
			detect(path_to_file);
		} else if ("improved".equals(type)) {
			// Get records
			String path_to_file = args[0];
			File file = new File(path_to_file);
			String content = FileUtils.readFileToString(file);
			CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
			List<CSVRecord> records = parser.getRecords();
			// Traverse
			String fileName = file.getName();
			String evaluation = file.getParent();
			StringBuilder builder = new StringBuilder();
			if (evaluation.endsWith("mutation")) {
				Map<CSVRecord, Double> improvedRecords = CommandUtils.improvedMutationRecords(records);
				for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
					CSVRecord record = entry.getKey();
					double rate = entry.getValue();
					for (int i = 0; i < 5; i++) { // until Result
						builder.append(record.get(i)).append(",");
					}
					builder.append(rate).append("\n");
				}
				FileUtils.write(new File(file.getParent(), "mutation_improve.csv"), builder.toString());
			} else if (evaluation.endsWith("readability")) {
				Map<CSVRecord, Double> improvedRecords = CommandUtils.improvedReadabilityRecords(records);
				for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
					CSVRecord record = entry.getKey();
					double rate = entry.getValue();
					for (int i = 0; i < 5; i++) { // until Result
						builder.append(record.get(i)).append(",");
					}
					builder.append(rate).append("\n");
				}
				FileUtils.write(new File(file.getParent(), "readability_improve.csv"), builder.toString());
			} else if (evaluation.endsWith("performance")) {
				Map<CSVRecord, Pair<Double, Double>> improvedRecords = CommandUtils.improvedPerformanceRecords(records);
				Map<CSVRecord, Pair<Double, Double>> partiallyImprovedRecords = CommandUtils.partiallyImprovedPerformanceRecords(records);
				for (Map.Entry<CSVRecord, Pair<Double, Double>> entry : improvedRecords.entrySet()) {
					CSVRecord record = entry.getKey();
					Pair<Double, Double> pair = entry.getValue();
					double elapsedTimeImproveRate = pair.getLeft();
					double usedMemoryImproveRate = pair.getRight();
					for (int i = 0; i < 5; i++) { // until Result
						builder.append(record.get(i)).append(",");
					}
					builder.append(elapsedTimeImproveRate).append(",");
					builder.append(usedMemoryImproveRate).append("\n");
				}
				for (Map.Entry<CSVRecord, Pair<Double, Double>> entry : partiallyImprovedRecords.entrySet()) {
					CSVRecord record = entry.getKey();
					Pair<Double, Double> pair = entry.getValue();
					double elapsedTimeImproveRate = pair.getLeft();
					double usedMemoryImproveRate = pair.getRight();
					for (int i = 0; i < 5; i++) { // until Result
						builder.append(record.get(i)).append(",");
					}
					builder.append(elapsedTimeImproveRate).append(",");
					builder.append(usedMemoryImproveRate).append("\n");
				}
				FileUtils.write(new File(file.getParent(), "performance_improve.csv"), builder.toString());
				FileUtils.write(new File(file.getParent(), "performance_partially_improve.csv"), builder.toString());
			} else if (evaluation.endsWith("output")) {
				if (fileName.endsWith("compile.csv")) {
					Map<CSVRecord, Double> improvedRecords = CommandUtils.improvedCompileOutputRecords(records);
					for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
						CSVRecord record = entry.getKey();
						double rate = entry.getValue();
						for (int i = 0; i < 5; i++) { // until Result
							builder.append(record.get(i)).append(",");
						}
						builder.append(rate).append("\n");
					}
					FileUtils.write(new File(file.getParent(), "compile_improve.csv"), builder.toString());
				} else if (fileName.endsWith("javadoc.csv")) {
					Map<CSVRecord, Double> improvedRecords = CommandUtils.improvedJavadocOutputRecords(records);
					for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
						CSVRecord record = entry.getKey();
						double rate = entry.getValue();
						for (int i = 0; i < 5; i++) { // until Result
							builder.append(record.get(i)).append(",");
						}
						builder.append(rate).append("\n");
					}
					FileUtils.write(new File(file.getParent(), "javadoc_improve.csv"), builder.toString());
				} else if (fileName.endsWith("runtime.csv")) {
					Map<CSVRecord, Double> improvedRecords = CommandUtils.improvedRuntimeOutputRecords(records);
					for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
						CSVRecord record = entry.getKey();
						double rate = entry.getValue();
						for (int i = 0; i < 5; i++) { // until Result
							builder.append(record.get(i)).append(",");
						}
						builder.append(rate).append("\n");
					}
					FileUtils.write(new File(file.getParent(), "runtime_improve.csv"), builder.toString());
				}
			}
		} else if ("detect-validate".equals(type)) {
			// Get records
			String path_to_detect_file = args[0];
			File detect_file = new File(path_to_detect_file);
			String detect_content = FileUtils.readFileToString(detect_file);
			CSVParser detect_parser = CSVParser.parse(detect_content, CSVFormat.DEFAULT);
			List<CSVRecord> detect_records = detect_parser.getRecords();
			Map<String, List<Long>> elapsedDays = new HashMap<>();
			// read
			for (int i = 1; i < detect_records.size(); i++) {
				CSVRecord detect_record = detect_records.get(i);
				String subject = detect_record.get(1);
				Project project = new Project(subject).setConfig(CONFIG_FILENAME);
				jp.mzw.vtr.dict.Dictionary dictionary = new Dictionary(project.getOutputDir(), project.getProjectId()).parse();
				String clazz = detect_record.get(3);
				String method = detect_record.get(4);
				String itemId = detect_record.get(13);
				String patternId = CommandUtils.patternIdFromItemId(itemId);
				File validate_file;
				if (patternId.equals("#1") || patternId.equals("#2")) {
					validate_file = new File(new File(project.getOutputDir(), "mutation"), "results.csv");
				} else if (patternId.equals("#3") || patternId.equals("#4")) {
					validate_file = new File(new File(project.getOutputDir(), "performance"), "results.csv");
				} else if (patternId.equals("#5")) {
					validate_file = new File(new File(project.getOutputDir(), "output"), "compile.csv");
				} else if (patternId.equals("#6")) {
					validate_file = new File(new File(project.getOutputDir(), "output"), "runtime.csv");
				} else if (patternId.equals("#7")) {
					validate_file = new File(new File(project.getOutputDir(), "output"), "javadoc.csv");
				} else if (patternId.equals("#8") || patternId.equals("#9") || patternId.equals("#10") || patternId.equals("#11") || patternId.equals("#12")
						|| patternId.equals("#13") || patternId.equals("#14") || patternId.equals("#15") || patternId.equals("#16")) {
					validate_file = new File(new File(project.getOutputDir(), "readability"), "results.csv");
				} else {
					continue;
				}
				String validate_content = FileUtils.readFileToString(validate_file);
				CSVParser validate_parser = CSVParser.parse(validate_content, CSVFormat.DEFAULT);
				List<CSVRecord> validate_records = validate_parser.getRecords();
				for (int j = 1; j < validate_records.size(); j++) {
					CSVRecord validate_record = validate_records.get(i);
					if (!validate_record.get(4).equals(Repair.Status.Improved) || !validate_record.get(4).equals(Repair.Status.PartiallyImproved)) {
						continue;
					}
					if (!clazz.equals(validate_record.get(2)) || !method.equals(validate_record.get(3))) {
						continue;
					}
					String detectCommitId = detect_record.get(2);
					Commit detectCommit = dictionary.getCommitBy(detectCommitId);
					long detectCommitTime = detectCommit.getDate().getTime();
					String validateCommitId = validate_record.get(0);
					Commit validateCommit = dictionary.getCommitBy(validateCommitId);
					long validateCommitTime = validateCommit.getDate().getTime();
					long elapsedDayData = (detectCommitTime - validateCommitTime) / (1000 * 60 * 60 * 24);
					List<Long> elapsedDayDatas = elapsedDays.get(subject);
					if (elapsedDayDatas == null) {
						elapsedDayDatas = new ArrayList<>();
					}
					elapsedDayDatas.add(elapsedDayData);
				}
			}
			// write
			StringBuilder builder = new StringBuilder();
			for (String subject : elapsedDays.keySet()) {
				builder.append(subject).append(",");
				List<Long> elapsedDayDatas = elapsedDays.get(subject);
				for (Long elapsedDay : elapsedDayDatas) {
					builder.append(elapsedDay).append(",");
				}
				builder.append("\n");
			}
			FileUtils.write(new File("reality_of_the_challenge.csv"), builder.toString());
		} else if ("improved-version2".equals(type)) {
			// Get records
			String path_to_file = args[0];
			String subject = args[1];
			String path_to_output = args[2];
			File file = new File(path_to_file);
			String content = FileUtils.readFileToString(file);
			CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
			List<CSVRecord> records = parser.getRecords();
			// Traverse
			String fileName = file.getName();
			String evaluation = file.getParent();
			StringBuilder builder = new StringBuilder();
			if (evaluation.endsWith("mutation")) {
				Map<CSVRecord, Double> improvedRecords = CommandUtils.improvedMutationRecords(records);
				DescriptiveStatistics stats1 = new DescriptiveStatistics();
				DescriptiveStatistics stats2 = new DescriptiveStatistics();
				for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
					CSVRecord record = entry.getKey();
					double num = entry.getValue();
					if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#1")) {
						stats1.addValue(num);
					} else if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#2")) {
						stats2.addValue(num);
					}
				}
				builder.append("#1").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats1.getN()).append(",").append(stats1.getSum() / stats1.getN()).append(",").append(stats1.getStandardDeviation())
						.append("\n");
				builder.append("#2").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats2.getN()).append(",").append(stats2.getSum() / stats2.getN()).append(",").append(stats2.getStandardDeviation())
						.append("\n");
				FileUtils.write(new File(new File(path_to_output, subject), "mutation_improve.csv"), builder.toString());
			} else if (evaluation.endsWith("performance")) {
				Map<CSVRecord, Pair<Double, Double>> improvedRecords = CommandUtils.improvedPerformanceRecords(records);
				DescriptiveStatistics stats3 = new DescriptiveStatistics();
				DescriptiveStatistics stats4 = new DescriptiveStatistics();
				for (Map.Entry<CSVRecord, Pair<Double, Double>> entry : improvedRecords.entrySet()) {
					CSVRecord record = entry.getKey();
					Pair<Double, Double> pair = entry.getValue();
					double elapsedTimeImproveRate = pair.getLeft();
					double usedMemoryImproveRate = pair.getRight();
					if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#3")) {
						stats3.addValue(elapsedTimeImproveRate);
					} else if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#4")) {
						stats4.addValue(usedMemoryImproveRate);
					}
				}
				builder.append("#3").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats3.getN()).append(",").append(stats3.getSum() / stats3.getN()).append(",").append(stats3.getStandardDeviation())
						.append("\n");
				builder.append("#4").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats4.getN()).append(",").append(stats4.getSum() / stats4.getN()).append(",").append(stats4.getStandardDeviation())
						.append("\n");
				FileUtils.write(new File(new File(path_to_output, subject), "performance_improve.csv"), builder.toString());
			} else if (evaluation.endsWith("output")) {
				if (fileName.endsWith("compile.csv")) {
					Map<CSVRecord, Double> improvedRecords = CommandUtils.improvedCompileOutputRecords(records);
					DescriptiveStatistics stats5 = new DescriptiveStatistics();
					for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
						CSVRecord record = entry.getKey();
						double rate = entry.getValue();
						if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#5")) {
							stats5.addValue(rate);
						}
					}
					builder.append("#5").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
					builder.append(",").append(stats5.getN()).append(",").append(stats5.getSum() / stats5.getN()).append(",")
							.append(stats5.getStandardDeviation()).append("\n");
					FileUtils.write(new File(new File(path_to_output, subject), "compile_improve.csv"), builder.toString());

				} else if (fileName.endsWith("runtime.csv")) {
					Map<CSVRecord, Double> improvedRecords = CommandUtils.improvedRuntimeOutputRecords(records);
					DescriptiveStatistics stats6 = new DescriptiveStatistics();
					for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
						CSVRecord record = entry.getKey();
						double rate = entry.getValue();
						if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#6")) {
							stats6.addValue(rate);
						}
					}
					builder.append("#6").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
					builder.append(",").append(stats6.getN()).append(",").append(stats6.getSum() / stats6.getN()).append(",")
							.append(stats6.getStandardDeviation()).append("\n");
					FileUtils.write(new File(new File(path_to_output, subject), "runtime_improve.csv"), builder.toString());
				} else if (fileName.endsWith("javadoc.csv")) {
					Map<CSVRecord, Double> improvedRecords = CommandUtils.improvedJavadocOutputRecords(records);
					DescriptiveStatistics stats7 = new DescriptiveStatistics();
					for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
						CSVRecord record = entry.getKey();
						double rate = entry.getValue();
						if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#7")) {
							stats7.addValue(rate);
						}
					}
					builder.append("#7").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
					builder.append(",").append(stats7.getN()).append(",").append(stats7.getSum() / stats7.getN()).append(",")
							.append(stats7.getStandardDeviation()).append("\n");
					FileUtils.write(new File(new File(path_to_output, subject), "javadoc_improve.csv"), builder.toString());
				}
			} else if (evaluation.endsWith("readability")) {
				Map<CSVRecord, Double> improvedRecords = CommandUtils.improvedReadabilityRecords(records);
				DescriptiveStatistics stats8 = new DescriptiveStatistics();
				DescriptiveStatistics stats9 = new DescriptiveStatistics();
				DescriptiveStatistics stats10 = new DescriptiveStatistics();
				DescriptiveStatistics stats11 = new DescriptiveStatistics();
				DescriptiveStatistics stats12 = new DescriptiveStatistics();
				DescriptiveStatistics stats13 = new DescriptiveStatistics();
				DescriptiveStatistics stats14 = new DescriptiveStatistics();
				DescriptiveStatistics stats15 = new DescriptiveStatistics();
				DescriptiveStatistics stats16 = new DescriptiveStatistics();
				for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
					CSVRecord record = entry.getKey();
					double rate = entry.getValue();
					if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#8")) {
						stats8.addValue(rate);
					} else if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#9")) {
						stats9.addValue(rate);
					} else if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#10")) {
						stats10.addValue(rate);
					} else if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#11")) {
						stats11.addValue(rate);
					} else if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#12")) {
						stats12.addValue(rate);
					} else if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#13")) {
						stats13.addValue(rate);
					} else if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#14")) {
						stats14.addValue(rate);
					} else if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#15")) {
						stats15.addValue(rate);
					} else if (CommandUtils.patternIdFromValidatorName(record.get(1)).equals("#16")) {
						stats16.addValue(rate);
					}
				}
				builder.append("#8").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats8.getN()).append(",").append(stats8.getSum() / stats8.getN()).append(",").append(stats8.getStandardDeviation())
						.append("\n");
				builder.append("#9").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats9.getN()).append(",").append(stats9.getSum() / stats9.getN()).append(",").append(stats9.getStandardDeviation())
						.append("\n");
				builder.append("#10").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats10.getN()).append(",").append(stats10.getSum() / stats10.getN()).append(",")
						.append(stats10.getStandardDeviation()).append("\n");
				builder.append("#11").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats11.getN()).append(",").append(stats11.getSum() / stats11.getN()).append(",")
						.append(stats11.getStandardDeviation()).append("\n");
				builder.append("#12").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats12.getN()).append(",").append(stats12.getSum() / stats12.getN()).append(",")
						.append(stats12.getStandardDeviation()).append("\n");
				builder.append("#13").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats13.getN()).append(",").append(stats13.getSum() / stats13.getN()).append(",")
						.append(stats13.getStandardDeviation()).append("\n");
				builder.append("#14").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats14.getN()).append(",").append(stats14.getSum() / stats14.getN()).append(",")
						.append(stats14.getStandardDeviation()).append("\n");
				builder.append("#15").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats15.getN()).append(",").append(stats15.getSum() / stats15.getN()).append(",")
						.append(stats15.getStandardDeviation()).append("\n");
				builder.append("#16").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats16.getN()).append(",").append(stats16.getSum() / stats16.getN()).append(",")
						.append(stats16.getStandardDeviation()).append("\n");
				FileUtils.write(new File(new File(path_to_output, subject), "readability_improve.csv"), builder.toString());
			}
		} else if ("latex-table".equals(type)) {
			String pathToOutputDir = args[0];
			Project project = new Project("dummy").setConfig(CLI.CONFIG_FILENAME);
			PatchImprovement.evalPatchImprovement(project.getOutputDir().getPath(), pathToOutputDir);
		}
	}

	private static void Nt(Project project) throws IOException, ParseException, GitAPIException {
		CheckoutConductor cc = new CheckoutConductor(project);
		final Map<Commit, Integer> map = new HashMap<>();
		cc.addListener(new CheckoutListener(project) {
			@Override
			public void onCheckout(Commit commit) {
				try {
					int num = 0;
					for (TestSuite ts : MavenUtils.getTestSuites(this.getProject().getProjectDir())) {
						num += ts.getTestCases().size();
					}
					if (0 < num) {
						map.put(commit, new Integer(num));
					}
				} catch (IOException e) {
					// NOP
				}
			}
		});
		cc.checkout();
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (Commit commit : map.keySet()) {
			Integer number = map.get(commit);
			if (0 < number) {
				stats.addValue(number);
			}
		}
		File dir = new File(project.getOutputDir(), "Nt");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		File file = new File(dir, project.getProjectId() + ".txt");
		StringBuilder builder = new StringBuilder();
		builder.append("N: ").append(stats.getN()).append("\n");
		builder.append("Max: ").append(stats.getMax()).append("\n");
		builder.append("Min: ").append(stats.getMin()).append("\n");
		builder.append("Mean: ").append(stats.getMean()).append("\n");
		builder.append("Std-dev: ").append(stats.getStandardDeviation()).append("\n");
		FileUtils.write(file, builder);
	}

	// analyze detecting results
	private static void detect(String path_to_file) throws IOException {
		File file = new File(path_to_file);
		String content = FileUtils.readFileToString(file);
		CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
		List<CSVRecord> records = parser.getRecords();
		// Traverse
		Map<String, Map<String, Integer>> patternsBySubject = new HashMap<>();
		Map<String, Map<String, Integer>> subjectsByPattern = new HashMap<>();
		for (int i = 1; i < records.size(); i++) { // skip header
			CSVRecord record = records.get(i);
			String subject = record.get(1);
			String pattern = record.get(13);
			// Read
			{
				Map<String, Integer> numberByPattern = patternsBySubject.get(subject);
				if (numberByPattern == null) {
					numberByPattern = new HashMap<>();
				}
				Integer number = numberByPattern.get(pattern);
				if (number == null) {
					number = new Integer(1);
				} else {
					number = new Integer(number + 1);
				}
				numberByPattern.put(pattern, number);
				patternsBySubject.put(subject, numberByPattern);
			}
			{
				Map<String, Integer> numberBySubject = subjectsByPattern.get(pattern);
				if (numberBySubject == null) {
					numberBySubject = new HashMap<>();
				}
				Integer number = numberBySubject.get(subject);
				if (number == null) {
					number = new Integer(1);
				} else {
					number = new Integer(number + 1);
				}
				numberBySubject.put(subject, number);
				subjectsByPattern.put(pattern, numberBySubject);
			}
		}
		// Print
		{
			StringBuilder builder = new StringBuilder();
			for (String subject : patternsBySubject.keySet()) {
				Map<String, Integer> patterns = patternsBySubject.get(subject);
				builder.append(subject).append("\n");
				for (String pattern : patterns.keySet()) {
					Integer number = patterns.get(pattern);
					builder.append("\t").append(pattern).append(": ").append(number).append("\n");
				}
			}
			FileUtils.write(new File(file.getParent(), "patterns_by_subject.txt"), builder.toString());
		}
		{
			StringBuilder builder = new StringBuilder();
			for (String pattern : subjectsByPattern.keySet()) {
				Map<String, Integer> subjects = subjectsByPattern.get(pattern);
				builder.append(pattern).append("\n");
				for (String subject : subjects.keySet()) {
					Integer number = subjects.get(subject);
					builder.append("\t").append(subject).append(": ").append(number).append("\n");
				}
			}
			FileUtils.write(new File(file.getParent(), "subjects_by_patterns.txt"), builder.toString());
		}
	}
}

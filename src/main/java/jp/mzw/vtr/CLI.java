package jp.mzw.vtr;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.*;

import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.dict.*;
import jp.mzw.vtr.dict.Dictionary;
import jp.mzw.vtr.git.*;
import jp.mzw.vtr.repair.Repair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.dom4j.DocumentException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.parser.Parser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import difflib.PatchFailedException;
import jp.mzw.vtr.cluster.HCluster;
import jp.mzw.vtr.cluster.similarity.DistAnalyzer;
import jp.mzw.vtr.cluster.similarity.DistMap;
import jp.mzw.vtr.cluster.visualize.VisualizerBase;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.detect.TestCaseModification;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestRunner;
import jp.mzw.vtr.maven.TestSuite;
import jp.mzw.vtr.repair.EvaluatorBase;
import jp.mzw.vtr.repair.RepairEvaluator;
import jp.mzw.vtr.validate.ValidationResult;
import jp.mzw.vtr.validate.Validator;
import jp.mzw.vtr.validate.ValidatorBase;

public class CLI {
	static Logger LOGGER = LoggerFactory.getLogger(CLI.class);

	public static final String CONFIG_FILENAME = "config.properties";

	public static void main(String[] args) throws IOException, NoHeadException, GitAPIException, ParseException, MavenInvocationException, DocumentException,
			PatchFailedException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException, ClassNotFoundException, InterruptedException {

		if (args.length < 1) { // Invalid usage
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI dict      <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov       <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov       <subject-id> At    <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov       <subject-id> After <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI detect    <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI detect    <subject-id> At    <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI detect    <subject-id> After <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cluster   <similarity> <cluster-method> <threshold>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI visualize <method>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI validate  <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI validate  <subject-id> At    <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI validate  <subject-id> After <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI gen       <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI repair    <subject-id>");
			return;
		}

		String command = args[0];
		if ("dict".equals(command)) {
			String projectId = args[1];
			Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
			dict(project);
		} else if ("cov".equals(command)) {
			String projectId = args[1];
			Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
			if (args.length == 2) { // all commits
				cov(project);
			} else if (args.length == 4) { // specific commit(s)
				CheckoutConductor.Type type = CheckoutConductor.Type.valueOf(args[2]);
				String commitId = args[3];
				cov(project, type, commitId);
			} else {
				LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov <subject-id>");
				LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov <subject-id> At    <commit-id>");
				LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov <subject-id> After <commit-id>");
			}
		} else if ("detect".equals(command)) {
			String projectId = args[1];
			Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
			if (args.length == 2) { // all commits
				detect(project);
			} else if (args.length == 4) { // specific commit(s)
				CheckoutConductor.Type type = CheckoutConductor.Type.valueOf(args[2]);
				String commitId = args[3];
				detect(project, type, commitId);
			} else {
				LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI detect <subject-id>");
				LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI detect <subject-id> At    <commit-id>");
				LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI detect <subject-id> After <commit-id>");
			}
		} else if ("cluster".equals(command)) {
			if (args.length == 4) {
				String analyzer = args[1];
				String strategy = args[2];
				double threshold = Double.parseDouble(args[3]);
				cluster(new Project(null).setConfig(CONFIG_FILENAME), analyzer, strategy, threshold);
			} else {
				LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cluster <similarity> <cluster-method> <threshold>");
			}
		} else if ("visualize".equals(command)) {
			String method = args[1];
			VisualizerBase visualizer = VisualizerBase.visualizerFactory(method, new Project(null).setConfig(CONFIG_FILENAME).getOutputDir());
			if (visualizer != null) {
				visualizer.loadGeneratedSourceFileList(Detector.GENERATED_SOURCE_FILE_LIST).visualize();
			} else {
				LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI visualize <method>");
			}
		} else if ("validate".equals(command)) {
			String projectId = args[1];
			Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
			if (args.length == 2) { // all commits
				validate(project);
			} else if (args.length == 4) { // specific commit(s)
				CheckoutConductor.Type type = CheckoutConductor.Type.valueOf(args[2]);
				String commitId = args[3];
				validate(project, type, commitId);
			} else {
				LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI validate <subject-id>");
				LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI validate <subject-id> At    <commit-id>");
				LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI validate <subject-id> After <commit-id>");
			}
		} else if ("gen".equals(command)) {
			String projectId = args[1];
			Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
			gen(project);
		} else if ("repair".equals(command)) {
			String projectId = args[1];
			Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
			repair(project);
		}
		// For us
		else if ("eval".equals(command)) {
			String type = args[1];
			eval(type, Arrays.copyOfRange(args, 2, args.length));
		}

		LOGGER.info("Finished: {}", command);
	}

	private static void dict(Project project) throws IOException, NoHeadException, GitAPIException {
		Git git = GitUtils.getGit(project.getProjectDir());
		DictionaryMaker dm = new DictionaryMaker(git);
		String refToComareBranch = GitUtils.getRefToCompareBranch(git);
		Map<Ref, Collection<RevCommit>> tagCommitsMap = dm.getTagCommitsMap(refToComareBranch);

		File dir = new File(project.getOutputDir(), project.getProjectId());
		dm.writeCommitListInXML(dir);
		dm.writeDictInXML(tagCommitsMap, refToComareBranch, dir);
	}

	private static void cov(Project project) throws IOException, ParseException, GitAPIException {
		CheckoutConductor cc = new CheckoutConductor(project);
		cc.addListener(new TestRunner(project));
		cc.checkout();
	}

	private static void cov(Project project, CheckoutConductor.Type type, String commitId) throws IOException, ParseException, GitAPIException {
		CheckoutConductor cc = new CheckoutConductor(project);
		cc.addListener(new TestRunner(project));
		cc.checkout(type, commitId);
	}

	private static void detect(Project project) throws IOException, ParseException, GitAPIException {
		CheckoutConductor cc = new CheckoutConductor(project);
		cc.addListener(new Detector(project).loadGeneratedSourceFileList(Detector.GENERATED_SOURCE_FILE_LIST));
		cc.checkout();
	}

	private static void detect(Project project, CheckoutConductor.Type type, String commitId) throws IOException, ParseException, GitAPIException {
		CheckoutConductor cc = new CheckoutConductor(project);
		cc.addListener(new Detector(project).loadGeneratedSourceFileList(Detector.GENERATED_SOURCE_FILE_LIST));
		cc.checkout(type, commitId);
	}

	private static void cluster(Project project, String analyzer, String strategy, double threshold)
			throws IOException, ParseException, NoHeadException, GitAPIException {
		// Similarity
		DistAnalyzer distAnalyzer = DistAnalyzer.analyzerFactory(project.getOutputDir(), analyzer);
		List<TestCaseModification> tcmList = distAnalyzer.parseTestCaseModifications();
		DistMap map = distAnalyzer.analyze(tcmList);
		String timestamp = distAnalyzer.output(map);
		// Clustering
		HCluster cluster = new HCluster(project.getOutputDir(), distAnalyzer.getMethodName()).parse();
		cluster.cluster(HCluster.getStrategy(strategy), threshold);
		cluster.output(timestamp);
	}

	private static void validate(Project project) throws IOException, ParseException, GitAPIException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, InterruptedException {
		CheckoutConductor cc = new CheckoutConductor(project);
		Validator validator = new Validator(project);
		cc.addListener(validator);
		cc.checkout();
		ValidatorBase.output(project.getOutputDir(), project.getProjectId(), validator.getValidationResults());
	}

	private static void validate(Project project, CheckoutConductor.Type type, String commitId)
			throws IOException, ParseException, GitAPIException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, InterruptedException {
		CheckoutConductor cc = new CheckoutConductor(project);
		Validator validator = new Validator(project);
		cc.addListener(validator);
		cc.checkout(type, commitId);
		ValidatorBase.output(project.getOutputDir(), project.getProjectId(), validator.getValidationResults());
	}

	private static void gen(Project project) throws IOException, ParseException, GitAPIException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		List<ValidatorBase> validators = ValidatorBase.getValidators(project, ValidatorBase.VALIDATORS_LIST);
		String curCommitId = null;
		for (ValidatorBase validator : validators) {
			List<ValidationResult> results = ValidatorBase.parse(project);
			for (ValidationResult result : results) {
				if (!validator.getClass().getName().equals(result.getValidatorName())) {
					continue;
				}
				if (new Boolean(false).equals(result.isTruePositive())) {
					LOGGER.info("Skip due to false-positive: {}#{} @ {} by {}", result.getTestCaseClassName(), result.getTestCaseMathodName(),
							result.getCommitId(), result.getValidatorName());
					continue;
				} else if (result.isTruePositive() == null) {
					LOGGER.info("Check whether true-positive or not: {}#{} @ {} by {}", result.getTestCaseClassName(), result.getTestCaseMathodName(),
							result.getCommitId(), result.getValidatorName());
					continue;
				}
				String commitId = result.getCommitId();
				if (!commitId.equals(curCommitId)) {
					new CheckoutConductor(project).checkout(new Commit(commitId, null));
					curCommitId = commitId;
				}
				validator.generate(result);
			}
		}
	}

	private static void repair(Project project)
			throws IOException, ParseException, GitAPIException, MavenInvocationException, DocumentException, PatchFailedException {
		// prepare
		RepairEvaluator evaluator = new RepairEvaluator(project).parse();
		List<EvaluatorBase> evaluators = EvaluatorBase.getEvaluators(project);
		// evaluate
		evaluator.evaluate(evaluators);
	}

	private static void eval(String type, String... args) throws IOException, ParseException, GitAPIException {
		if ("Nt".equals(type)) {
			String projectId = args[0];
			Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
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
		} else if ("detect".equals(type)) {
			// Parse
			String path_to_file = args[0];
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
		} else if ("repair".equals(type)) {
			// Get records
			String path_to_file = args[0];
			File file = new File(path_to_file);
			String content = FileUtils.readFileToString(file);
			CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
			List<CSVRecord> records = parser.getRecords();
			// Traverse
			Map<String, Map<String, Integer>> patternsBySubject = new HashMap<>();
			for (int i = 1; i < records.size(); i++) { // skip header
				CSVRecord record = records.get(i);
				String subject = file.getParent();
				String pattern = record.get(1);
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
			}
			// Print
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
				Map<CSVRecord, Double> improvedRecords = improvedMutationRecords(records);
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
				Map<CSVRecord, Double> improvedRecords = improvedReadabilityRecords(records);
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
				Map<CSVRecord, Pair<Double, Double>> improvedRecords = improvedPerformanceRecords(records);
				Map<CSVRecord, Pair<Double, Double>> partiallyImprovedRecords = partiallyImprovedPerformanceRecords(records);
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
					Map<CSVRecord, Double> improvedRecords = improvedCompileOutputRecords(records);
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
					Map<CSVRecord, Double> improvedRecords = improvedJavadocOutputRecords(records);
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
					Map<CSVRecord, Double> improvedRecords = improvedRuntimeOutputRecords(records);
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
				String commitId = detect_record.get(2);
				String clazz = detect_record.get(3);
				String method = detect_record.get(4);
				String itemId = detect_record.get(13);
				String patternId = patternIdFromItemId(itemId);
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
				} else if (patternId.equals("#8") || patternId.equals("#9") ||patternId.equals("#10") ||patternId.equals("#11") ||patternId.equals("#12") ||patternId.equals("#13")
						||patternId.equals("#14") ||patternId.equals("#15") ||patternId.equals("#16")) {
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
					//String detectHtml = "origin/" + subject + ":" + commitId + ":" + clazz + ":" + method + ".html";
					//Date latest = DictionaryBase.SDF.parse("1970-01-01 00:00:00 -0800");
					//List<String> tags = getCoveredLinesLatestTag(new File(detectHtml));
					//Tag latestCoveredTag = null;
					//for (String tagId : tags) {
					//	Tag tag = dictionary.getTagBy(tagId);
					//	if (tag.getDate().after(latest)) {
					//			latest = tag.getDate();
					//		latestCoveredTag = tag;
					//	}
					//}
					String detectCommitId = detect_record.get(2);
					Commit detectCommit = dictionary.getCommitBy(detectCommitId);
					long detectCommitTime = detectCommit.getDate().getTime();
					String validateCommitId = validate_record.get(0);
					Commit validateCommit = dictionary.getCommitBy(validateCommitId);
					long validateCommitTime = validateCommit.getDate().getTime();
					boolean beforeRelease;
					//if (latestCoveredTag == null) {
					//	beforeRelease = false;
					//} else {
					//	beforeRelease = validateCommit.getDate().before(latestCoveredTag.getDate());
					//}
					long elapsedDayData = (detectCommitTime - validateCommitTime) / (1000 * 60 * 60 * 24);
					//beforeReleaseResults.put(detectHtml, beforeRelease);
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
			};
			FileUtils.write(new File("reality_of_the_challenge.csv"), builder.toString());
		} else if ("improved-version2".equals(type)) {
			// Get records
			String path_to_file = args[0];
			String subject = args[1];
			File file = new File(path_to_file);
			String content = FileUtils.readFileToString(file);
			CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
			List<CSVRecord> records = parser.getRecords();
			// Traverse
			String fileName = file.getName();
			String evaluation = file.getParent();
			StringBuilder builder = new StringBuilder();
			if (evaluation.endsWith("mutation")) {
				Map<CSVRecord, Double> improvedRecords = improvedMutationRecords(records);
				DescriptiveStatistics stats1 = new DescriptiveStatistics();
				DescriptiveStatistics stats2 = new DescriptiveStatistics();
				for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
					CSVRecord record = entry.getKey();
					double num = entry.getValue();
					if (patternIdFromValidatorName(record.get(1)).equals("#1")) {
						stats1.addValue(num);
					} else if (patternIdFromValidatorName(record.get(1)).equals("#2")) {
						stats2.addValue(num);
					}
				}
				builder.append("#1").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats1.getN()).append(",").append(stats1.getSum() / stats1.getN()).append(",").append(stats1.getStandardDeviation()).append("\n");
				builder.append("#2").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats2.getN()).append(",").append(stats2.getSum() / stats2.getN()).append(",").append(stats2.getStandardDeviation()).append("\n");
				FileUtils.write(new File("/Users/yuta/Desktop/output/" + subject, "mutation_improve.csv"), builder.toString());
			} else if (evaluation.endsWith("performance")) {
				Map<CSVRecord, Pair<Double, Double>> improvedRecords = improvedPerformanceRecords(records);
				Map<CSVRecord, Pair<Double, Double>> partiallyImprovedRecords = partiallyImprovedPerformanceRecords(records);
				DescriptiveStatistics stats3 = new DescriptiveStatistics();
				DescriptiveStatistics stats4 = new DescriptiveStatistics();
				for (Map.Entry<CSVRecord, Pair<Double, Double>> entry : improvedRecords.entrySet()) {
					CSVRecord record = entry.getKey();
					Pair<Double, Double> pair = entry.getValue();
					double elapsedTimeImproveRate = pair.getLeft();
					double usedMemoryImproveRate = pair.getRight();
					if (patternIdFromValidatorName(record.get(1)).equals("#3")) {
						stats3.addValue(elapsedTimeImproveRate);
					} else if (patternIdFromValidatorName(record.get(1)).equals("#4")) {
						stats4.addValue(usedMemoryImproveRate);
					}
				}
				builder.append("#3").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats3.getN()).append(",").append(stats3.getSum() / stats3.getN()).append(",").append(stats3.getStandardDeviation()).append("\n");
				builder.append("#4").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats4.getN()).append(",").append(stats4.getSum() / stats4.getN()).append(",").append(stats4.getStandardDeviation()).append("\n");
				FileUtils.write(new File("/Users/yuta/Desktop/output/" + subject, "performance_improve.csv"), builder.toString());
			} else if (evaluation.endsWith("output")) {
				if (fileName.endsWith("compile.csv")) {
					Map<CSVRecord, Double> improvedRecords = improvedCompileOutputRecords(records);
					DescriptiveStatistics stats5 = new DescriptiveStatistics();
					for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
						CSVRecord record = entry.getKey();
						double rate = entry.getValue();
						if (patternIdFromValidatorName(record.get(1)).equals("#5")) {
							stats5.addValue(rate);
						}
					}
					builder.append("#5").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
					builder.append(",").append(stats5.getN()).append(",").append(stats5.getSum() / stats5.getN()).append(",").append(stats5.getStandardDeviation()).append("\n");
					FileUtils.write(new File("/Users/yuta/Desktop/output/" + subject, "compile_improve.csv"), builder.toString());

				} else if (fileName.endsWith("runtime.csv")) {
					Map<CSVRecord, Double> improvedRecords = improvedRuntimeOutputRecords(records);
					DescriptiveStatistics stats6 = new DescriptiveStatistics();
					for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
						CSVRecord record = entry.getKey();
						double rate = entry.getValue();
						if (patternIdFromValidatorName(record.get(1)).equals("#6")) {
							stats6.addValue(rate);
						}
					}
					builder.append("#6").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
					builder.append(",").append(stats6.getN()).append(",").append(stats6.getSum() / stats6.getN()).append(",").append(stats6.getStandardDeviation()).append("\n");
					FileUtils.write(new File("/Users/yuta/Desktop/output/" + subject, "runtime_improve.csv"), builder.toString());
				} else if (fileName.endsWith("javadoc.csv")) {
					Map<CSVRecord, Double> improvedRecords = improvedJavadocOutputRecords(records);
					DescriptiveStatistics stats7 = new DescriptiveStatistics();
					for (Map.Entry<CSVRecord, Double> entry : improvedRecords.entrySet()) {
						CSVRecord record = entry.getKey();
						double rate = entry.getValue();
						if (patternIdFromValidatorName(record.get(1)).equals("#7")) {
							stats7.addValue(rate);
						}
					}
					builder.append("#7").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
					builder.append(",").append(stats7.getN()).append(",").append(stats7.getSum() / stats7.getN()).append(",").append(stats7.getStandardDeviation()).append("\n");
					FileUtils.write(new File("/Users/yuta/Desktop/output/" + subject, "javadoc_improve.csv"), builder.toString());
				}
			} else if (evaluation.endsWith("readability")) {
				Map<CSVRecord, Double> improvedRecords = improvedReadabilityRecords(records);
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
					if (patternIdFromValidatorName(record.get(1)).equals("#8")) {
						stats8.addValue(rate);
					} else if (patternIdFromValidatorName(record.get(1)).equals("#9")) {
						stats9.addValue(rate);
					} else if (patternIdFromValidatorName(record.get(1)).equals("#10")) {
						stats10.addValue(rate);
					} else if (patternIdFromValidatorName(record.get(1)).equals("#11")) {
						stats11.addValue(rate);
					} else if (patternIdFromValidatorName(record.get(1)).equals("#12")) {
						stats12.addValue(rate);
					} else if (patternIdFromValidatorName(record.get(1)).equals("#13")) {
						stats13.addValue(rate);
					} else if (patternIdFromValidatorName(record.get(1)).equals("#14")) {
						stats14.addValue(rate);
					} else if (patternIdFromValidatorName(record.get(1)).equals("#15")) {
						stats15.addValue(rate);
					} else if (patternIdFromValidatorName(record.get(1)).equals("#16")) {
						stats16.addValue(rate);
					}
				}
				builder.append("#8").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats8.getN()).append(",").append(stats8.getSum() / stats8.getN()).append(",").append(stats8.getStandardDeviation()).append("\n");
				builder.append("#9").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats9.getN()).append(",").append(stats9.getSum() / stats9.getN()).append(",").append(stats9.getStandardDeviation()).append("\n");
				builder.append("#10").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats10.getN()).append(",").append(stats10.getSum() / stats10.getN()).append(",").append(stats10.getStandardDeviation()).append("\n");
				builder.append("#11").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats11.getN()).append(",").append(stats11.getSum() / stats11.getN()).append(",").append(stats11.getStandardDeviation()).append("\n");
				builder.append("#12").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats12.getN()).append(",").append(stats12.getSum() / stats12.getN()).append(",").append(stats12.getStandardDeviation()).append("\n");
				builder.append("#13").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats13.getN()).append(",").append(stats13.getSum() / stats13.getN()).append(",").append(stats13.getStandardDeviation()).append("\n");
				builder.append("#14").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats14.getN()).append(",").append(stats14.getSum() / stats14.getN()).append(",").append(stats14.getStandardDeviation()).append("\n");
				builder.append("#15").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats15.getN()).append(",").append(stats15.getSum() / stats15.getN()).append(",").append(stats15.getStandardDeviation()).append("\n");
				builder.append("#16").append(",").append("N").append(",").append("Average").append(",").append("StandardDeviation").append("\n");
				builder.append(",").append(stats16.getN()).append(",").append(stats16.getSum() / stats16.getN()).append(",").append(stats16.getStandardDeviation()).append("\n");
				FileUtils.write(new File("/Users/yuta/Desktop/output/" + subject, "readability_improve.csv"), builder.toString());
			}
		}
	}

	private static Map<CSVRecord, Double> improvedMutationRecords(List<CSVRecord> records) {
		Map<CSVRecord, Double> ret = new HashMap<>();
		for (CSVRecord record : records) {
			if (!record.get(4).equals("Improved")) {
				continue;
			}
			double beforeScore = Double.parseDouble(record.get(5));
			double afterScore = Double.parseDouble(record.get(6));
			double improveRate;
			//improveRate = (beforeScore != 0) ? (afterScore - beforeScore) / beforeScore * 100 : 100;
			improveRate = afterScore - beforeScore;
			ret.put(record, improveRate);
		}
		return ret;
	}

	private static Map<CSVRecord, Double> improvedReadabilityRecords(List<CSVRecord> records) {
		Map<CSVRecord, Double> ret = new HashMap<>();
		for (CSVRecord record : records) {
			if (!record.get(4).equals("Improved")) {
				continue;
			}
			double beforeScore = Double.parseDouble(record.get(5));
			double afterScore = Double.parseDouble(record.get(7));
			double improveRate;
			improveRate = (beforeScore != 0) ? (afterScore - beforeScore) / beforeScore * 100 : 100;
			ret.put(record, improveRate);
		}
		return ret;
	}

	private static Map<CSVRecord, Pair<Double, Double>> improvedPerformanceRecords(List<CSVRecord> records) {
		Map<CSVRecord, Pair<Double, Double>> ret = new HashMap<>();
		for (CSVRecord record : records) {
			if (!(record.get(4).equals("Improved") || record.get(4).equals("PartiallyImproved"))) {
				continue;
			}
			double beforeElapsedTime = Double.parseDouble(record.get(5));
			double afterElapsedTime = Double.parseDouble(record.get(7));
			double beforeUsedMemory = Double.parseDouble(record.get(6));
			double afterUsedMemory = Double.parseDouble(record.get(8));
			double elapsedTimeImproveRate;
			elapsedTimeImproveRate = (beforeElapsedTime != 0) ? (beforeElapsedTime - afterElapsedTime) / beforeElapsedTime * 100 : 100;
			double usedMemoryImproveRate;
			usedMemoryImproveRate = (beforeUsedMemory != 0) ? (beforeUsedMemory - afterUsedMemory) / beforeUsedMemory * 100 : 100;
			ret.put(record, new ImmutablePair<>(elapsedTimeImproveRate, usedMemoryImproveRate));
		}
		return ret;
	}

	private static Map<CSVRecord, Pair<Double, Double>> partiallyImprovedPerformanceRecords(List<CSVRecord> records) {
		Map<CSVRecord, Pair<Double, Double>> ret = new HashMap<>();
		for (CSVRecord record : records) {
			if (!record.get(4).equals("PartiallyImproved")) {
				continue;
			}
			double beforeElapsedTime = Double.parseDouble(record.get(5));
			double afterElapsedTime = Double.parseDouble(record.get(7));
			double beforeUsedMemory = Double.parseDouble(record.get(6));
			double afterUsedMemory = Double.parseDouble(record.get(8));
			double elapsedTimeImproveRate;
			elapsedTimeImproveRate = (beforeElapsedTime != 0) ? (beforeElapsedTime - afterElapsedTime) / beforeElapsedTime * 100 : 100;
			double usedMemoryImproveRate;
			usedMemoryImproveRate = (beforeUsedMemory != 0) ? (beforeUsedMemory - afterUsedMemory) / beforeUsedMemory * 100 : 100;
			ret.put(record, new ImmutablePair<>(elapsedTimeImproveRate, usedMemoryImproveRate));
		}
		return ret;
	}

	private static Map<CSVRecord, Double> improvedCompileOutputRecords(List<CSVRecord> records) {
		Map<CSVRecord, Double> ret = new HashMap<>();
		for (CSVRecord record : records) {
			if (!record.get(4).equals("Improved")) {
				continue;
			}
			double beforeScore = Double.parseDouble(record.get(5));
			double afterScore = Double.parseDouble(record.get(7));
			double improveRate;
			improveRate = (beforeScore != 0) ? (beforeScore - afterScore) / beforeScore * 100 : 100;
			ret.put(record, improveRate);
		}
		return ret;
	}

	private static Map<CSVRecord, Double> improvedJavadocOutputRecords(List<CSVRecord> records) {
		Map<CSVRecord, Double> ret = new HashMap<>();
		for (CSVRecord record : records) {
			if (!record.get(4).equals("Improved")) {
				continue;
			}
			double beforeScore = Double.parseDouble(record.get(5));
			double afterScore = Double.parseDouble(record.get(6));
			double improveRate;
			improveRate = (beforeScore != 0) ? (beforeScore - afterScore) / beforeScore * 100 : 100;
			ret.put(record, improveRate);
		}
		return ret;
	}

	private static Map<CSVRecord, Double> improvedRuntimeOutputRecords(List<CSVRecord> records) {
		Map<CSVRecord, Double> ret = new HashMap<>();
		for (CSVRecord record : records) {
			if (!record.get(4).equals("Improved")) {
				continue;
			}
			double beforeScore = Double.parseDouble(record.get(5));
			double afterScore = Double.parseDouble(record.get(6));
			double improveRate;
			improveRate = (beforeScore != 0) ? (beforeScore - afterScore) / beforeScore * 100 : 100;
			ret.put(record, improveRate);
		}
		return ret;
	}

	private static String patternFromId(String patternId) {
		if (patternId.equals("#1")) {
			return "AddTestAnnotations";
		} else if (patternId.equals("#2")) {
			return "UseAssertArrayEqualsProperly";
		} else if (patternId.equals("#3")) {
			return "UseAssertEqualsProperly";
		} else if (patternId.equals("#4")) {
			return "UseAssertFalseProperly";
		} else if (patternId.equals("#5")) {
			return "UseAssertNotSameProperly";
		} else if (patternId.equals("#6")) {
			return "UseAssertNullProperly";
		} else if (patternId.equals("#7")) {
			return "UseAssertTrueProperly";
		} else if (patternId.equals("#8")) {
			return "UseFailInsteadOfAssertTrueFalse";
		} else if (patternId.equals("#9")) {
			return "UseStringContains";
		} else if (patternId.equals("#10")) {
			return "SwapActualExpectedValues";
		} else if (patternId.equals("#11")) {
			return "AssertNotNullToInstances";
		} else if (patternId.equals("#12")) {
			return "ModifyAssertImports";
		} else if (patternId.equals("#13")) {
			return "DoNotSwallowTestErrorsSiliently";
		} else if (patternId.equals("#14")) {
			return "AddFailStatementsForHandlingExpectedExceptions";
		} else if (patternId.equals("#15")) {
			return "HandleExpectedExecptionsProperly";
		} else if (patternId.equals("#16")) {
			return "RemoveUnusedExceptions";
		} else if (patternId.equals("#17")) {
			return "CloseResources";
		} else if (patternId.equals("#18")) {
			return "UseTryWithResources";
		} else if (patternId.equals("#19")) {
			return "UseProcessWaitfor";
		} else if (patternId.equals("#20")) {
			return "AddSuppressWarnings";
		} else if (patternId.equals("#21")) {
			return "DeleteUnnecessaryAssignmenedVariables";
		} else if (patternId.equals("#22")) {
			return "IntroduceAutoBoxing";
		} else if (patternId.equals("#23")) {
			return "RemoveUnnecessaryCasts";
		} else if (patternId.equals("#24")) {
			return "RemovePrintStatements";
		} else if (patternId.equals("#25")) {
			return "FixJavadocErrors";
		} else if (patternId.equals("#26")) {
			return "ReplaceAtTodoWithTODO";
		} else if (patternId.equals("#27")) {
			return "UseCodeAnnotationsAtJavaDoc";
		} else if (patternId.equals("#28")) {
			return "FormatCode";
		} else if (patternId.equals("#29")) {
			return "ConvertForLoopsToEnhanced";
		} else if (patternId.equals("#30")) {
			return "UseFinalModifierWherePossible";
		} else if (patternId.equals("#31")) {
			return "AddExplicitBlocks";
		} else if (patternId.equals("#32")) {
			return "UseThisIfNecessary";
		} else if (patternId.equals("#33")) {
			return "AddSerialVersionUIDs";
		} else if (patternId.equals("#34")) {
			return "AddOverrideAnnotation";
		} else if (patternId.equals("#35")) {
			return "UseDiamondOperators";
		} else if (patternId.equals("#36")) {
			return "UseArithmeticAssignmentOperators";
		} else if (patternId.equals("#37")) {
			return "AccessFilesProperly";
		} else if (patternId.equals("#38")) {
			return "AddCastToNull";
		} else if (patternId.equals("#39")) {
			return "AccessStaticMethodsAtDefinedClasses";
		} else if (patternId.equals("#40")) {
			return "AccessStaticFieldsAtDefinedClasses";
		} else if (patternId.equals("#FP1")) {
			return "FalsePositive";
		} else {
			return "Limitation";
		}
	}

	private static String patternIdFromValidatorName(String validatorName) {
		if (validatorName.endsWith("DoNotSwallowTestErrorsSilently") || validatorName.endsWith("AddFailStatementsForHandlingExpectedExceptions") ||
				validatorName.endsWith("UseFailInsteadOfAssertTrueFalse")) {
			return "#1";
		} else if (validatorName.endsWith("AssertNotNullToInstances")) {
			return "UseAssertArrayEqualsProperly";
		} else if (validatorName.endsWith("UseProcessWaitfor")) {
			return "#3";
		} else if (validatorName.endsWith("CloseResources") || validatorName.endsWith("UseTryWithResources")) {
			return "#4";
		} else if (validatorName.endsWith("DeleteUnnecessaryAssignmenedVariables") || validatorName.endsWith("RemoveUnnecessaryCasts") ||
				validatorName.endsWith("IntroduceAutoBoxing") || validatorName.endsWith("AddOverrideAnnotationToMethodsInConstructors") ||
				validatorName.endsWith("AddOverrideAnnotationToTestCase") || validatorName.endsWith("AddSerialVersionUids") || validatorName.endsWith("AddSuppressWarningsDeprecationAnnotation") ||
				validatorName.endsWith("AddSuppressWarningsRawtypesAnnotation") || validatorName.endsWith("AddSuppressWarningsUncheckedAnnotation")) {
			return "#5";
		} else if (validatorName.endsWith("RemovePrintStatements")) {
			return "#6";
		} else if (validatorName.endsWith("FixJavadocErrors") || validatorName.endsWith("ReplaceAtTodoWithTODO") ||
				validatorName.endsWith("UseCodeAnnotationsAtJavaDoc")) {
			return "#7";
		} else if (validatorName.endsWith("AddTestAnnotations")) {
			return "#8";
		} else if (validatorName.endsWith("ModifyAssertImports")) {
			return "#9";
		} else if (validatorName.endsWith("UseAssertArrayEqualsProperly") || validatorName.endsWith("UseAssertEqualsProperly") ||
				validatorName.endsWith("UseAssertFalseProperly") || validatorName.endsWith("UseAssertNotSameProperly") ||
				validatorName.endsWith("UseAssertNullProperly") || validatorName.endsWith("UseAssertTrueProperly")) {
			return "#10";
		} else if (validatorName.endsWith("SwapActualExpectedValues")) {
			return "#11";
		} else if (validatorName.endsWith("HandleExpectedExecptionsProperly")) {
			return "#12";
		} else if (validatorName.endsWith("FormatCode") || validatorName.endsWith("ConvertForLoopsToEnhanced") ||
				validatorName.endsWith("UseModifierFinalWherePossible") || validatorName.endsWith("AddExplicitBlocks") ||
				validatorName.endsWith("UseThisIfNecessary") || validatorName.endsWith("UseDiamondOperators") ||
				validatorName.endsWith("UseArithmeticAssignmentOperators") || validatorName.endsWith("RemoveUnusedExceptions")) {
			return "#13";
		} else if (validatorName.endsWith("AccessStaticFieldsAtDefinedSuperClass") || validatorName.endsWith("AccessStaticMethodsAtDefinedSuperClass")) {
			return "#14";
		} else if (validatorName.endsWith("AccessFilesProperly")) {
			return "#15";
		} else if (validatorName.endsWith("AddCastToNull")) {
			return "#16";
		} else {
			return "#20";
		}
	}

	private static List<String> getCoveredLinesLatestTag(File file) throws IOException {
		String content = FileUtils.readFileToString(file);
		List<String> ret = new ArrayList<>();
		Document document = Jsoup.parse(content, "", Parser.xmlParser());
		Elements tbodys = document.select("tbody");
		Elements trs = new Elements();
		for (int i = 1; i < tbodys.size(); i++) {
			trs.addAll(tbodys.get(i).select("tr.target"));
		}
		Elements tds = new Elements();
		for (Element tr : trs) {
			tds.add(tr.select("td").first());
		}
		Elements hrefs = tds.select("a[href]");
		for (Element href : hrefs) {
			ret.add((href.text()));
		}
		return ret;
	}

	private static String patternIdFromItemId(String itemId) {
		if (itemId.equals("#1")) {
			return "#8";
		} else if (itemId.equals("#2")) {
			return "#10";
		} else if (itemId.equals("#3")) {
			return "#10";
		} else if (itemId.equals("#4")) {
			return "#10";
		} else if (itemId.equals("#5")) {
			return "#10";
		} else if (itemId.equals("#6")) {
			return "#10";
		} else if (itemId.equals("#7")) {
			return "#10";
		} else if (itemId.equals("#8")) {
			return "#10";
		} else if (itemId.equals("#9")) {
			return "#10";
		} else if (itemId.equals("#10")) {
			return "#11";
		} else if (itemId.equals("#11")) {
			return "#2";
		} else if (itemId.equals("#12")) {
			return "#9";
		} else if (itemId.equals("#13")) {
			return "#1";
		} else if (itemId.equals("#14")) {
			return "#1";
		} else if (itemId.equals("#15")) {
			return "#1";
		} else if (itemId.equals("#16")) {
			return "#12";
		} else if (itemId.equals("#17")) {
			return "#4";
		} else if (itemId.equals("#18")) {
			return "#4";
		} else if (itemId.equals("#19")) {
			return "#3";
		} else if (itemId.equals("#20")) {
			return "#5";
		} else if (itemId.equals("#21")) {
			return "#5";
		} else if (itemId.equals("#22")) {
			return "#5";
		} else if (itemId.equals("#23")) {
			return "#5";
		} else if (itemId.equals("#24")) {
			return "#6";
		} else if (itemId.equals("#25")) {
			return "#7";
		} else if (itemId.equals("#26")) {
			return "#7";
		} else if (itemId.equals("#27")) {
			return "#7";
		} else if (itemId.equals("#28")) {
			return "#13";
		} else if (itemId.equals("#29")) {
			return "#13";
		} else if (itemId.equals("#30")) {
			return "#13";
		} else if (itemId.equals("#31")) {
			return "#13";
		} else if (itemId.equals("#32")) {
			return "#13";
		} else if (itemId.equals("#33")) {
			return "#13";
		} else if (itemId.equals("#34")) {
			return "#13";
		} else if (itemId.equals("#35")) {
			return "#13";
		} else if (itemId.equals("#36")) {
			return "#13";
		} else if (itemId.equals("#37")) {
			return "#15";
		} else if (itemId.equals("#38")) {
			return "#16";
		} else if (itemId.equals("#39")) {
			return "#14";
		} else if (itemId.equals("#40")) {
			return "#14";
		} else {
			return "";
		}
	}
}

package jp.mzw.vtr;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
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

import difflib.PatchFailedException;
import jp.mzw.vtr.cluster.HCluster;
import jp.mzw.vtr.cluster.similarity.DistAnalyzer;
import jp.mzw.vtr.cluster.similarity.DistMap;
import jp.mzw.vtr.cluster.visualize.VisualizerBase;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.detect.TestCaseModification;
import jp.mzw.vtr.dict.DictionaryMaker;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.CheckoutListener;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.GitUtils;
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
			// Parse
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
		}
	}
}

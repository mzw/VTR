package jp.mzw.vtr;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
import jp.mzw.vtr.git.GitUtils;
import jp.mzw.vtr.maven.TestRunner;
import jp.mzw.vtr.repair.Repair;
import jp.mzw.vtr.repair.RepairEvaluator;
import jp.mzw.vtr.validate.ValidationResult;
import jp.mzw.vtr.validate.ValidatorBase;

public class CLI {
	static Logger LOGGER = LoggerFactory.getLogger(CLI.class);

	public static final String CONFIG_FILENAME = "config.properties";

	public static void main(String[] args) throws IOException, NoHeadException, GitAPIException, ParseException, MavenInvocationException, DocumentException,
			PatchFailedException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException, ClassNotFoundException {

		if (args.length < 1) { // Invalid usage
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI dict      <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov       <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov       <subject-id> At    <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov       <subject-id> After <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI detect    <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cluster   <similarity> <cluster-method> <threshold>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI visualize <method>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI validate  <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI gen       <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI repair    <subject-id> <source-classes>");
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
			detect(project);
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
			validate(project);
		} else if ("gen".equals(command)) {
			String projectId = args[1];
			Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
			gen(project);
		} else if ("repair".equals(command)) {
			String projectId = args[1];
			String sut = args[2];
			Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
			repair(project, sut);
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

	private static void cluster(Project project, String analyzer, String strategy, double threshold) throws IOException, ParseException, NoHeadException,
			GitAPIException {
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
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		CheckoutConductor cc = new CheckoutConductor(project);
		List<ValidatorBase> validators = ValidatorBase.getValidators(project, ValidatorBase.VALIDATORS_LIST);
		for (ValidatorBase validator : validators) {
			cc.addListener(validator);
		}
		cc.checkout();
		ValidatorBase.output(project, validators);
	}

	private static void gen(Project project) throws IOException, ParseException, GitAPIException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		List<ValidatorBase> validators = ValidatorBase.getValidators(project, ValidatorBase.VALIDATORS_LIST);
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
				validator.generate(result);
			}
		}
	}

	private static void repair(Project project, String sut) throws IOException, ParseException, GitAPIException, MavenInvocationException, DocumentException,
			PatchFailedException {
		RepairEvaluator repairEvaluator = new RepairEvaluator(project, sut).parse();
		CheckoutConductor cc = new CheckoutConductor(project);
		for (Repair repair : repairEvaluator.getRepairs()) {
			repairEvaluator.repair(cc, repair);
		}
	}

}

package jp.mzw.vtr;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.cluster.HCluster;
import jp.mzw.vtr.cluster.similarity.DistAnalyzer;
import jp.mzw.vtr.cluster.similarity.DistMap;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.detect.TestCaseModification;
import jp.mzw.vtr.dict.DictionaryMaker;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.GitUtils;
import jp.mzw.vtr.maven.TestRunner;

public class CLI {
	static Logger LOGGER = LoggerFactory.getLogger(CLI.class);
	
	public static final String CONFIG_FILENAME = "config.properties";

	public static void main(String[] args) throws IOException, NoHeadException, GitAPIException, ParseException {

		if (args.length < 1) { // Invalid usage
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI dict    <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov     <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov     <subject-id> At    <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cov     <subject-id> After <commit-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI detect  <subject-id>");
			LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cluster <similarity> <cluster-method> <threshold>");
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
			if (args.length == 1) { // all commits
				cov(project);
			} else if (args.length == 3) { // specific commit(s)
				CheckoutConductor.Type type = CheckoutConductor.Type.valueOf(args[1]);
				String commitId = args[2];
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
				String storategy = args[2];
				double threshold = Double.parseDouble(args[3]);
				cluster(new Project(null), analyzer, storategy, threshold);
			} else {
				LOGGER.info("$ java -cp=<class-path> jp.mzw.vtr.CLI cluster <similarity> <cluster-method> <threshold>");
			}
		}

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
		cc.addListener(new Detector(project));
		cc.checkout();
	}
	
	private static void cluster(Project project, String analyzer, String strategy, double threshold) throws IOException, ParseException {
		// Similarity
		DistAnalyzer distAnalyzer = DistAnalyzer.analyzerFactory(project.getOutputDir(), analyzer);
		List<TestCaseModification> tcmList = distAnalyzer.parseTestCaseModifications();
		DistMap map = distAnalyzer.analyze(tcmList);
		distAnalyzer.output(map);
		// Clustering
		HCluster cluster = new HCluster(project.getOutputDir(), distAnalyzer.getMethodName()).parse();
		cluster.cluster(HCluster.getStrategy(strategy), threshold);
		cluster.output();
	}
}

package jp.mzw.vtr;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.cluster.LCSAnalyzer;
import jp.mzw.vtr.cluster.LCSMap;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.dict.DictionaryMaker;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.GitUtils;
import jp.mzw.vtr.maven.TestRunner;

public class CLI {
	static Logger LOGGER = LoggerFactory.getLogger(CLI.class);

	public static void main(String[] args) throws IOException, NoHeadException, GitAPIException, ParseException {

		if (args.length < 1) {
			// Invalid usage
			LOGGER.info("Ex) $ java -cp=CLASSPATH jp.mzw.vtr.CLI dict vtr-example subjects/vtr-example refs/heads/master");
			LOGGER.info("Ex) $ java -cp=CLASSPATH jp.mzw.vtr.CLI cov vtr-example subjects/vtr-example");
			LOGGER.info("Ex) $ java -cp=CLASSPATH jp.mzw.vtr.CLI detect vtr-example subjects/vtr-example");
			LOGGER.info("Ex) $ java -cp=CLASSPATH jp.mzw.vtr.CLI lcs");
			return;
		}

		String command = args[0];
		if ("lcs".equals(command)) {
			Project project = new Project(null, null);
			lcs(project);
			return;
		}
		
		String projectId = args[1];
		String pathToProject = args[2];
		Project project = new Project(projectId, pathToProject);
		project.setConfig("config.properties");

		if ("dict".equals(command)) {
			String refToCompare = args[3];
			project.setRefToCompare(refToCompare);
			dict(project);
		} else if ("cov".equals(command)) {
			// All commits
			if (args.length == 3) {
				cov(project);
			}
			// Specific commit(s)
			else if (args.length == 5) {
				CheckoutConductor.Type type = CheckoutConductor.Type.valueOf(args[3]);
				String commitId = args[4];
				cov(project, type, commitId);
			}
		} else if ("detect".equals(command)) {
			detect(project);
		}

	}

	private static void dict(Project project) throws IOException, NoHeadException, GitAPIException {
		Git git = GitUtils.getGit(project.getPathToProject());
		DictionaryMaker dm = new DictionaryMaker(git);
		Map<Ref, Collection<RevCommit>> tagCommitsMap = dm.getTagCommitsMap(project.getRefToCompare());

		File dir = new File(project.getOutputDir(), project.getProjectId());
		dm.writeCommitListInXML(dir);
		dm.writeDictInXML(tagCommitsMap, project.getRefToCompare(), dir);
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
	
	private static void lcs(Project project) throws IOException, ParseException {
		LCSAnalyzer analyzer = new LCSAnalyzer(project.getOutputDir());
		LCSMap map = analyzer.analyze();
		analyzer.output(map);
	}
}

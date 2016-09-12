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

import jp.mzw.vtr.core.Config;
import jp.mzw.vtr.cov.TestRunner;
import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.dict.DictionaryMaker;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.GitUtils;

public class CLI {
	static Logger LOGGER = LoggerFactory.getLogger(CLI.class);
	
	public static void main(String[] args) throws IOException, NoHeadException, GitAPIException, ParseException {
		
		Config config = new Config("config.properties");
		
		if (args.length == 0) {
			// Invalid usage
			LOGGER.info("Ex) $ java -cp=CLASSPATH jp.mzw.vtr.CLI dict vtr-example subjects/vtr-example refs/heads/master");
			LOGGER.info("Ex) $ java -cp=CLASSPATH jp.mzw.vtr.CLI cov vtr-example subjects/vtr-example");
			LOGGER.info("Ex) $ java -cp=CLASSPATH jp.mzw.vtr.CLI detect vtr-example subjects/vtr-example");
		} else if ("dict".equals(args[0])) {
			String subjectId = args[1];
			String pathToSubject = args[2];
			String refToCompare = args[3];
			dict(config.getOutputDir(), subjectId, pathToSubject, refToCompare);
		} else if ("cov".equals(args[0])) {
			String subjectId = args[1];
			String pathToSubject = args[2];
			if (args[3] == null) { // All commits
				cov(subjectId, pathToSubject, config);
			} else if (args[3] != null && args[4] != null) {
				CheckoutConductor.Type type = CheckoutConductor.Type.valueOf(args[3]);
				String commitId = args[4];
				cov(subjectId, pathToSubject, type, commitId, config);
			}
		} else if ("detect".equals(args[0])) {
			String subjectId = args[1];
			String pathToSubject = args[2];
			detect(subjectId, pathToSubject, config);
		}
	}
	
	private static void dict(File outputDir, String subjectId, String pathToSubject, String refToCompare) throws IOException, NoHeadException, GitAPIException {
		Git git = GitUtils.getGit(pathToSubject);
		DictionaryMaker dm = new DictionaryMaker(git);
		Map<Ref, Collection<RevCommit>> tagCommitsMap = dm.getTagCommitsMap(refToCompare);
		
		File dir = new File(outputDir, subjectId);
		dm.writeCommitListInXML(dir);
		dm.writeDictInXML(tagCommitsMap, refToCompare, dir);
	}

	private static void cov(String subjectId, String pathToSubject, Config config) throws IOException, ParseException, GitAPIException {
		Git git = GitUtils.getGit(pathToSubject);
		CheckoutConductor cc = new CheckoutConductor(git, new File(config.getOutputDir(), subjectId));
		cc.addListener(new TestRunner(subjectId, new File(pathToSubject), config));
		cc.checkout();
	}

	private static void cov(String subjectId, String pathToSubject, CheckoutConductor.Type type, String commitId, Config config) throws IOException, ParseException, GitAPIException {
		Git git = GitUtils.getGit(pathToSubject);
		CheckoutConductor cc = new CheckoutConductor(git, new File(config.getOutputDir(), subjectId));
		cc.addListener(new TestRunner(subjectId, new File(pathToSubject), config));
		cc.checkout(type, commitId);
	}
	
	private static void detect(String subjectId, String pathToSubject, Config config) throws IOException, ParseException, GitAPIException {
		Git git = GitUtils.getGit(pathToSubject);
		CheckoutConductor cc = new CheckoutConductor(git, new File(config.getOutputDir(), subjectId));
		cc.addListener(new Detector(subjectId, pathToSubject, config));
		cc.checkout();
	}
}

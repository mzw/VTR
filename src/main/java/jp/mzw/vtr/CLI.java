package jp.mzw.vtr;

import java.io.File;
import java.io.IOException;
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
import jp.mzw.vtr.dict.DictionaryMaker;
import jp.mzw.vtr.git.GitUtils;

public class CLI {
	static Logger LOGGER = LoggerFactory.getLogger(CLI.class);
	
	public static void main(String[] args) throws IOException, NoHeadException, GitAPIException {
		
		Config config = new Config("config.properties");
		
		if (args.length == 0) {
			// Invalid usage
			LOGGER.info("Ex) $ java jp.mzw.vtr.CLI dict vtr-example subjects/vtr-example refs/heads/master");
		} else if ("dict".equals(args[0])) {
			String subjectId = args[1];
			String pathToSubject = args[2];
			String refToCompare = args[3];
			dict(config.getOutputDir(), subjectId, pathToSubject, refToCompare);
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

}

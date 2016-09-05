package jp.mzw.vtr.git;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.Project;
import jp.mzw.vtr.Utils;

public class GitUtils {
	static Logger log = LoggerFactory.getLogger(GitUtils.class);
	
	public static final String DOT_GIT = ".git";
	

	public static void checkout(Project project, Properties config, String commitId) throws IOException, InterruptedException {
		Utils.exec(project.getBaseDir(), Arrays.asList(Utils.getPathToGit(config), "checkout", commitId));
		log.info("Git-checkout: " + commitId);
	}
	
	public static void stash(Project project, Properties config) throws IOException, InterruptedException {
		Utils.exec(project.getBaseDir(), Arrays.asList(Utils.getPathToGit(config), "stash"));
		log.info("Git stash");
	}

	public static void stash_drop(Project project, Properties config) throws IOException, InterruptedException {
		Utils.exec(project.getBaseDir(), Arrays.asList(Utils.getPathToGit(config), "stash", "drop"));
		log.info("Git-stash-drop");
	}
	
	public static void stash_clear(Project project, Properties config) throws IOException, InterruptedException {
		Utils.exec(project.getBaseDir(), Arrays.asList(Utils.getPathToGit(config), "stash", "clear"));
		log.info("Git-stach-clear");
	}

	public static List<Commit> blame(int start, int end, File file, Project project, Properties config, List<Commit> commits) throws IOException, InterruptedException {
		String commitId = getCommitidByBlame(start, end, file, project, config);
		ArrayList<Commit> ret = new ArrayList<>();
		for(Commit commit : commits) {
			if(commitId.equals(commit.getId()) && !ret.contains(commit)) {
				ret.add(commit);
			}
		}
		return ret;
	}

	public static String getCommitidByBlame(int start, int end, File file, Project project, Properties config) throws IOException, InterruptedException {
		List<String> results = Utils.exec(project.getBaseDir(), Arrays.asList(Utils.getPathToGit(config), "blame", "--line-porcelain", "-L", start+","+end, file.getAbsolutePath()));
		if(results == null) return null;
		if(results.get(0) == null) return null;
		if(results.get(0).length() < 40) return null;

		String commitId = results.get(0).substring(0, 40);
		
		return commitId;
	}

	public static Commit blame(int lineno, File file, Project project, Properties config, List<Commit> commits) throws IOException, InterruptedException {
		String commitId = getCommitidByBlame(lineno, lineno, file, project, config);
		ArrayList<Commit> ret = new ArrayList<>();

		for(Commit commit : commits) {
			if(commitId.equals(commit.getId()) && !ret.contains(commit)) {
				ret.add(commit);
			}
		}
		
		if(ret.size() == 0) {
			return null;
		}
		if(1 < ret.size()) {
			log.warn("Found more than one commits: " + ret.size() + " for " + Arrays.asList("blame", "--line-porcelain", "-L", lineno+","+lineno, file.getAbsolutePath()));
			for(Commit commit : ret) {
				log.warn(commit.getId());
			}
		}
		return ret.get(0);
	}

	public static List<String> blame(int start, int end, File file, Project project, Properties config, boolean stdout) throws IOException, InterruptedException {
		return Utils.exec(project.getBaseDir(), Arrays.asList(Utils.getPathToGit(config), "blame", "-ls", "-L", start+","+end, file.getAbsolutePath()));
	}
}

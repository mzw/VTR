package jp.mzw.vtr.cov;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;

import jp.mzw.vtr.dict.DictionaryParser;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.Tag;

public class CheckoutConductor {

	Git git;
	List<Commit> commits;
	Map<Tag, List<Commit>> dict;
	
	public CheckoutConductor(Git git, File dir) throws IOException, ParseException {
		this.git = git;
		commits = DictionaryParser.parseCommits(dir);
		dict = DictionaryParser.parseDictionary(dir);
	}
	
	/**
	 * 
	 * @return
	 */
	public List<Commit> getCommitsAfterInitialRelease() {
		List<Commit> ret = new ArrayList<>();
		for (Commit commit : this.commits) {
			// Skip until initial release
			boolean init = true;
			for (Tag tag : dict.keySet()) {
				if (commit.getDate().after(tag.getDate())) {
					init = false;
					break;
				}
			}
			if (init) {
				continue;
			}
			ret.add(commit);
		}
		return ret;
	}
	
	
	/**
	 * Get commits after given commit ID
	 * @param commitId
	 */
	public List<Commit> getCommitsAfter(String commitId) {
		List<Commit> ret = new ArrayList<>();
		boolean detect = false;
		for (Commit commit : this.commits) {
			if (commit.getId().equals(commitId)) {
				detect = true;
			}
			if (!detect) {
				continue;
			}
			ret.add(commit);
		}
		return ret;
	}
	
	
	public List<Commit> getCommitAt(String commitId) {
		List<Commit> ret = new ArrayList<>();
		for (Commit commit : this.commits) {
			if (commit.getId().equals(commitId)) {
				ret.add(commit);
				return ret;
			}
		}
		return ret;
	}
	
}

package jp.mzw.vtr.git;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import jp.mzw.vtr.dict.DictionaryParser;

public class CheckoutConductor {

	/**
	 * Listener interface for Observer pattern
	 * 
	 * @author Yuta Maezawa
	 *
	 */
	public interface Listener {
		public void onCheckout(Commit commit);
	}

	/** Observer listeners */
	private Set<Listener> listenerSet = new CopyOnWriteArraySet<>();

	/**
	 * Add Observer listeners
	 * 
	 * @param listener
	 */
	public void addListener(Listener listener) {
		this.listenerSet.add(listener);
	}

	/** Notify Observer listeners */
	private void notifyListeners(Commit commit) {
		for (Listener listener : this.listenerSet) {
			listener.onCheckout(commit);
		}
	}

	Git git;
	List<Commit> commits;
	Map<Tag, List<Commit>> dict;

	public CheckoutConductor(Git git, File dir) throws IOException, ParseException {
		this.git = git;
		commits = DictionaryParser.parseCommits(dir);
		dict = DictionaryParser.parseDictionary(dir);
	}

	/**
	 * API for conducting checkout
	 * 
	 * @throws GitAPIException
	 */
	public void checkout() throws GitAPIException {
		List<Commit> commits = getCommitsAfterInitialRelease();
		checkout(commits);
	}

	/**
	 * Types representing checkout after or at given commit ID
	 * 
	 * @author yuta
	 *
	 */
	public static enum Type {
		After, At,
	}

	/**
	 * API for conducting checkout
	 * 
	 * @param type
	 * @param commitId
	 * @throws GitAPIException
	 */
	public void checkout(Type type, String commitId) throws GitAPIException {
		List<Commit> commits = new ArrayList<>();
		switch (type) {
		case After:
			commits = getCommitsAfter(commitId);
			break;
		case At:
			commits.addAll(getCommitAt(commitId));
			break;
		default:
			return;
		}
		checkout(commits);
	}

	/**
	 * Actual checkout conductor
	 * 
	 * @param commits
	 * @throws GitAPIException
	 */
	private void checkout(List<Commit> commits) throws GitAPIException {
		for (Commit commit : commits) {
			// Stash before checkout
			git.stashCreate().call();
			Collection<RevCommit> stashes = git.stashList().call();
			if (!stashes.isEmpty()) {
				git.stashDrop().setStashRef(0).call();
			}
			// Checkout
			git.checkout().setName(commit.getId()).call();
			notifyListeners(commit);
		}
	}

	/**
	 * Get commits after initial release
	 * 
	 * @return
	 */
	protected List<Commit> getCommitsAfterInitialRelease() {
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
	 * 
	 * @param commitId
	 */
	protected List<Commit> getCommitsAfter(String commitId) {
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

	/**
	 * Get a commit at given commit ID
	 * 
	 * @param commitId
	 * @return
	 */
	protected List<Commit> getCommitAt(String commitId) {
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

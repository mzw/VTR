package jp.mzw.vtr.git;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.dict.DictionaryParser;
import jp.mzw.vtr.maven.MavenUtils;

public class CheckoutConductor {
	protected static Logger LOGGER = LoggerFactory.getLogger(CheckoutConductor.class);

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
		LOGGER.info("Complete to checkout given commits");
	}

	/**
	 * Checkout given commits
	 * 
	 * @param commits
	 * @throws GitAPIException
	 */
	private void checkout(List<Commit> commits) throws GitAPIException {
		for (Commit commit : commits) {
			checkout(commit);
			notifyListeners(commit);
		}
		// Recover initial state
		checkout(getLatestCommit());
	}
	
	/**
	 * Checkout given commit
	 * @param commit
	 * @throws GitAPIException
	 */
	private void checkout(Commit commit) throws GitAPIException {
		git.stashCreate().call();
		Collection<RevCommit> stashes = git.stashList().call();
		if (!stashes.isEmpty()) {
			git.stashDrop().setStashRef(0).call();
		}
		git.checkout().setName(commit.getId()).call();
		LOGGER.info("Checkout: {}", commit.getId());
	}
	

	/**
	 * Maven compile to obtain source programs covered by test cases
	 */
	public static void before(File projectDir, File mavenHome) {
		try {
			MavenUtils.maven(projectDir, Arrays.asList("compile", "test-compile"), mavenHome);
		} catch (MavenInvocationException e) {
			LOGGER.warn("Failed to compile subject");
			return;
		}
	}

	/**
	 * Maven clean to initialize
	 */
	public static void after(File projectDir, File mavenHome) {
		try {
			MavenUtils.maven(projectDir, Arrays.asList("clean"), mavenHome);
		} catch (MavenInvocationException e) {
			LOGGER.warn("Failed to clean subject");
			return;
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

	/**
	 * Get latest commit
	 * @return latest commit
	 */
	public Commit getLatestCommit() {
		int index = this.commits.size() - 1;
		return this.commits.get(index);
	}
	
	/**
	 * 
	 * @return
	 */
	public Commit getOneBeforeInitialRelease() {
		Commit prev = null;
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
				prev = commit;
				continue;
			}
			return prev;
		}
		return null;
		
	}
	
}

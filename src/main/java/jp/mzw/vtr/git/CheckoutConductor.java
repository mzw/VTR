package jp.mzw.vtr.git;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.dict.Dictionary;
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
	private void onCheckout(Commit commit) {
		for (Listener listener : this.listenerSet) {
			listener.onCheckout(commit);
		}
	}

	Git git;
	Dictionary dict;

	public CheckoutConductor(Project project) throws IOException, ParseException {
		this.git = GitUtils.getGit(project.getProjectDir());
		this.dict = new Dictionary(project.getOutputDir(), project.getProjectId()).parse();
	}
	
	public CheckoutConductor(String projectId, File projectDir, File outputDir) throws IOException, ParseException {
		this.git = GitUtils.getGit(projectDir);
		this.dict = new Dictionary(outputDir, projectId).parse();
	}

	/**
	 * API for conducting checkout
	 * 
	 * @throws GitAPIException
	 * @throws ParseException
	 * @throws IOException
	 */
	public void checkout() throws GitAPIException, IOException, ParseException {
		List<Commit> commits = getCommitsAfterInitialRelease();
		checkout(commits);
	}

	/**
	 * Types representing checkout after or at given commit ID
	 * 
	 * @author Yuta Maezawa
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
	 * @throws ParseException
	 * @throws IOException
	 */
	public void checkout(Type type, String commitId) throws GitAPIException, IOException, ParseException {
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
	 * @throws ParseException
	 * @throws IOException
	 */
	private void checkout(List<Commit> commits) throws GitAPIException, IOException, ParseException {
		for (Commit commit : commits) {
			try {
				checkout(commit);
			} catch (GitAPIException e) {
				LOGGER.warn("Failed to checkout @ {}", commit.getId());
				git.clean().call();
				continue;
			}
			onCheckout(commit);
		}
		// Recover initial state
		checkout(getLatestCommit());
	}

	/**
	 * Temporary implementation
	 *
	 * @param commitId
	 * @throws GitAPIException
	 * @throws IOException
	 * @throws ParseException
	 *
	 * @deprecated
	 */
	public void checkoutAt(String commitId) throws GitAPIException, IOException, ParseException {
		for (Commit commit : getCommitAt(commitId)) {
			try {
				checkout(commit);
			} catch (GitAPIException e) {
				LOGGER.warn("Failed to checkout @ {}", commit.getId());
				git.clean().call();
				continue;
			}
		}
	}

	/**
	 * Checkout given commit
	 * 
	 * @param commit
	 * @throws GitAPIException
	 */
	public void checkout(Commit commit) throws GitAPIException {
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
	public static int before(File projectDir, File mavenHome, boolean mavenOutput) {
		try {
			return MavenUtils.maven(projectDir, Arrays.asList("compile", "test-compile"), mavenHome, mavenOutput);
		} catch (MavenInvocationException e) {
			LOGGER.warn("Failed to compile subject");
			return -1;
		}
	}

	/**
	 * Maven clean to initialize
	 */
	public static int after(File projectDir, File mavenHome, boolean mavenOutput) {
		try {
			return MavenUtils.maven(projectDir, Arrays.asList("clean"), mavenHome, mavenOutput);
		} catch (MavenInvocationException e) {
			LOGGER.warn("Failed to clean subject");
			return -1;
		}
	}

	/**
	 * Get commits after initial release
	 * 
	 * @return
	 */
	protected List<Commit> getCommitsAfterInitialRelease() {
		List<Commit> ret = new ArrayList<>();
		for (Commit commit : this.dict.getCommits()) {
			// Skip until initial release
			boolean init = true;
			for (Tag tag : dict.getTags()) {
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
		for (Commit commit : this.dict.getCommits()) {
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
		for (Commit commit : this.dict.getCommits()) {
			if (commit.getId().equals(commitId)) {
				ret.add(commit);
				return ret;
			}
		}
		return ret;
	}

	/**
	 * Get latest commit
	 * 
	 * @return latest commit
	 */
	public Commit getLatestCommit() {
		int index = this.dict.getCommits().size() - 1;
		return this.dict.getCommits().get(index);
	}

	/**
	 * 
	 * @return
	 */
	public Commit getOneBeforeInitialRelease() {
		Commit prev = null;
		for (Commit commit : this.dict.getCommits()) {
			// Skip until initial release
			boolean init = true;
			for (Tag tag : dict.getTags()) {
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

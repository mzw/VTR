package jp.mzw.vtr.git;

import java.util.Date;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

public class Commit {

	/** Commit ID */
	String id;

	/** Commit date */
	Date date;

	/**
	 * Commit
	 * 
	 * @param id
	 *            ID
	 * @param date
	 *            Date
	 */
	public Commit(String id, Date date) {
		this.id = id;
		this.date = date;
	}

	/**
	 * Commit based on RevCommit instantiated by JGit
	 * 
	 * @param commit
	 */
	public Commit(RevCommit commit) {
		this.id = commit.getId().name();
		this.date = commit.getAuthorIdent().getWhen();
	}

	/**
	 * Get commit ID
	 * 
	 * @return Commit ID
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Get commit date
	 * 
	 * @return commit date
	 */
	public Date getDate() {
		return this.date;
	}

	/**
	 * Get commit by given commit ID from given commit list
	 * 
	 * @param commitId
	 *            Commit ID
	 * @param commits
	 *            Commit list
	 * @return Specified commit or null if no commit in the list
	 */
	public static Commit getCommitBy(String commitId, List<Commit> commits) {
		for (Commit commit : commits) {
			if (commit.getId().equals(commitId)) {
				return commit;
			}
		}
		return null;
	}
}

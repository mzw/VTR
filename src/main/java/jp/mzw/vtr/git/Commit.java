package jp.mzw.vtr.git;

import java.util.Date;
import java.util.List;

import org.eclipse.jgit.revwalk.RevCommit;

public class Commit {

	String id;
	Date date;
	public Commit(String id, Date date) {
		this.id = id;
		this.date = date;
	}
	
	public Commit(RevCommit commit) {
		this.id = commit.getId().name();
		this.date = commit.getAuthorIdent().getWhen();
	}
	
	public String getId() {
		return this.id;
	}
	
	public Date getDate() {
		return this.date;
	}
	
	public static Commit getCommitBy(String commitId, List<Commit> commits) {
		for (Commit commit : commits) {
			if (commit.getId().equals(commitId)) {
				return commit;
			}
		}
		return null;
	}
}

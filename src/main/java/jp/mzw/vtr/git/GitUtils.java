package jp.mzw.vtr.git;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.patch.Patch;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitUtils {
	static Logger log = LoggerFactory.getLogger(GitUtils.class);

	public static final String GIT_DIR = ".git";

	/**
	 * 
	 * @param pathToGitRepo
	 * @return
	 * @throws IOException
	 */
	public static Git getGit(File projectDir) throws IOException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repository = builder.setGitDir(new File(projectDir, GitUtils.GIT_DIR)).readEnvironment().findGitDir().build();
		return new Git(repository);
	}

	/**
	 * 
	 * @param pathToGitRepo
	 * @param gitDir
	 * @return
	 * @throws IOException
	 */
	public static Git getGit(File projectDir, String gitDir) throws IOException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repository = builder.setGitDir(new File(projectDir, gitDir)).readEnvironment().findGitDir().build();
		return new Git(repository);
	}

	/**
	 *
	 * @param git
	 * @return
	 */
	public static String getRemoteOriginUrl(Git git) {
		StoredConfig config = git.getRepository().getConfig();
		return config.getString("remote", "origin", "url");
	}

	/**
	 * 
	 * @param git
	 * @return
	 */
	public static String getRefToCompareBranch(Git git) {
		StoredConfig config = git.getRepository().getConfig();
		String subsection = config.getSubsections("branch").iterator().next();
		return config.getString("branch", subsection, "merge");
	}

	/**
	 * 
	 * @param git
	 * @param branchName
	 * @return
	 * @throws GitAPIException
	 */
	public static Ref getBranch(Git git, String branchName) throws GitAPIException {
		List<Ref> branchList = git.branchList().call();
		for (Ref branch : branchList) {
			if (branch.getName().equals(branchName)) {
				return branch;
			}
		}
		return null;
	}

	/**
	 * Get RevCommit by given Commit
	 * 
	 * @param repository
	 * @param commit
	 * @return
	 * @throws RevisionSyntaxException
	 * @throws AmbiguousObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	public static RevCommit getCommit(Repository repository, Commit commit) throws RevisionSyntaxException, AmbiguousObjectException,
			IncorrectObjectTypeException, IOException {
		ObjectId id = repository.resolve(commit.getId());
		RevWalk walk = new RevWalk(repository);
		return walk.parseCommit(id);
	}

	/**
	 * Get patches between given previous and current commits
	 * 
	 * @param prv
	 * @param cur
	 * @return
	 * @throws RevisionSyntaxException
	 * @throws AmbiguousObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	public static Patch getPatch(Repository rep, Commit prv, Commit cur) throws RevisionSyntaxException, AmbiguousObjectException,
			IncorrectObjectTypeException, IOException {
		Patch patch = new Patch();
		// Get commits
		RevCommit prvCommit = GitUtils.getCommit(rep, prv);
		RevCommit curCommit = GitUtils.getCommit(rep, cur);
		// Get diff
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DiffFormatter df = new DiffFormatter(baos);
		df.setRepository(rep);
		if (prvCommit != null && curCommit != null) {
			List<DiffEntry> changes = df.scan(prvCommit.getTree(), curCommit.getTree());
			for (DiffEntry change : changes) {
				df.format(df.toFileHeader(change));
				String raw = baos.toString();
				InputStream is = new ByteArrayInputStream(raw.getBytes());
				patch.parse(is);
			}
		}
		return patch;
	}
}

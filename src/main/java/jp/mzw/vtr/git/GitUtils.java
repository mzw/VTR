package jp.mzw.vtr.git;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitUtils {
	static Logger log = LoggerFactory.getLogger(GitUtils.class);

	public static final String DOT_GIT = ".git";

	/**
	 * 
	 * @param pathToGitRepo
	 * @return
	 * @throws IOException
	 */
	public static Git getGit(String pathToGitRepo) throws IOException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repository = builder.setGitDir(new File(pathToGitRepo, GitUtils.DOT_GIT)).readEnvironment().findGitDir().build();
		return new Git(repository);
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

}

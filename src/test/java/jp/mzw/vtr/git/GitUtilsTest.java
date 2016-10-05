package jp.mzw.vtr.git;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import jp.mzw.vtr.VtrTestBase;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;

public class GitUtilsTest extends VtrTestBase {

	@Test
	public void testGetGit() throws IOException, GitAPIException {
		Git git = GitUtils.getGit(new File(PATH_TO_PROJECT_DIR));
		assertNotNull(git);
	}

	@Test
	public void testGitConfig() throws IOException, GitAPIException {
		Git git = GitUtils.getGit(new File("src/test/resources/git-config"), "vtr-example");
		String url = GitUtils.getRemoteOriginUrl(git);
		assertArrayEquals("https://github.com/mzw/vtr-example".toCharArray(), url.toCharArray());
	}

	@Test
	public void testGitConfigForDbUtils() throws IOException, GitAPIException {
		Git git = GitUtils.getGit(new File("src/test/resources/git-config"), "commons-dbutils");
		String url = GitUtils.getRemoteOriginUrl(git);
		assertArrayEquals("https://github.com/apache/commons-dbutils".toCharArray(), url.toCharArray());
	}

	@Test
	public void testGetRefToCompareBranch() throws IOException, GitAPIException {
		Git git = GitUtils.getGit(new File("src/test/resources/git-config"), "vtr-example");
		String ref = GitUtils.getRefToCompareBranch(git);
		assertArrayEquals("refs/heads/master".toCharArray(), ref.toCharArray());
	}

	@Test
	public void testGetRefToCompareBranchForDbUtils() throws IOException, GitAPIException {
		Git git = GitUtils.getGit(new File("src/test/resources/git-config"), "commons-dbutils");
		String ref = GitUtils.getRefToCompareBranch(git);
		assertArrayEquals("refs/heads/trunk".toCharArray(), ref.toCharArray());
	}

}

package jp.mzw.vtr.git;

import java.io.IOException;

import jp.mzw.vtr.VtrTestBase;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.junit.Assert;
import org.junit.Test;

public class GitUtilsTest extends VtrTestBase {

	@Test
	public void testGetGit() throws IOException {
		Git git = GitUtils.getGit(PATH_TO_PROJECT_DIR);
		Assert.assertNotNull(git);
	}

	@Test
	public void testGetBranch() throws GitAPIException {
		Ref masterBranch = GitUtils.getBranch(git, "refs/heads/master");
		Assert.assertNotNull(masterBranch);
		Ref nullBranch = GitUtils.getBranch(git, "refs/heads/develop");
		Assert.assertNull(nullBranch);
	}

}

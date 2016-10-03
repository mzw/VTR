package jp.mzw.vtr.git;

import java.io.IOException;

import jp.mzw.vtr.VtrTestBase;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;

public class GitUtilsTest extends VtrTestBase {
	
	Git git;

	@Test
	public void testGetGit() throws IOException, GitAPIException {
		this.git = GitUtils.getGit(PATH_TO_PROJECT_DIR);
		Assert.assertNotNull(git);
	}

}

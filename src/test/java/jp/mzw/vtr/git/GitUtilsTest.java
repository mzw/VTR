package jp.mzw.vtr.git;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GitUtilsTest {

	public static final String PATH_TO_PROJECT_DIR = "src/test/resources/vtr-example";
	protected Git git;
	
	@Before
	public void setup() throws IOException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repository = builder.setGitDir(new File(PATH_TO_PROJECT_DIR, GitUtils.DOT_GIT)).readEnvironment().findGitDir().build();
		this.git = new Git(repository);
	}
	
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

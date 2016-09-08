package jp.mzw.vtr.git;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.GitUtils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CheckoutConductorTest {

	public static final String PATH_TO_GIT_REPO = "src/test/resources/vtr-example";
	public static final String PATH_TO_OUTPUT_DIR = "src/test/resources/output/vtr-example";

	protected Git git;

	@Before
	public void setup() throws IOException {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		Repository repository = builder.setGitDir(new File(PATH_TO_GIT_REPO, GitUtils.DOT_GIT)).readEnvironment().findGitDir().build();
		this.git = new Git(repository);
	}

	@Test
	public void testGetCommitsAfterInitialRelease() throws IOException, ParseException {
		CheckoutConductor cc = new CheckoutConductor(git, new File(PATH_TO_OUTPUT_DIR));
		List<Commit> commits = cc.getCommitsAfterInitialRelease();
		Assert.assertEquals(2, commits.size());
	}
	
	@Test
	public void testGetCommitsAfter() throws IOException, ParseException {
		CheckoutConductor cc = new CheckoutConductor(git, new File(PATH_TO_OUTPUT_DIR));
		List<Commit> commits = cc.getCommitsAfter("57da0a18d92daf0d9750f4665bdccb3eb054e2db");
		Assert.assertEquals(3, commits.size());
	}

	@Test
	public void testGetCommitsAfterWithInvalidCommitId() throws IOException, ParseException {
		CheckoutConductor cc = new CheckoutConductor(git, new File(PATH_TO_OUTPUT_DIR));
		List<Commit> commits = cc.getCommitsAfter("maezawa8d92daf0d9750f4665bdccb3eb054e2db");
		Assert.assertEquals(0, commits.size());
	}

	@Test
	public void testGetCommitAt() throws IOException, ParseException {
		CheckoutConductor cc = new CheckoutConductor(git, new File(PATH_TO_OUTPUT_DIR));
		List<Commit> commits = cc.getCommitAt("57da0a18d92daf0d9750f4665bdccb3eb054e2db");
		Assert.assertEquals(1, commits.size());
	}

	@Test
	public void testGetCommitAtWithInvalidCommitId() throws IOException, ParseException {
		CheckoutConductor cc = new CheckoutConductor(git, new File(PATH_TO_OUTPUT_DIR));
		List<Commit> commits = cc.getCommitAt("maezawa8d92daf0d9750f4665bdccb3eb054e2db");
		Assert.assertEquals(0, commits.size());
	}

}

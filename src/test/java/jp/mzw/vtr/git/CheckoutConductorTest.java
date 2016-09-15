package jp.mzw.vtr.git;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;

import org.junit.Assert;
import org.junit.Test;

public class CheckoutConductorTest extends VtrTestBase {

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

	@Test
	public void testGetLatestCommit() throws IOException, ParseException {
		CheckoutConductor cc = new CheckoutConductor(git, new File(PATH_TO_OUTPUT_DIR));
		Commit commit = cc.getLatestCommit();
		Assert.assertArrayEquals("fcf9382884874b7ceecc16cd2155ab73b1346931".toCharArray(), commit.id.toCharArray());
	}

}

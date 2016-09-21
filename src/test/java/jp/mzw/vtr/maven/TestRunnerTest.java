package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.TestRunner;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Test;

public class TestRunnerTest extends VtrTestBase {

//	@Test
	public void testRevertGitRepository() throws IOException, ParseException, GitAPIException {
		CheckoutConductor cc = new CheckoutConductor(git, new File(PATH_TO_OUTPUT_DIR));
		cc.addListener(new TestRunner(this.project));
		cc.checkout();
		Iterable<RevCommit> commits = this.git.log().call();
		boolean latest = false;
		for (RevCommit commit : commits) {
			if (commit.name().equals("fcf9382884874b7ceecc16cd2155ab73b1346931")) {
				latest = true;
				break;
			}
		}
		Assert.assertTrue(latest);
	}

//	@Test
	public void testOnCheckout() {
		try {
			TestRunner tr = new TestRunner(this.project);
			tr.onCheckout(new Commit("fcf9382884874b7ceecc16cd2155ab73b1346931", new Date()));
		} catch (IOException e) {
			Assert.fail();
		}
	}

}

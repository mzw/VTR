package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.dict.DictionaryBase;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.TestRunner;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class TestRunnerTest extends VtrTestBase {

	public static final String COMMIT_ID = "fcf9382884874b7ceecc16cd2155ab73b1346931";
	public static final String COMMIT_DATE = "2016-03-05 15:47:44 +0900";

	protected Commit commit;

	@Before
	public void setup() throws ParseException {
		this.commit = new Commit(COMMIT_ID, DictionaryBase.SDF.parse(COMMIT_DATE));
	}

	// @Test
	public void testRevertGitRepository() throws IOException, ParseException, GitAPIException {
		CheckoutConductor cc = new CheckoutConductor(this.project);
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
		assertTrue(latest);
	}

	// @Test
	public void testOnCheckout() throws ParseException {
		try {
			TestRunner tr = new TestRunner(this.project);
			tr.onCheckout(new Commit("fcf9382884874b7ceecc16cd2155ab73b1346931", new Date()));
		} catch (IOException e) {
			fail();
		}
	}

	@Test
	public void testGetOutputDir() throws IOException, ParseException {
		TestRunner tr = new TestRunner(this.project);
		File dir = tr.getOutputDir(this.commit);
		assertArrayEquals("output/vtr-example/jacoco/fcf9382884874b7ceecc16cd2155ab73b1346931".toCharArray(), dir.getPath().toCharArray());
	}

}

package jp.mzw.vtr.dict;

import java.io.IOException;
import java.text.ParseException;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.git.Commit;

public class DictionaryTest extends VtrTestBase {

	public static final String COMMIT_ID = "fcf9382884874b7ceecc16cd2155ab73b1346931";
	public static final String COMMIT_DATE = "2016-03-05 15:47:44 +0900";

	public static final String PREV_COMMIT_ID = "7fcfdfa99bf9f220b9643f372c36609ca35c60b3";

	protected Commit commit;

	@Before
	public void setup() throws IOException, MavenInvocationException, ParseException, GitAPIException {
		this.commit = new Commit(COMMIT_ID, DictionaryBase.SDF.parse(COMMIT_DATE));
	}

	@Test
	public void testConstructor() {
		Dictionary dict = new Dictionary(this.project.getOutputDir(), this.project.getProjectId());
		Assert.assertNotNull(dict);
	}

	@Test
	public void testParse() throws IOException, ParseException {
		Dictionary dict = new Dictionary(this.project.getOutputDir(), this.project.getProjectId());
		dict.parse();
		Assert.assertEquals(4, dict.getCommits().size());
		Assert.assertEquals(3, dict.getTags().size());
	}

	@Test
	public void testPrevCommits() throws IOException, ParseException {
		Dictionary dict = new Dictionary(this.project.getOutputDir(), this.project.getProjectId());
		dict.parse();
		dict.createPrevCommitByCommitIdMap();
		Commit prevCommit = dict.getPrevCommitBy(COMMIT_ID);
		Assert.assertNotNull(prevCommit);
		Assert.assertArrayEquals(PREV_COMMIT_ID.toCharArray(), prevCommit.getId().toCharArray());
	}

}

package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.dict.DictionaryBase;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.TestRunner;
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

	@Test
	public void testGetOutputDir() throws IOException, ParseException {
		TestRunner tr = new TestRunner(this.project);
		File dir = tr.getOutputDir(this.commit, false);
		assertArrayEquals((PATH_TO_OUTPUT_DIR + "/jacoco/" + COMMIT_ID).toCharArray(), dir.getPath().toCharArray());
	}
	
	@Test
	public void testSkipTestRunnerTest() throws IOException, ParseException {
		TestRunner tr = new TestRunner(this.project);
		List<String> list = tr.parseTestRunnerSkipList();
		assertTrue(list.contains("org.apache.commons.exec.DefaultExecutorTest#testExecuteWithStdin@ebbbf46365fdfb560b17ae3faa2beb1afb27d63b"));
		assertFalse(tr.skip(commit, new TestCase("testNonExistFile", "jp.mzw.vtr.example.FileUtilsTest", null, null, null)));
	}

}

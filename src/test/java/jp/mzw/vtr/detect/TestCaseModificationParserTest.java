package jp.mzw.vtr.detect;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.dict.DictionaryBase;
import jp.mzw.vtr.git.Commit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestCaseModificationParserTest extends VtrTestBase {
	
	public static final String COMMIT_ID = "fcf9382884874b7ceecc16cd2155ab73b1346931";
	public static final String COMMIT_DATE = "2016-03-05 15:47:44 +0900";
	
	protected Commit commit;
	
	@Before
	public void setup() throws IOException, MavenInvocationException, ParseException, GitAPIException {
		this.commit = new Commit(COMMIT_ID, DictionaryBase.SDF.parse(COMMIT_DATE));
	}
	
	@After
	public void teardown() throws GitAPIException, MavenInvocationException {
	}

	@Test
	public void testDetectingSubjectTestCaseModification() throws IOException {
		TestCaseModificationParser parser = new TestCaseModificationParser(this.project);
		List<TestCaseModification> tcm = parser.parse(commit);
		Assert.assertEquals(1, tcm.size());
		Assert.assertArrayEquals("jp.mzw.vtr.example.FileUtilsTest#testNonExistFile".toCharArray(), tcm.get(0).getTestCase().getFullName().toCharArray());

		Map<String, List<Integer>> covered = tcm.get(0).getCoveredLines();
		List<Integer> lines = covered.get("src/main/java/jp/mzw/vtr/example/FileUtils.java");
		Assert.assertEquals(12, lines.get(0).intValue());
	}

}

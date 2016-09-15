package jp.mzw.vtr.detect;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.git.Commit;

import org.junit.Assert;
import org.junit.Test;

public class TestCaseModificationParserTest extends VtrTestBase {

	@Test
	public void testDetectingSubjectTestCaseModification() throws IOException {
		TestCaseModificationParser parser = new TestCaseModificationParser(this.project);
		List<TestCaseModification> tcm = parser.parse(new Commit("fcf9382884874b7ceecc16cd2155ab73b1346931", new Date()));
		Assert.assertEquals(1, tcm.size());
		Assert.assertArrayEquals("jp.mzw.vtr.example.FileUtilsTest#testNonExistFile".toCharArray(), tcm.get(0).getTestCase().getFullName().toCharArray());

		Map<String, List<Integer>> covered = tcm.get(0).getCoveredLines();
		List<Integer> lines = covered.get("src/main/java/jp/mzw/vtr/example/FileUtils.java");
		Assert.assertEquals(12, lines.get(0).intValue());
	}
}

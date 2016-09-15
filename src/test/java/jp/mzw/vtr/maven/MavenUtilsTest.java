package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jp.mzw.vtr.VtrTestBase;

import org.junit.Assert;
import org.junit.Test;

public class MavenUtilsTest extends VtrTestBase {

	@Test
	public void testGetTestMethods() throws IOException {
		List<TestSuite> testSuites = MavenUtils.getTestSuites(new File(PATH_TO_PROJECT_DIR));
		Assert.assertEquals(1, testSuites.size());
		Assert.assertEquals(6, testSuites.get(0).getTestCases().size());
	}
}

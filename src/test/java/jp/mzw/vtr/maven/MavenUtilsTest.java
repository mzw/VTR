package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jp.mzw.vtr.core.Config;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MavenUtilsTest {

	public static final String PATH_TO_MVN_PRJ = "src/test/resources/vtr-example";
	
	protected Config config;

	@Before
	public void setup() throws IOException {
		this.config = new Config("config.properties");
	}
	
	@Test
	public void testGetTestMethods() throws IOException {
		List<TestSuite> testSuites = MavenUtils.getTestSuites(new File(PATH_TO_MVN_PRJ));
		Assert.assertEquals(1, testSuites.size());
		Assert.assertEquals(6, testSuites.get(0).getTestCases().size());
	}
}

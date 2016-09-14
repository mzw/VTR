package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;

import jp.mzw.vtr.core.Project;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MavenUtilsTest {

	public static final String PROJECT_ID = "vtr-example";
	public static final String PATH_TO_PROJECT_DIR = "src/test/resources/vtr-example";
	
	protected Project project;
	
	@Before
	public void setup() throws IOException {
		this.project = new Project(PROJECT_ID, PATH_TO_PROJECT_DIR);
		this.project.setConfig("config.properties");
	}
	
	@Test
	public void testGetTestMethods() throws IOException {
		List<TestSuite> testSuites = MavenUtils.getTestSuites(new File(PATH_TO_PROJECT_DIR));
		Assert.assertEquals(1, testSuites.size());
		Assert.assertEquals(6, testSuites.get(0).getTestCases().size());
	}
}

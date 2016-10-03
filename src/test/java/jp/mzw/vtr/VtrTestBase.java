package jp.mzw.vtr;

import java.io.IOException;

import jp.mzw.vtr.core.Project;

import org.junit.Before;

public class VtrTestBase {

	public static final String PROJECT_ID = "vtr-example";
	public static final String PATH_TO_PROJECT_DIR = "src/test/resources/vtr-subjects/vtr-example";
	public static final String TEST_CONFIG_FILENAME = "test-config.properties";
	public static final String PATH_TO_OUTPUT_DIR = "src/test/resources/vtr-output/vtr-example";

	protected Project project;

	@Before
	public void _setup() throws IOException {
		this.project = new Project(PROJECT_ID, PATH_TO_PROJECT_DIR);
		this.project.setConfig(TEST_CONFIG_FILENAME);
	}
	
}

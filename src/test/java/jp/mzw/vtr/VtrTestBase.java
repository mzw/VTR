package jp.mzw.vtr;

import java.io.IOException;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.GitUtils;

import org.eclipse.jgit.api.Git;
import org.junit.Before;

public class VtrTestBase {

	public static final String PROJECT_ID = "vtr-example";
	public static final String PATH_TO_PROJECT_DIR = "src/test/resources/vtr-example";
	public static final String PATH_TO_OUTPUT_DIR = "src/test/resources/output/vtr-example";

	protected Git git;
	protected Project project;

	@Before
	public void _setup() throws IOException {
		this.git = GitUtils.getGit(PATH_TO_PROJECT_DIR);
		this.project = new Project(PROJECT_ID, PATH_TO_PROJECT_DIR);
		this.project.setConfig("test-config.properties");
	}
	
}

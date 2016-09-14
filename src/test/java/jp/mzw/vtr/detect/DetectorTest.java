package jp.mzw.vtr.detect;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.GitUtils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DetectorTest {

	public static final String PROJECT_ID = "vtr-example";
	public static final String PATH_TO_PROJECT_DIR = "src/test/resources/vtr-example";
	public static final String PATH_TO_OUTPUT_DIR = "src/test/resources/output/vtr-example";

	protected Git git;
	protected Project project;

	@Before
	public void setup() throws IOException {
		this.git = GitUtils.getGit(PATH_TO_PROJECT_DIR);
		this.project = new Project(PROJECT_ID, PATH_TO_PROJECT_DIR);
		this.project.setConfig("config.properties");
	}

	@Test
	public void testDetectingSubjectTestCaseModification() throws IOException, ParseException, GitAPIException {
		try {
			CheckoutConductor cc = new CheckoutConductor(this.git, new File(PATH_TO_OUTPUT_DIR));
			cc.addListener(new Detector(this.project));
			cc.checkout();
		} catch (Exception e) {
			Assert.fail();
		}
	}

}

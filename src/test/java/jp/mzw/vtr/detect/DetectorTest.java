package jp.mzw.vtr.detect;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import jp.mzw.vtr.core.Config;
//import jp.mzw.vtr.dict.DictionaryBase;
import jp.mzw.vtr.git.CheckoutConductor;
//import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.GitUtils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DetectorTest {

	public static final String SUBJECT_ID = "vtr-example";
	public static final String PATH_TO_SUBJECT = "src/test/resources/vtr-example";
	public static final String PATH_TO_OUTPUT_DIR = "src/test/resources/output/vtr-example";

	protected Git git;
	protected Config config;

	@Before
	public void setup() throws IOException {
		this.git = GitUtils.getGit(PATH_TO_SUBJECT);
		this.config = new Config("config.properties");
	}

//	@Test
//	public void testOnCheckout() {
//		try {
//			Detector detector = new Detector(SUBJECT_ID, PATH_TO_SUBJECT, this.config);
//			detector.onCheckout(new Commit("fcf9382884874b7ceecc16cd2155ab73b1346931", DictionaryBase.SDF.parse("2016-03-05 15:47:44 +0900")));
//		} catch (Exception e) {
//			Assert.fail();
//		}
//	}

	@Test
	public void testRevertGitRepository() throws IOException, ParseException, GitAPIException {
		try {
			CheckoutConductor cc = new CheckoutConductor(this.git, new File(PATH_TO_OUTPUT_DIR));
			cc.addListener(new Detector(SUBJECT_ID, PATH_TO_SUBJECT, this.config));
			cc.checkout();
		} catch (Exception e) {
			Assert.fail();
		}
	}
	
}

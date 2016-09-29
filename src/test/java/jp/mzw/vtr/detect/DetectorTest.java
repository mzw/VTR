package jp.mzw.vtr.detect;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.dict.DictionaryBase;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DetectorTest extends VtrTestBase {

	public static final String COMMIT_ID = "fcf9382884874b7ceecc16cd2155ab73b1346931";
	public static final String COMMIT_DATE = "2016-03-05 15:47:44 +0900";
	
	protected Commit commit;
	protected CheckoutConductor cc;
	
	@Before
	public void setup() throws IOException, MavenInvocationException, ParseException, GitAPIException {
		this.commit = new Commit(COMMIT_ID, DictionaryBase.SDF.parse(COMMIT_DATE));
		this.cc = new CheckoutConductor(this.git, new File(PATH_TO_OUTPUT_DIR));
		this.cc.checkout(CheckoutConductor.Type.At, commit.getId());
		MavenUtils.maven(this.project.getProjectDir(), Arrays.asList("compile"), this.project.getMavenHome());
	}
	
	@After
	public void teardown() throws GitAPIException, MavenInvocationException {
		MavenUtils.maven(this.project.getProjectDir(), Arrays.asList("clean"), this.project.getMavenHome());
		this.cc.checkout(CheckoutConductor.Type.At, "refs/heads/master");
	}

//	@Test
//	public void testSetCoverageResults() throws IOException, MavenInvocationException, ParseException, GitAPIException {
//		Detector detector = new Detector(this.project);
//		List<TestSuite> ts = detector.setCoverageResults(this.commit);
//		Assert.assertFalse(ts.isEmpty());
//		for (TestCase tc : ts.get(0).getTestCases()) {
//			Map<File, List<Integer>> map = tc.getCoveredClassLinesMap();
//			for (File key : map.keySet()) {
//				List<Integer> lines = map.get(key);
//				Assert.assertFalse(lines.isEmpty());
//			}
//		}
//	}
//
//	@Test
//	public void testDetect() throws IOException, MavenInvocationException, ParseException, GitAPIException {
//		Detector detector = new Detector(this.project);
//		List<TestSuite> ts = detector.setCoverageResults(this.commit);
//		List<TestCaseModification> tcmList = detector.detect(this.commit, ts);
//		Assert.assertEquals(1, tcmList.size());
//		TestCaseModification tcm = tcmList.get(0);
//		Assert.assertArrayEquals("jp.mzw.vtr.example.FileUtilsTest#testNonExistFile".toCharArray(), tcm.getTestCase().getFullName().toCharArray());
//		Map<File, List<Integer>> map = tcm.getTestCase().getCoveredClassLinesMap();
//		for (File key : map.keySet()) {
//			List<Integer> lines = map.get(key);
//			Assert.assertFalse(lines.isEmpty());
//		}
//	}
//	
//	@Test
//	public void testOutput() throws IOException, MavenInvocationException, ParseException, GitAPIException {
//		Detector detector = new Detector(this.project);
//		File file = detector.getOutputFile(commit);
//		Assert.assertArrayEquals("fcf9382884874b7ceecc16cd2155ab73b1346931.xml".toCharArray(), file.getName().toCharArray());
//		List<TestSuite> ts = detector.setCoverageResults(this.commit);
//		List<TestCaseModification> tcmList = detector.detect(this.commit, ts);
//		String xml = detector.getXml(tcmList);
//		Assert.assertTrue(xml.contains("<Line number=\"12\""));
//	}
	
}

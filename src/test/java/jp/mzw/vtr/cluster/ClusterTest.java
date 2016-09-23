package jp.mzw.vtr.cluster;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.cluster.difflib.ChunkTagRest;
import jp.mzw.vtr.detect.TestCaseModification;
import jp.mzw.vtr.detect.TestCaseModificationParser;
import jp.mzw.vtr.dict.DictionaryBase;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import difflib.Patch;

public class ClusterTest extends VtrTestBase {

	public static final String COMMIT_ID = "fcf9382884874b7ceecc16cd2155ab73b1346931";
	public static final String COMMIT_DATE = "2016-03-05 15:47:44 +0900";

	public static final String PREV_COMMIT_ID = "7fcfdfa99bf9f220b9643f372c36609ca35c60b3";
	public static final String PREV_COMMIT_DATE = "2016-03-05 15:46:42 +0900";

//	public static final String COMMIT_ID = "7fcfdfa99bf9f220b9643f372c36609ca35c60b3";
//	public static final String COMMIT_DATE = "2016-03-05 15:46:42 +0900";
//
//	public static final String PREV_COMMIT_ID = "57da0a18d92daf0d9750f4665bdccb3eb054e2db";
//	public static final String PREV_COMMIT_DATE = "2016-03-05 15:45:09 +0900";

	protected Commit commit;
	protected Commit prevCommit;
	protected Cluster cluster;
	TestCaseModificationParser parser;

	@Before
	public void setup() throws ParseException, IOException {
		this.commit = new Commit(COMMIT_ID, DictionaryBase.SDF.parse(COMMIT_DATE));
		this.prevCommit = new Commit(PREV_COMMIT_ID, DictionaryBase.SDF.parse(PREV_COMMIT_DATE));
		this.cluster = new Cluster(this.project);
		this.parser = new TestCaseModificationParser(project);
	}

	// @Test
	public void testGetModifiedPartialSource() throws IOException {
		List<TestCaseModification> tcmList = this.parser.parse(commit);
		Assert.assertEquals(1, tcmList.size());
	}

//	@Test
	public void testGetModifiedTestSuites() throws IOException, GitAPIException {
		List<TestSuite> tsList = MavenUtils.getTestSuites(this.project.getProjectDir());
		List<TestCaseModification> tcmList = this.parser.parse(commit);
		List<TestSuite> modifiedTestSuites = this.cluster.getModifiedTestSuites(tsList, tcmList);
		Assert.assertEquals(1, modifiedTestSuites.size());
		TestSuite ts = modifiedTestSuites.get(0);
		Assert.assertEquals(1, ts.getTestCases().size());
		TestCase tc = ts.getTestCases().get(0);
		Assert.assertArrayEquals("jp.mzw.vtr.example.FileUtilsTest#testNonExistFile".toCharArray(), tc.getFullName().toCharArray());
	}
	
//	@Test
	public void testGetPatch() throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
		Map<Patch<ChunkTagRest>, String> patches = this.cluster.getPatches(prevCommit, commit);
		Assert.assertEquals(1, patches.size());
	}
	
	@Test
	public void testGetTestCaseModificationContents() throws IOException, GitAPIException {
		List<TestSuite> tsList = MavenUtils.getTestSuites(this.project.getProjectDir());
		List<TestCaseModification> tcmList = this.parser.parse(commit);
		List<TestSuite> modifiedTestSuites = this.cluster.getModifiedTestSuites(tsList, tcmList);
//		this.cluster.getTestCaseModificationContents(this.commit, modifiedTestSuites);
		this.cluster.getTestCaseModificationContents(this.commit, tsList);
	}

}

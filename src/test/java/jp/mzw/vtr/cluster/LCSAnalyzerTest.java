package jp.mzw.vtr.cluster;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.detect.TestCaseModification;

import org.junit.Before;
import org.junit.Test;

public class LCSAnalyzerTest extends VtrTestBase {

	public static final String PATH_TO_OUTPUT_DIR = "src/test/resources/output";
//	public static final String PATH_TO_OUTPUT_DIR = "output";
	
	protected File outputDir;
	protected LCSAnalyzer lcsAnalyzer;
	
	@Before
	public void setup() {
		this.outputDir = new File(PATH_TO_OUTPUT_DIR);
		this.lcsAnalyzer = new LCSAnalyzer(this.outputDir);
	}
	
	@Test
	public void testConstructor() throws IOException {
		LCSAnalyzer analyzer = new LCSAnalyzer(this.project.getOutputDir());
		List<TestCaseModification> tcmList = analyzer.parseTestCaseModifications();
		assertEquals(1, tcmList.size());
	}
	
	@Test
	public void testMeasureLcs() throws IOException, ParseException {
		LCSAnalyzer analyzer = new LCSAnalyzer(this.project.getOutputDir());
		analyzer.analyze();
		
	}
	
//	@Test
//	public void testGetDiffFiles() {
//		List<File> files = this.lcsAnalyzer.getDiffFiles();
//		Assert.assertTrue(0 < files.size());
//	}
	
////	@Test
//	public void testParse() {
//		List<File> files = this.lcsAnalyzer.getDiffFiles();
//		Assert.assertTrue(0 < files.size());
//		File file = files.get(0);
//		try {
//			this.lcsAnalyzer.parse(file);
//		} catch (IOException e) {
//			Assert.fail();
//		}
//	}
//	
//	@Test
//	public void testDiff() throws IOException {
//		File dir = new File("output/commons-exec/diff/12b4a201cb887fccb7d396f6ed19566795a60d12");
//		File file = new File(dir, "");
//		DiffResult result = this.lcsAnalyzer.parse(file);
//		for (String clazz : result.getOriginalNodeClasses()) {
//			System.out.println(clazz);
//		}
//	}
	
//	@Test
//	public void testLcs() {
//		
//	}
	
//	@Test
//	public void testAnalyze() throws IOException {
//		this.lcsAnalyzer.analyze();
//		
//		
//	}
}

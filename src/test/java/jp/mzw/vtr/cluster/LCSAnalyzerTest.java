package jp.mzw.vtr.cluster;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LCSAnalyzerTest {

	public static final String PATH_TO_OUTPUT_DIR = "src/test/resources/output";
//	public static final String PATH_TO_OUTPUT_DIR = "output";
	
	protected File outputDir;
	protected LCSAnalyzer lcsAnalyzer;
	
	@Before
	public void setup() {
		this.outputDir = new File(PATH_TO_OUTPUT_DIR);
		this.lcsAnalyzer = new LCSAnalyzer(this.outputDir);
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

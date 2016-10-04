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
	
	protected File outputDir;
	protected LcsAnalyzer lcsAnalyzer;
	
	@Before
	public void setup() {
		this.outputDir = new File(PATH_TO_OUTPUT_DIR);
		this.lcsAnalyzer = new LcsAnalyzer(this.outputDir);
	}
	
	@Test
	public void testConstructor() throws IOException {
		LcsAnalyzer analyzer = new LcsAnalyzer(this.project.getOutputDir());
		List<TestCaseModification> tcmList = analyzer.parseTestCaseModifications();
		assertEquals(37, tcmList.size());
	}
	
	@Test
	public void testMeasureLcs() throws IOException, ParseException {
		LcsAnalyzer analyzer = new LcsAnalyzer(this.project.getOutputDir());
		LcsMap map = analyzer.analyze();
		assertEquals(-1.0, map.getMap()[0][0], 0.01);
		assertNotEquals(-1.0, map.getMap()[0][1], 0.01);
		assertEquals(-1.0, map.getMap()[1][0], 0.01);
		assertEquals(-1.0, map.getMap()[1][1], 0.01);
	}
	
}

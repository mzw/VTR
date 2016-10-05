package jp.mzw.vtr.cluster.visualize;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import jp.mzw.vtr.VtrTestBase;

public class VisualizerBaseTest extends VtrTestBase {

	private File outputDir;
	
	@Before
	public void setup() {
		this.outputDir = this.project.getOutputDir();
	}
	
	@Test
	public void testParseDict() {
		VisualizerBase vb = new VisualizerBase(this.outputDir);
		assertNotNull(vb.getDict("vtr-example"));
	}
	
}

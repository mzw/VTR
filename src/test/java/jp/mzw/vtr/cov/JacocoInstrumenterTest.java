package jp.mzw.vtr.cov;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class JacocoInstrumenterTest {

	public static final String PATH_TO_MVN_PRJ = "src/test/resources/vtr-example";
	
	@Test
	public void testConstructor() throws IOException {
		File dir = new File(PATH_TO_MVN_PRJ);
		JacocoInstrumenter ji = new JacocoInstrumenter(dir);
		Assert.assertNotNull(ji);
	}
	
	@Test
	public void testInstrument() throws IOException {
		File dir = new File(PATH_TO_MVN_PRJ);
		JacocoInstrumenter ji = new JacocoInstrumenter(dir);
		
		String origin = FileUtils.readFileToString(new File(dir, JacocoInstrumenter.FILENAME_POM));
		String modified = FileUtils.readFileToString(new File(dir, JacocoInstrumenter.FILENAME_POM));
		ji.revert();
		String reverted = FileUtils.readFileToString(new File(dir, JacocoInstrumenter.FILENAME_POM));
		
		Assert.assertFalse(origin.equals(modified));
		Assert.assertTrue(origin.equals(reverted));
	}
	
}

package jp.mzw.vtr.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.maven.JacocoInstrumenter;

import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.Assert;
import org.junit.Test;

public class JacocoInstrumenterTest extends VtrTestBase {

	@Test
	public void testConstructor() throws IOException {
		File dir = new File(PATH_TO_PROJECT_DIR);
		JacocoInstrumenter ji = new JacocoInstrumenter(dir);
		Assert.assertNotNull(ji);
	}

	@Test
	public void testInstrument() throws IOException, DocumentException {
		File dir = new File(PATH_TO_PROJECT_DIR);
		JacocoInstrumenter ji = new JacocoInstrumenter(dir);

		String origin = FileUtils.readFileToString(new File(dir, JacocoInstrumenter.FILENAME_POM));
		ji.instrument();
		String modified = FileUtils.readFileToString(new File(dir, JacocoInstrumenter.FILENAME_POM));
		ji.revert();
		String reverted = FileUtils.readFileToString(new File(dir, JacocoInstrumenter.FILENAME_POM));

		Assert.assertFalse(origin.equals(modified));
		Assert.assertTrue(origin.equals(reverted));
	}

	@Test(expected = FileNotFoundException.class)
	public void testPomNotFound() throws IOException {
		File dir = new File("src/test/resouces/output");
		new JacocoInstrumenter(dir);
	}

	@Test
	public void testModifyJunitVersion() throws IOException, DocumentException {
		JacocoInstrumenter ji = new JacocoInstrumenter(new File("src/test/resources/jacoco-test"));
		String modified = ji.modifyJunitVersion(ji.originalPomContent);
		Document modifiedDocument = Jsoup.parse(modified, "", Parser.xmlParser());
		for (Element plugins : modifiedDocument.select("plugins plugin")) {
			Element artifactId = null;
			Element version = null;
			for (Element plugin : plugins.children()) {
				if ("artifactId".equalsIgnoreCase(plugin.tagName())) {
					artifactId = plugin;
				} else if ("version".equalsIgnoreCase(plugin.tagName())) {
					version = plugin;
				}
			}
			if (artifactId != null && "junit".equalsIgnoreCase(artifactId.text())) {
				Assert.assertArrayEquals("4.12".toCharArray(), version.text().toCharArray());
			}
		}
	}

	@Test
	public void testModifySurefireVersion() throws IOException, DocumentException {
		JacocoInstrumenter ji = new JacocoInstrumenter(new File("src/test/resources/jacoco-test"));
		String modified = ji.modifySurefireVersion(ji.originalPomContent);
		Document modifiedDocument = Jsoup.parse(modified, "", Parser.xmlParser());
		for (Element plugins : modifiedDocument.select("plugins plugin")) {
			Element artifactId = null;
			Element version = null;
			for (Element plugin : plugins.children()) {
				if ("artifactId".equalsIgnoreCase(plugin.tagName())) {
					artifactId = plugin;
				} else if ("version".equalsIgnoreCase(plugin.tagName())) {
					version = plugin;
				}
			}
			if (artifactId != null && "maven-surefire-plugin".equalsIgnoreCase(artifactId.text())) {
				Assert.assertArrayEquals("2.18.1".toCharArray(), version.text().toCharArray());
			}
		}
	}
}

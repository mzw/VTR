package jp.mzw.vtr.cov;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import jp.mzw.vtr.core.Config;
import jp.mzw.vtr.maven.MavenUtils;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.dom4j.DocumentException;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JacocoInstrumenterTest {

	public static final String PATH_TO_MVN_PRJ = "src/test/resources/vtr-example";
	
	Config config;
	
	@Before
	public void setup() throws IOException {
		this.config = new Config("config.properties");
	}

	@Test
	public void testConstructor() throws IOException {
		File dir = new File(PATH_TO_MVN_PRJ);
		JacocoInstrumenter ji = new JacocoInstrumenter(dir);
		Assert.assertNotNull(ji);
	}

	@Test
	public void testInstrument() throws IOException, DocumentException {
		File dir = new File(PATH_TO_MVN_PRJ);
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
	public void testParse() throws IOException, MavenInvocationException {
		// Subject
		File subject = new File(PATH_TO_MVN_PRJ);
		File targetClasses = new File(subject, "target/Classes");
		// Coverage results
		File jacocoDir = new File("src/test/resources/output/vtr-example/jacoco");
		File commitDir = new File(jacocoDir, "7fcfdfa99bf9f220b9643f372c36609ca35c60b3");
		File exec = new File(commitDir, "jp.mzw.vtr.example.FileUtilsTest#testGetFiles!jacoco.exec");
		
		MavenUtils.maven(new File(PATH_TO_MVN_PRJ), Arrays.asList("compile"), this.config.getMavenHome());
		CoverageBuilder builder = JacocoInstrumenter.parse(exec, targetClasses);
		MavenUtils.maven(new File(PATH_TO_MVN_PRJ), Arrays.asList("clean"), this.config.getMavenHome());
		
		Assert.assertNotNull(builder);
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

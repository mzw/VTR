package jp.mzw.vtr.cov;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.tools.ExecFileLoader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JacocoInstrumenter {
	static Logger LOGGER = LoggerFactory.getLogger(JacocoInstrumenter.class);

	public static final String FILENAME_POM = "pom.xml";

	protected File pom;
	protected String originalPomContent;
	protected Document document;

	public JacocoInstrumenter(File dir) throws IOException {
		this.pom = new File(dir, FILENAME_POM);
		if (!this.pom.exists()) {
			throw new FileNotFoundException("pom.xml not found");
		}
		this.originalPomContent = FileUtils.readFileToString(pom);
		this.document = Jsoup.parse(this.originalPomContent, "", Parser.xmlParser());
	}

	public void revert() throws IOException {
		FileUtils.writeStringToFile(this.pom, this.originalPomContent);
	}

	public boolean instrument() throws IOException {
		String modified = String.copyValueOf(this.originalPomContent.toCharArray());
		modified = this.modifyJunitVersion(modified);
		modified = this.modifySurefireVersion(modified);
		modified = this.insturumentJacoco(modified);

		if (this.originalPomContent.compareTo(modified) != 0) { // modified
			FileUtils.writeStringToFile(this.pom, modified);
			return true;
		}
		return false;
	}

	/**
	 * Modify JUnit version to 4.12
	 * 
	 * @param content
	 *            POM content under modify
	 * @return Modified POM content
	 */
	protected String modifyJunitVersion(String content) {
		for (Element dependencies : this.document.select("dependencies dependency")) {
			// Detect
			String artifactId = null;
			String version = null;
			for (Element dependency : dependencies.children()) {
				if ("artifactId".equalsIgnoreCase(dependency.tagName())) {
					artifactId = dependency.text();
				} else if ("version".equalsIgnoreCase(dependency.tagName())) {
					version = dependency.text();
				}
			}
			// Modify
			if ("junit".equalsIgnoreCase(artifactId)) {
				if (version == null) {
					content = content.replace("<artifactId>junit</artifactId>", "<artifactId>junit</artifactId><version>4.12</version>");
					LOGGER.info("Add JUnit version");
				} else if (version.startsWith("3")) {
					content = content.replace("<version>" + version + "</version>", "<version>4.12</version>");
					LOGGER.info("Change JUnit version from {} to 4.12", version);
				}
			}
		}
		return content;
	}

	/**
	 * Modify Maven Surefire plugin version to 2.18.1
	 * 
	 * @param content
	 *            POM content under modify
	 * @return modified POM content
	 */
	protected String modifySurefireVersion(String content) {
		for (Element plugins : this.document.select("build plugins plugin")) {
			// Detect
			String artifactId = null;
			String version = null;
			for (Element plugin : plugins.children()) {
				if ("artifactId".equalsIgnoreCase(plugin.tagName())) {
					artifactId = plugin.text();
				} else if ("version".equalsIgnoreCase(plugin.tagName())) {
					version = plugin.text();
				}
			}
			// Modify
			if ("maven-surefire-plugin".equalsIgnoreCase(artifactId)) {
				if (version == null) {
					content = content.replace("<artifactId>maven-surefire-plugin</artifactId>",
							"<artifactId>maven-surefire-plugin</artifactId><version>2.18.1</version>");
				} else if (version.startsWith("3")) {
					content = content.replace("<version>" + version + "</version>", "<version>2.18.1</version>");
				}
			}
		}
		return content;
	}

	/**
	 * Instrument Jacoco plugin to measure code coverage
	 * 
	 * @param content
	 *            POM content under modify
	 * @return modified POM content
	 * @throws IOException
	 */
	protected String insturumentJacoco(String content) throws IOException {
		// Read Jacoco snippet
		InputStream in = JacocoInstrumenter.class.getClassLoader().getResourceAsStream("jacoco.pom.txt");
		String jacoco = IOUtils.toString(in);
		// Modify
		Elements build = this.document.getElementsByTag("build");
		if (build.size() == 0) {
			LOGGER.info("Add Jacoco plugin");
			return content.replace("</project>", "<build><plugins>" + jacoco + "</plugins></build></project>");
		}
		boolean found = false;
		for (Element plugins : this.document.select("build plugins plugin")) {
			String artifactId = null;
			for (Element plugin : plugins.children()) {
				if ("artifactId".equalsIgnoreCase(plugin.tagName())) {
					artifactId = plugin.text();
				}
			}
			if ("jacoco-maven-plugin".equalsIgnoreCase(artifactId)) {
				found = true;
			}
		}
		if (!found) {
			LOGGER.info("Add Jacoco plugin");
			String _build = content.substring(content.indexOf("<build>"));
			String _plugins = _build.substring(0, _build.indexOf("</plugins>") + "</plugins>".length());
			String _modified = _plugins.replace("</plugins>", jacoco + "</plugins>");
			content = content.replace(_plugins, _modified);
		}
		// Return
		return content;
	}
	
	
	/**
	 * Parse coverage results
	 * @param exec
	 * @param targetClasses
	 * @return
	 * @throws IOException
	 */
	public static CoverageBuilder parse(File exec, File targetClasses) throws IOException {
		ExecFileLoader loader = new ExecFileLoader();
		loader.load(exec);
		CoverageBuilder builder = new CoverageBuilder();
		Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), builder);
		analyzer.analyzeAll(targetClasses);
		return builder;
	}
	
	/**
	 * Determine whether given line is covered by test cases
	 * @param status
	 * @return
	 */
	public static boolean isCoveredLine(int status) {
		switch(status) {
		case ICounter.PARTLY_COVERED:
		case ICounter.FULLY_COVERED:
			return true;
		case ICounter.NOT_COVERED:
		case ICounter.EMPTY:
		}
		return false;
	}
	
}

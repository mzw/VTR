package jp.mzw.vtr.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.tools.ExecFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JacocoInstrumenter {
	static Logger LOGGER = LoggerFactory.getLogger(JacocoInstrumenter.class);

	public static final String FILENAME_POM = "pom.xml";

	protected File pom;
	protected String originalPomContent;

	public JacocoInstrumenter(File dir) throws IOException {
		this.pom = new File(dir, FILENAME_POM);
		if (!this.pom.exists()) {
			throw new FileNotFoundException("pom.xml not found");
		}
		this.originalPomContent = FileUtils.readFileToString(pom);
	}

	public void revert() throws IOException {
		FileUtils.writeStringToFile(this.pom, this.originalPomContent);
	}

	public boolean instrument() throws IOException, org.dom4j.DocumentException {
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
	 * @throws DocumentException
	 */
	protected String modifyJunitVersion(String content) throws org.dom4j.DocumentException {
		org.dom4j.Document document = org.dom4j.DocumentHelper.parseText(content);
		org.dom4j.Element root = document.getRootElement();
		List<org.dom4j.Element> elements = new ArrayList<org.dom4j.Element>();
		elements.add(root);
		boolean find = false;
		do {
			find = false;
			List<org.dom4j.Element> _elements = new ArrayList<org.dom4j.Element>();
			for (org.dom4j.Element element : elements) {
				for (Iterator<?> it = element.elementIterator(); it.hasNext();) {
					org.dom4j.Element plugin_candidate = (org.dom4j.Element) it.next();
					if ("dependency".equals(plugin_candidate.getName())) {
						org.dom4j.Element artifactId = null;
						org.dom4j.Element version = null;
						for (Iterator<?> _it = plugin_candidate.elementIterator(); _it.hasNext();) {
							org.dom4j.Element artifact_version_candidate = (org.dom4j.Element) _it.next();
							if ("artifactId".equals(artifact_version_candidate.getName())) {
								if ("junit".equalsIgnoreCase(artifact_version_candidate.getText())) {
									artifactId = artifact_version_candidate;
								}
							} else if ("version".equals(artifact_version_candidate.getName())) {
								version = artifact_version_candidate;
							}
						}
						if (artifactId != null) {
							if (version == null) {
								plugin_candidate.addElement("version").setText("4.12");
								LOGGER.info("Add JUnit version");
							} else if (version.getText().startsWith("3")) {
								version.setText("4.12");
								LOGGER.info("Change JUnit version from {} to 4.12", version);
							}
						}
					} else if (plugin_candidate.asXML().contains("<plugin>")) {
						find = true;
						_elements.add(plugin_candidate);
					}
				}
			}
			elements = _elements;
		} while (find);
		return document.asXML();
	}

	/**
	 * Modify Maven Surefire plugin version to 2.18.1
	 * 
	 * @param content
	 *            POM content under modify
	 * @return modified POM content
	 * @throws IOException
	 * @throws DocumentException
	 */
	protected String modifySurefireVersion(String content) throws IOException, org.dom4j.DocumentException {
		org.dom4j.Document document = org.dom4j.DocumentHelper.parseText(content);
		org.dom4j.Element root = document.getRootElement();
		List<org.dom4j.Element> elements = new ArrayList<org.dom4j.Element>();
		elements.add(root);
		boolean find = false;
		do {
			find = false;
			List<org.dom4j.Element> _elements = new ArrayList<org.dom4j.Element>();
			for (org.dom4j.Element element : elements) {
				for (Iterator<?> it = element.elementIterator(); it.hasNext();) {
					org.dom4j.Element plugin_candidate = (org.dom4j.Element) it.next();
					if ("plugin".equals(plugin_candidate.getName())) {
						org.dom4j.Element artifactId = null;
						org.dom4j.Element version = null;
						for (Iterator<?> _it = plugin_candidate.elementIterator(); _it.hasNext();) {
							org.dom4j.Element artifact_version_candidate = (org.dom4j.Element) _it.next();
							if ("artifactId".equals(artifact_version_candidate.getName())) {
								if ("maven-surefire-plugin".equalsIgnoreCase(artifact_version_candidate.getText())) {
									artifactId = artifact_version_candidate;
								}
							} else if ("version".equals(artifact_version_candidate.getName())) {
								version = artifact_version_candidate;
							}
						}
						if (artifactId != null) {
							if (version == null) {
								plugin_candidate.addElement("version").setText("2.18.1");
							} else if (version.getText().startsWith("3")) {
								version.setText("2.18.1");
							}
						}
					} else if (plugin_candidate.asXML().contains("<plugin>")) {
						find = true;
						_elements.add(plugin_candidate);
					}
				}
			}
			elements = _elements;
		} while (find);
		return document.asXML();
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
		org.jsoup.nodes.Document document = org.jsoup.Jsoup.parse(content, "", org.jsoup.parser.Parser.xmlParser());
		org.jsoup.select.Elements build = document.getElementsByTag("build");
		if (build.size() == 0) {
			LOGGER.info("Add Jacoco plugin");
			return content.replace("</project>", "<build><plugins>" + jacoco + "</plugins></build></project>");
		}
		boolean found = false;
		for (org.jsoup.nodes.Element plugins : document.select("build plugins plugin")) {
			String artifactId = null;
			for (org.jsoup.nodes.Element plugin : plugins.children()) {
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
	 * 
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
	 * 
	 * @param status
	 * @return
	 */
	public static boolean isCoveredLine(int status) {
		switch (status) {
		case ICounter.PARTLY_COVERED:
		case ICounter.FULLY_COVERED:
			return true;
		case ICounter.NOT_COVERED:
		case ICounter.EMPTY:
		}
		return false;
	}

}

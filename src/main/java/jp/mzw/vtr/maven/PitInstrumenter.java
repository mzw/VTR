package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.VtrUtils;

public class PitInstrumenter extends JacocoInstrumenter {
	protected static Logger LOGGER = LoggerFactory.getLogger(PitInstrumenter.class);

	private String sut;
	private String clazz;

	public PitInstrumenter(File dir, String sut, String clazz) throws IOException {
		super(dir);
		this.sut = sut;
		this.clazz = clazz;
	}

	@Override
	public boolean instrument() throws IOException, org.dom4j.DocumentException {
		String modified = String.copyValueOf(this.originalPomContent.toCharArray());
		modified = this.insturumentPit(modified);
		if (this.originalPomContent.compareTo(modified) != 0) { // modified
			FileUtils.writeStringToFile(this.pom, modified);
			return true;
		}
		return false;
	}

	protected String insturumentPit(String content) throws IOException {
		// Read PIT snippet
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("pit.pom.txt");
		String pit = IOUtils.toString(is).replace("SUT-CLASSES", this.sut).replace("TEST-CLASS", this.clazz);
		// Modify
		org.jsoup.nodes.Document document = org.jsoup.Jsoup.parse(content, "", org.jsoup.parser.Parser.xmlParser());
		org.jsoup.select.Elements build = document.getElementsByTag("build");
		if (build.size() == 0) {
			LOGGER.info("Add PIT plugin");
			return content.replace("</project>", "<build><plugins>" + pit + "</plugins></build></project>");
		}
		boolean found = false;
		for (org.jsoup.nodes.Element plugins : document.select("build plugins plugin")) {
			String artifactId = null;
			for (org.jsoup.nodes.Element plugin : plugins.children()) {
				if ("artifactId".equalsIgnoreCase(plugin.tagName())) {
					artifactId = plugin.text();
				}
			}
			if ("pitest-maven".equalsIgnoreCase(artifactId)) {
				found = true;
			}
		}
		if (!found) {
			LOGGER.info("Add PIT plugin");
			String _build = content.substring(content.indexOf("<build>"));
			String _plugins = _build.substring(0, _build.indexOf("</plugins>") + "</plugins>".length());
			String _modified = _plugins.replace("</plugins>", pit + "</plugins>");
			content = content.replace(_plugins, _modified);
		}
		return content;
	}

	public static File[] getPitResultsDir(File projectDir) {
		File targetDir = new File(projectDir, "target");
		File pitDir = new File(targetDir, "pit-reports");
		return pitDir.listFiles();
	}

	public static String getTargetClasses(File projectDir) {
		StringBuilder builder = new StringBuilder();
		String delim = "";
		File root = new File(projectDir, "src/main/java");
		if (!root.exists()) {
			return "jp.mzw.vtr.invalid";
		}
		List<File> files = getJavaFiles(root);
		List<String> classNames = getClassNames(root, files);
		for (String className : classNames) {
			builder.append(delim).append(className);
			delim = "</param><param>";
		}
		return builder.toString();
	}
	
	public static List<String> getClassNames(File root, List<File> files) {
		final List<String> ret = new ArrayList<>();
		for (File file : files) {
			String path = VtrUtils.getFilePath(root, file);
			String className = path.replace(".java", "").replace("/", ".");
			ret.add(className);
		}
		return ret;
	}
	
	public static List<File> getJavaFiles(File root) {
		final List<File> ret = new ArrayList<>();
		for (File child : root.listFiles()) {
			if (child.isFile() && child.getName().endsWith(".java")) {
				ret.add(child);
			} else if (child.isDirectory()) {
				ret.addAll(getJavaFiles(child));
			}
		}
		return ret;
	}
}

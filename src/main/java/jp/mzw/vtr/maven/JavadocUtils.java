package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.VtrUtils;

public class JavadocUtils {
	protected static Logger LOGGER = LoggerFactory.getLogger(JavadocUtils.class);

	/**
	 * 
	 * @param projectDir
	 * @param mavenHome
	 * @param testFile
	 * @param packageName
	 * @return
	 * @throws IOException
	 * @throws MavenInvocationException
	 * @throws InterruptedException
	 */
	public static Map<String, List<JavadocErrorMessage>> getJavadocErrorMessages(File projectDir, File mavenHome)
			throws IOException, MavenInvocationException, InterruptedException {
		final Map<String, List<JavadocErrorMessage>> ret = new HashMap<>();
		String classpath = MavenUtils.getBuildClassPath(projectDir, mavenHome);
		if (classpath == null) {
			LOGGER.warn("Failed to get class-path of dependencies");
			return ret;
		}
		// Run JavaDoc
		String packageName = getJavadocPackageName(projectDir);
		List<String> cmd = Arrays.asList("javadoc", "-sourcepath", "src/main/java/:src/test/java/", "-classpath", classpath, "-subpackages", packageName,
				"--allow-script-in-comments");
		Pair<List<String>, List<String>> results = VtrUtils.exec(projectDir, cmd);
		if (results == null) {
			// No JavaDoc warnings/errors
			return ret;
		}
		// Parse error messages
		Stack<JavadocErrorMessage> stack = new Stack<>();
		for (String line : results.getRight()) {
			if (line.startsWith("src/main/java") || line.startsWith("src/test/java")) {
				String[] split = line.split(":");
				String filepath = split[0].trim();
				int lineno = Integer.parseInt(split[1].trim());
				String type = split[2].trim();
				String description = split[3].trim();
				JavadocErrorMessage message = new JavadocErrorMessage(filepath, lineno, type, description);
				if (5 <= split.length) {
					String tag = split[4].trim();
					message.setTag(tag);
				}
				stack.push(message);
			} else if (line.replaceAll(" ", "").equals("^")) {
				JavadocErrorMessage message = stack.pop();
				int pos = line.length();
				message.setPos(pos);
				stack.push(message);
			}
		}
		// Arrange
		for (JavadocErrorMessage message : stack) {
			String filepath = message.getFilePath();
			List<JavadocErrorMessage> list = ret.get(filepath);
			if (list == null) {
				list = new ArrayList<>();
			}
			list.add(message);
			ret.put(filepath, list);
		}
		return ret;
	}

	/**
	 * 
	 * @param projectDir
	 * @return
	 */
	public static String getJavadocPackageName(File projectDir) {
		StringBuilder builder = new StringBuilder();
		String delim = "";
		File current = new File(projectDir, "src/test/java");
		boolean only = true;
		while (only) {
			only = false;
			File[] children = current.listFiles();
			if (children.length == 1) {
				File child = children[0];
				if (child.isDirectory()) {
					only = true;
					current = child;
					builder.append(delim).append(child.getName());
					delim = ".";
				}
			}
		}
		return builder.toString();
	}

	public static class JavadocErrorMessage {

		private String filepath;
		private int lineno;
		private String type;
		private String description;

		private String tag;
		private int pos;

		private MethodDeclaration method;

		public JavadocErrorMessage(String filepath, int lineno, String type, String description) {
			this.filepath = filepath;
			this.lineno = lineno;
			this.type = type;
			this.description = description;
			this.tag = null;
			this.pos = -1;
		}

		public String getFilePath() {
			return filepath;
		}

		public int getLineno() {
			return lineno;
		}

		public void setPos(int pos) {
			this.pos = pos;
		}

		public int getPos() {
			return pos;
		}

		public String getType() {
			return type;
		}

		public String getDescription() {
			return description;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		public String getTag() {
			return tag;
		}

		public void setMethod(MethodDeclaration method) {
			this.method = method;
		}

		public MethodDeclaration getMethod() {
			return method;
		}

		public String toString() {
			return filepath + ": [" + type + "] " + description + " (" + lineno + ", " + pos + ")";
		}

		public Element toXMLElement() {
			Element element = new Element(Tag.valueOf("Message"), "");
			element.attr("filepath", StringEscapeUtils.escapeXml10(filepath));
			element.attr("type", StringEscapeUtils.escapeXml10(type));
			element.attr("description", StringEscapeUtils.escapeXml10(description));
			element.attr("lineno", Integer.toString(lineno));
			if (tag != null) {
				element.attr("tag", StringEscapeUtils.escapeXml10(tag));
			}
			element.attr("pos", Integer.toString(pos));
			return element;
		}
	}

	public static Document getXMLDocument(Map<String, List<JavadocErrorMessage>> map) throws IOException {
		InputStream is = JavadocUtils.class.getClassLoader().getResourceAsStream("javadoc_error_messages_empty.xml");
		String content = IOUtils.toString(is);
		Document document = Jsoup.parse(content, "", Parser.xmlParser());
		Element root = document.getElementById("root");
		for (String filepath : map.keySet()) {
			Element filepathElement = new Element(Tag.valueOf("Filepath"), "");
			filepathElement.attr("filepath", StringEscapeUtils.escapeXml10(filepath));
			for (JavadocErrorMessage message : map.get(filepath)) {
				Element messageElement = message.toXMLElement();
				filepathElement.appendChild(messageElement);
			}
			root.appendChild(filepathElement);
		}
		return document;
	}

	public static Map<String, List<JavadocErrorMessage>> parse(File file) throws IOException {
		Map<String, List<JavadocErrorMessage>> ret = new HashMap<>();
		String javadocErrorMessagesContent = FileUtils.readFileToString(file);
		Document document = Jsoup.parse(javadocErrorMessagesContent, "", Parser.xmlParser());
		Element root = document.getElementById("root");
		for (Element filepathElement : root.getElementsByTag("Filepath")) {
			String filepath = filepathElement.attr("filepath");
			List<JavadocErrorMessage> messages = new ArrayList<>();
			for (Element messageElement : filepathElement.getElementsByTag("Message")) {
				String type = messageElement.attr("type");
				String description = messageElement.attr("description");
				int lineno = Integer.parseInt(messageElement.attr("lineno"));
				int pos = Integer.parseInt(messageElement.attr("pos"));
				JavadocErrorMessage message = new JavadocErrorMessage(filepath, lineno, type, description);
				String tag = messageElement.attr("tag");
				if (tag != null) {
					message.setTag(tag);
				}
				message.setPos(pos);
				messages.add(message);
			}
			ret.put(filepath, messages);
		}
		return ret;
	}

}

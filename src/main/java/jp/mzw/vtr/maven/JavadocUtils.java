package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.VtrUtils;

public class JavadocUtils {
	protected static Logger LOGGER = LoggerFactory.getLogger(JavadocUtils.class);

	/**
	 * 
	 * @param projectDir
	 * @param mavenHome
	 * @return
	 * @throws MavenInvocationException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static List<String> executeJavadoc(File projectDir, File mavenHome) throws MavenInvocationException, IOException, InterruptedException {
		String classpath = MavenUtils.getBuildClassPath(projectDir, mavenHome);
		if (classpath == null) {
			LOGGER.warn("Failed to '$ mvn dependency:build-classpath'");
			return null;
		}
		// Run JavaDoc
		String packageName = getJavadocPackageName(projectDir);
		List<String> cmd = Arrays.asList("javadoc", "-sourcepath", "src/main/java/:src/test/java/", "-classpath", classpath, "-subpackages", packageName);
		Pair<List<String>, List<String>> results = VtrUtils.exec(projectDir, cmd);
		return results.getRight();
	}

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
	public static Map<String, List<JavadocErrorMessage>> parseJavadocErrorMessages(List<String> results) {
		final Map<String, List<JavadocErrorMessage>> ret = new HashMap<>();
		if (results == null) {
			// No JavaDoc warnings/errors
			return ret;
		}
		// Parse error messages
		Stack<JavadocErrorMessage> stack = new Stack<>();
		for (String line : results) {
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
	}
}

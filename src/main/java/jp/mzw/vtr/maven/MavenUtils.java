package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.VtrUtils;

public class MavenUtils {
	static Logger LOGGER = LoggerFactory.getLogger(MavenUtils.class);

	/**
	 * Invoke Maven command
	 * 
	 * @param subject
	 * @param goals
	 * @param mavenHome
	 * @throws MavenInvocationException
	 */
	public static int maven(File subject, List<String> goals, File mavenHome, final boolean logger) throws MavenInvocationException {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(new File(subject, JacocoInstrumenter.FILENAME_POM));
		request.setGoals(goals);
		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(mavenHome);
		invoker.setOutputHandler(new InvocationOutputHandler() {
			@Override
			public void consumeLine(String line) {
				if (logger) {
					LOGGER.info(line);
				}
			}
		});
		invoker.setErrorHandler(new InvocationOutputHandler() {
			@Override
			public void consumeLine(String line) {
				if (logger) {
					LOGGER.warn(line);
				}
			}
		});
		InvocationResult result = invoker.execute(request);
		return result.getExitCode();
	}

	/**
	 * 
	 * @param subject
	 * @param goals
	 * @param mavenHome
	 * @param stdio
	 * @param stderr
	 * @return
	 * @throws MavenInvocationException
	 */
	public static List<String> maven(File subject, List<String> goals, File mavenHome, final boolean stdio, final boolean stderr)
			throws MavenInvocationException {
		final List<String> ret = new ArrayList<>();
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(new File(subject, JacocoInstrumenter.FILENAME_POM));
		request.setGoals(goals);
		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(mavenHome);
		invoker.setOutputHandler(new InvocationOutputHandler() {
			@Override
			public void consumeLine(String line) {
				if (stdio) {
					ret.add(line);
				}
			}
		});
		invoker.setErrorHandler(new InvocationOutputHandler() {
			@Override
			public void consumeLine(String line) {
				if (stderr) {
					ret.add(line);
				}
			}
		});
		invoker.execute(request);
		return ret;
	}

	/**
	 * 
	 * @param subject
	 * @param goals
	 * @param mavenHome
	 * @return
	 * @throws MavenInvocationException
	 */
	public static Results maven(File subject, List<String> goals, File mavenHome) throws MavenInvocationException {
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(new File(subject, JacocoInstrumenter.FILENAME_POM));
		request.setGoals(goals);
		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(mavenHome);

		final List<String> outputs = new ArrayList<>();
		final List<String> errors = new ArrayList<>();
		invoker.setOutputHandler(new InvocationOutputHandler() {
			@Override
			public void consumeLine(String line) {
				outputs.add(line);
			}
		});
		invoker.setErrorHandler(new InvocationOutputHandler() {
			@Override
			public void consumeLine(String line) {
				errors.add(line);
			}
		});
		invoker.execute(request);
		return Results.of(outputs, errors);
	}

	public static class Results {
		private List<String> compileOutputs;
		private List<String> compileErrors;
		private List<JavadocErrorMessage> javadocErrorMessages;

		private Results(List<String> outputs, List<String> errors) {
			this.compileOutputs = outputs;
			this.compileErrors = errors;
		}
		
		public static Results of(List<String> outputs, List<String> errors) {
			return new Results(outputs, errors);
		}

		public List<String> getCompileOutputs() {
			return compileOutputs;
		}

		public List<String> getCompileErrors() {
			return compileErrors;
		}
		
		public void setJavadocErrorMessages(List<JavadocErrorMessage> javadocErrorMessages) {
			this.javadocErrorMessages = javadocErrorMessages;
		}
		
		public List<JavadocErrorMessage> getJavadocErrorMessages() {
			return javadocErrorMessages;
		}
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public static List<TestSuite> getTestSuites(File subjectDir) throws IOException {
		ArrayList<TestSuite> testSuites = new ArrayList<TestSuite>();
		File testDir = new File(subjectDir, "src/test/java");
		// Determine
		ArrayList<File> mvnTestFileList = new ArrayList<File>();
		for (File file : VtrUtils.getFiles(testDir)) {
			if (Pattern.compile(".*Test(Case)?.*\\.java").matcher(file.getName()).find()) {
				mvnTestFileList.add(file);
			}
		}
		// Return
		for (File testFile : mvnTestFileList) {
			TestSuite testSuite = new TestSuite(testDir, testFile).parseJuitTestCaseList();
			testSuites.add(testSuite);
		}
		return testSuites;
	}

	public static List<TestSuite> getTestSuitesAtLevel2(File subjectDir) throws IOException {
		ArrayList<TestSuite> testSuites = new ArrayList<TestSuite>();
		File testDir = new File(subjectDir, "src/test/java");
		// Determine
		ArrayList<File> mvnTestFileList = new ArrayList<File>();
		for (File file : VtrUtils.getFiles(testDir)) {
			if (Pattern.compile(".*Test(Case)?.*\\.java").matcher(file.getName()).find()) {
				mvnTestFileList.add(file);
			}
		}
		// Return
		Map<String, CompilationUnit> units = TestSuite.getFileUnitMap(subjectDir);
		for (File testFile : mvnTestFileList) {
			TestSuite testSuite = new TestSuite(testDir, testFile);
			CompilationUnit cu = units.get(testFile.getCanonicalPath());
			testSuite.setParseResults(cu);
			testSuites.add(testSuite);
		}
		return testSuites;
	}

	/**
	 * 
	 * @param subjectDir
	 * @param relativePathToSrcFile
	 * @return
	 */
	public static File getSrcFile(File subjectDir, String className) {
		File srcDir = new File(subjectDir, "src/main/java");
		for (File file : VtrUtils.getFiles(srcDir)) {
			URI relative = srcDir.toURI().relativize(file.toURI());
			if (relative.toString().equals(className + ".java")) {
				return file;
			}
		}
		return null;
	}

	/**
	 * Traverse given test suites and find test case whose full name is equals
	 * to given test case
	 * 
	 * @param testSuites
	 * @param testCase
	 * @return
	 */
	public static TestCase getTestCaseInBy(List<TestSuite> testSuites, TestCase testCase) {
		for (TestSuite ts : testSuites) {
			for (TestCase tc : ts.getTestCases()) {
				if (tc.getFullName().equals(testCase.getFullName())) {
					return tc;
				}
			}
		}
		return null;
	}

	public static boolean isMavenTest(File file) {
		if (file == null) {
			return false;
		}
		String filename = file.getName();
		Matcher matcher = Pattern.compile(".*Test(Case)?.*\\.java").matcher(filename);
		if (matcher.find()) {
			return true;
		}
		return false;
	}

	public static boolean isJUnitTest(MethodDeclaration method) {
		for (Object _modifier : method.modifiers()) {
			// For JUnit4
			if (_modifier instanceof MarkerAnnotation) {
				MarkerAnnotation annotation = (MarkerAnnotation) _modifier;
				if ("Test".equals(annotation.getTypeName().getFullyQualifiedName())) {
					boolean hasIgnore = false;
					for (Object __modifier : method.modifiers()) {
						if (__modifier instanceof MarkerAnnotation) {
							MarkerAnnotation _annotation = (MarkerAnnotation) __modifier;
							if ("Ignore".equals(_annotation.getTypeName().getFullyQualifiedName())) {
								hasIgnore = true;
							}
						}
					}
					if (hasIgnore) {
						return false;
					}
					return true;
				}
			}
			// For JUnit3
			else if (_modifier instanceof Modifier) {
				Modifier modifier = (Modifier) _modifier;
				if (modifier.isPublic()) {
					String ret_type_str = method.getReturnType2() != null ? method.getReturnType2().toString() : "";
					if ("void".equals(ret_type_str) && method.getName().getIdentifier().startsWith("test")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Get children from given parent
	 * 
	 * @param node
	 *            Parent
	 * @return Children
	 */
	public static List<ASTNode> getChildren(ASTNode node) {
		List<ASTNode> children = new ArrayList<ASTNode>();
		List<?> list = node.structuralPropertiesForType();
		for (int i = 0; i < list.size(); i++) {
			Object child = node.getStructuralProperty((StructuralPropertyDescriptor) list.get(i));
			if (child instanceof ASTNode) {
				children.add((ASTNode) child);
			} else if (child instanceof List) {
				for (Object _child : ((List<?>) child).toArray()) {
					if (_child instanceof ASTNode) {
						children.add((ASTNode) _child);
					}
				}

			}
		}
		return children;
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
	public static List<JavadocErrorMessage> getJavadocErrorMessages(File projectDir, File mavenHome, File testFile, String packageName) throws IOException, MavenInvocationException, InterruptedException {
		final List<JavadocErrorMessage> ret = new ArrayList<>();
		// Obtain class path of dependencies
		String classpath = null;
		List<String> outputs = MavenUtils.maven(projectDir, Arrays.asList("dependency:build-classpath"), mavenHome, true, false);
		for (String output : outputs) {
			if (!output.startsWith("[")) {
				classpath = output;
				break;
			}
		}
		if (classpath == null) {
			LOGGER.warn("Failed to get class-path of dependencies");
			return ret;
		}
		// Run JavaDoc
		List<String> cmd = Arrays.asList("javadoc", "-sourcepath", "src/main/java/:src/test/java/", "-classpath", classpath, packageName);
		Pair<List<String>, List<String>> results = VtrUtils.exec(projectDir, cmd);
		if (results == null) {
			// No JavaDoc warnings/errors
			return ret;
		}
		List<String> lines = results.getRight();
		if (lines.size() % 3 != 0) {
			LOGGER.warn("Javadoc results might be unexpected: {}", results.getRight());
			return ret;
		}
		String target = VtrUtils.getFilePath(projectDir, testFile);
		for (int i = 0; i < lines.size(); i += 3) {
			String[] split = lines.get(i).split(":");
			String filepath = split[0].trim();
			if (!filepath.equals(target)) {
				continue;
			}
			int lineno = Integer.parseInt(split[1].trim());
			String type = split[2].trim();
			String message = split[3].trim();
			int pos = lines.get(i + 2).length();
			ret.add(new JavadocErrorMessage(filepath, lineno, pos, type, message));
		}
		return ret;
	}

	public static class JavadocErrorMessage {

		private String filepath;
		private int lineno;
		private int pos;
		private String type;
		private String message;
		
		private MethodDeclaration method;

		public JavadocErrorMessage(String filepath, int lineno, int pos, String type, String message) {
			this.filepath = filepath;
			this.lineno = lineno;
			this.pos = pos;
			this.type = type;
			this.message = message;
		}

		public String getFilePath() {
			return filepath;
		}

		public int getLineno() {
			return lineno;
		}

		public int getPos() {
			return pos;
		}

		public String getType() {
			return type;
		}

		public String getMessage() {
			return message;
		}
		
		public void setMethod(MethodDeclaration method) {
			this.method = method;
		}
		
		public MethodDeclaration getMethod() {
			return method;
		}

		public String toString() {
			return filepath + ": [" + type + "] " + message + " (" + lineno + ", " + pos + ")";
		}
	}

}

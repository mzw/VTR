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

import org.apache.commons.io.FileUtils;
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

	public static final String FILENAME_POM = "pom.xml";
	public static final int FAIL_TEST_WITHOUT_MUTATION = 10;

	public static String getPomContent(File projectDir) throws IOException {
		File pom = new File(projectDir, FILENAME_POM);
		if (!pom.exists()) {
			return null;
		}
		return FileUtils.readFileToString(pom);
	}

	/**
	 * Invoke Maven command
	 * 
	 * @param subject
	 * @param goals
	 * @param mavenHome
	 * @throws MavenInvocationException
	 */
	public static int maven(File subject, List<String> goals, File mavenHome, final boolean logger) throws MavenInvocationException {
		// TODO: quick implementation for fse17
		final List<Boolean> failTestWithoutMutation = new ArrayList<>();
		InvocationRequest request = new DefaultInvocationRequest();
		request.setPomFile(new File(subject, FILENAME_POM));
		request.setGoals(goals);
		Invoker invoker = new DefaultInvoker();
		invoker.setMavenHome(mavenHome);
		invoker.setOutputHandler(new InvocationOutputHandler() {
			@Override
			public void consumeLine(String line) {
				if (logger) {
					LOGGER.info(line);
				}
				if (line.contains("did not pass without mutation.")) {
					failTestWithoutMutation.add(true);
				}
			}
		});
		invoker.setErrorHandler(new InvocationOutputHandler() {
			@Override
			public void consumeLine(String line) {
				if (logger) {
					LOGGER.warn(line);
				}
				if (line.contains("did not pass without mutation.")) {
					failTestWithoutMutation.add(true);
				}
			}
		});
		InvocationResult result = invoker.execute(request);
		if (!failTestWithoutMutation.isEmpty()) {
			return FAIL_TEST_WITHOUT_MUTATION;
		}
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
		request.setPomFile(new File(subject, FILENAME_POM));
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
		request.setPomFile(new File(subject, FILENAME_POM));
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

	/**
	 * 
	 * @param projectDir
	 * @param mavenHome
	 * @return
	 * @throws MavenInvocationException
	 */
	public static String getBuildClassPath(File projectDir, File mavenHome) throws MavenInvocationException {
		List<String> outputs = MavenUtils.maven(projectDir, Arrays.asList("dependency:build-classpath"), mavenHome, true, false);
		for (String output : outputs) {
			if (!output.startsWith("[")) {
				return output;
			}
		}
		return null;
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
	 * @return
	 */
	public static String getPackageName(File projectDir) {
		StringBuilder builder = new StringBuilder();
		String delim = "";
		File current = new File(projectDir, "src/main/java");
		if (!current.exists()) {
			return "jp.mzw.vtr.invalid";
		}
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
}

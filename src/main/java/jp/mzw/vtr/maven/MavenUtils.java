package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.Project;
import jp.mzw.vtr.Utils;

public class MavenUtils {
	static Logger log = LoggerFactory.getLogger(MavenUtils.class);
	

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	public static List<TestSuite> getTestSuites(File mvnPrj) throws IOException {
		ArrayList<TestSuite> testSuites = new ArrayList<TestSuite>();
		File testDir = new File(mvnPrj, "src/test/java");
		// Determine
		ArrayList<File> mvnTestFileList = new ArrayList<File>();
		for(File file : Utils.getFiles(testDir)) {
			if(Pattern.compile(".*Test(Case)?.*\\.java").matcher(file.getName()).find()) {
				mvnTestFileList.add(file);
			}
		}
		// Return
		for(File testFile : mvnTestFileList) {
			TestSuite testSuite = new TestSuite(testDir, testFile).parseJuitTestCaseList();
			testSuites.add(testSuite);
		}
		return testSuites;
	}
	
	
	
	

	public static boolean compile(Project project, Properties config) throws IOException, InterruptedException {
		log.info("Maven-compile: " + project.getBaseDir().getAbsolutePath());
		List<String> results = Utils.exec(project.getBaseDir(), Arrays.asList(Utils.getPathToMaven(config), "clean", "compile", "test-compile"));
		for(String result : results) {
			if(result.startsWith("[ERROR]")) {
				return false;
			}
		}
		return true;
	}

	public static List<String> clean(Project project, Properties config) throws IOException, InterruptedException {
		log.info("Maven-clean: " + project.getBaseDir().getAbsolutePath());
		return Utils.exec(project.getBaseDir(), Arrays.asList(Utils.getPathToMaven(config), "clean"));
	}

	public static boolean isMavenTest(File file) {
		if(file == null) return false;
		
		String filename = file.getName();
		
		Matcher matcher = Pattern.compile(".*Test(Case)?.*\\.java").matcher(filename);
		if(matcher.find()) { // && !filename.startsWith("Abstract")) {
			return true;
		}
		
		return false;
	}

	public static boolean isJUnitTest(MethodDeclaration method) {
		for(Object _modifier : method.modifiers()) {
			// For JUnit4
			if(_modifier instanceof MarkerAnnotation) {
				MarkerAnnotation annotation = (MarkerAnnotation)_modifier;
				if("Test".equals(annotation.getTypeName().getFullyQualifiedName())) {
					boolean hasIgnore = false;
					for(Object __modifier : method.modifiers()) {
						if(__modifier instanceof MarkerAnnotation) {
							MarkerAnnotation _annotation = (MarkerAnnotation)__modifier;
							if("Ignore".equals(_annotation.getTypeName().getFullyQualifiedName())) {
								hasIgnore = true;
							}
						}
					}
					if(hasIgnore) {
						return false;
					}
					return true;
				}
			}
			// For JUnit3
			else if(_modifier instanceof Modifier) {
				Modifier modifier = (Modifier) _modifier;
				if(modifier.isPublic()) {
					String ret_type_str = method.getReturnType2() != null ? method.getReturnType2().toString() : "";
					if("void".equals(ret_type_str) && method.getName().getIdentifier().startsWith("test")) {
						return true;
					}
				}
			}
		}
		return false;
	}
}

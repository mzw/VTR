package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import jp.mzw.vtr.Project;
import jp.mzw.vtr.Utils;

import org.eclipse.jgit.revwalk.RevCommit;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.tools.ExecFileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JacocoRunner {
	static Logger log = LoggerFactory.getLogger(JacocoRunner.class);

	Project project;
	Properties config;
	public JacocoRunner(Project project, Properties config) {
		this.project = project;
		this.config = config;
	}

	public List<String> maven(TestCase testCase) throws IOException, InterruptedException {
		String method = testCase.getClassName() + "#" + testCase.getName();
		return Utils.exec(project.getBaseDir(),
				Arrays.asList(Utils.getPathToMaven(config),
						"-Dtest="+method, "jacoco:prepare-agent", "test", "jacoco:report"));
	}

	public CoverageBuilder parse(File exec) throws IOException {
		ExecFileLoader loader = new ExecFileLoader();
		loader.load(exec);

		CoverageBuilder builder = new CoverageBuilder();
		Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), builder);
		analyzer.analyzeAll(project.getDefaultTargetClassesDir());
		
		return builder;
	}

	public static final String JACOCO_EXEC = "jacoco.exec";
	public void copy(File dir, TestCase testCase) throws IOException {
		File src = new File(project.getDefaultTargetDir(), JACOCO_EXEC);
		File dst = new File(dir, copyFileName(testCase.getClassName(), testCase.getName()));
		if(src.exists()) {
			boolean copy = dst.exists() ? dst.delete() : true;
			if(copy) {
				Files.copy(src.toPath(), dst.toPath());
			} else {
				log.error("Cannot copy: " + dst);
			}
		}
	}

	public static String copyFileName(String className, String methodName) {
		return className + "#" + methodName + "!" + JACOCO_EXEC;
	}

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
	

	public static final String JACOCO_EXEC_FILE = "jacoco.exec";
	
	public static String copyFileName(TestCase testCase) {
		return testCase.getClassName() + "#" + testCase.getName() + "!" + JACOCO_EXEC_FILE;
	}
	
//	public static String copyFileNameWithTimestamp(TestCase testCase) {
//		long timestamp = new Date().getTime();
//		return copyFileName(testCase) + "." + timestamp;
//	}
	

	public static File getJacocoLogBaseDir(Project project, Properties config) {
		File projectLogDir = Utils.getLogBaseDir(project, config);
		File jacocoLogDir = new File(projectLogDir, "jacoco");
		if(!jacocoLogDir.exists() && !jacocoLogDir.mkdirs()) {
			log.error("Cannot create directory: " + jacocoLogDir.getAbsolutePath());
		}
		return jacocoLogDir;
	}

	public static File getVisualLogBaseDir(Project project, Properties config) {
		File projectLogDir = Utils.getLogBaseDir(project, config);
		File visualLogDir = new File(projectLogDir, "visual");
		if(!visualLogDir.exists() && !visualLogDir.mkdirs()) {
			log.error("Cannot create directory: " + visualLogDir.getAbsolutePath());
		}
		return visualLogDir;
	}
	
//	public File getLogDir(File logBaseDir, RevCommit commit) {
//		File logDir = new File(logBaseDir, commit.getName());
//		if(!logDir.exists() && !logDir.mkdirs()) {
//			log.error("Cannot make directory: " + logDir);
//		}
//		return logDir;
//	}
}

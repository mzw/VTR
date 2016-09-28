package jp.mzw.vtr.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRunner implements CheckoutConductor.Listener {
	static Logger LOGGER = LoggerFactory.getLogger(TestRunner.class);

	protected String projectId;
	protected File projectDir;

	protected File outputDir;

	protected File mavenHome;

	protected JacocoInstrumenter ji;

	public TestRunner(Project project) throws IOException {
		this.projectId = project.getProjectId();
		this.projectDir = project.getProjectDir();
		this.outputDir = project.getOutputDir();
		this.mavenHome = project.getMavenHome();
		this.ji = new JacocoInstrumenter(this.projectDir);
	}

	/**
	 * Run Maven test cases and measure code coverage
	 * 
	 * @param commit
	 * @throws DocumentException
	 */
	@Override
	public void onCheckout(Commit commit) {
		File dir = this.getOutputDir(commit);
		boolean modified = false;
		try {
			modified = ji.instrument();
			CheckoutConductor.before(this.projectDir, this.mavenHome);
			// Measure coverage
			List<TestSuite> testSuites = MavenUtils.getTestSuites(this.projectDir);
			for (TestSuite ts : testSuites) {
				for (TestCase tc : ts.getTestCases()) {
					File src = new File(this.projectDir, "target/jacoco.exec");
					File dst = new File(dir, tc.getFullName() + "!jacoco.exec");
					if (skip(dst)) {
						continue;
					}
					run(tc);
					copy(src, dst);
					clean(src);
				}
			}
			// Revert
			if (modified) {
				ji.revert();
			}
			CheckoutConductor.after(this.projectDir, this.mavenHome);
		}
		// Not found "pom.xml" meaning not Maven project
		catch (FileNotFoundException e) {
			LOGGER.info("Not Maven project b/c 'pom.xml' was not found");
			return;
		} catch (IOException | MavenInvocationException | DocumentException e) {
			try {
				if (modified) {
					ji.revert();
				}
			} catch (IOException _e) {
				LOGGER.warn("Failed to revert even though test running exception");
			}
		}
	}

	/**
	 * 
	 * @param commit
	 * @return
	 */
	protected File getOutputDir(Commit commit) {
		File subjectDir = new File(this.outputDir, this.projectId);
		File jacocoDir = new File(subjectDir, "jacoco");
		File commitDir = new File(jacocoDir, commit.getId());
		if (!commitDir.exists()) {
			commitDir.mkdirs();
		}
		return commitDir;
	}

	/**
	 * Determine skip to measure coverage if already done
	 * 
	 * @param dst
	 * @return
	 */
	protected boolean skip(File dst) {
		if (dst.exists()) {
			LOGGER.info("Skip to measure coverage b/c already done: {}", dst.getPath());
			return true;
		}
		return false;
	}

	/**
	 * Copy measured coverage data to output directory
	 * 
	 * @param src
	 * @param dst
	 * @throws IOException
	 */
	protected void copy(File src, File dst) throws IOException {
		if (src.exists()) {
			LOGGER.info("Found coverage results: {}", src.getAbsolutePath());
			boolean copy = dst.exists() ? dst.delete() : true;
			if (copy) {
				Files.copy(src.toPath(), dst.toPath());
			} else {
				LOGGER.error("Cannot copy: " + dst);
			}
		} else {
			LOGGER.warn("Failed to measure coverage: {}", src.getAbsolutePath());
		}
	}

	/**
	 * Run test case to measure its coverage
	 * @param testCase
	 * @throws MavenInvocationException
	 */
	protected void run(TestCase testCase) throws MavenInvocationException {
		LOGGER.info("Measure coverage: {}", testCase.getFullName());
		String each = "-Dtest=" + testCase.getFullName();
		List<String> args = Arrays.asList(each, "org.jacoco:jacoco-maven-plugin:prepare-agent", "test", "org.jacoco:jacoco-maven-plugin:report");
		MavenUtils.maven(this.projectDir, args, this.mavenHome);
	}

	/**
	 * Clean measured coverage data
	 * @param src
	 * @throws MavenInvocationException
	 */
	protected void clean(File src) throws MavenInvocationException {
		if (src.exists()) {
			src.delete();
		}
	}
}

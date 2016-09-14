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

	protected Project project;
	
	protected String projectId;
	protected File projectDir;
	
	protected File outputDir;
	
	protected File mavenHome;

	protected JacocoInstrumenter ji;

	public TestRunner(Project project) throws IOException {
		this.project = project;
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
		boolean modified = false;
		try {
			modified = ji.instrument();
			if (modified) {
				// Create directories
				File subjectDir = new File(this.outputDir, this.projectId);
				if (!subjectDir.exists()) {
					subjectDir.mkdirs();
				}
				File jacocoDir = new File(subjectDir, "jacoco");
				if (!jacocoDir.exists()) {
					jacocoDir.mkdirs();
				}
				File commitDir = new File(jacocoDir, commit.getId());
				if (!commitDir.exists()) {
					commitDir.mkdirs();
				}
				// Measure coverage
				List<TestSuite> testSuites = MavenUtils.getTestSuites(this.projectDir);
				for (TestSuite ts : testSuites) {
					for (TestCase tc : ts.getTestCases()) {
						// Skip if coverage is already measured
						String method = tc.getClassName() + "#" + tc.getName();
						File dst = new File(commitDir, method + "!jacoco.exec");
						if (dst.exists()) {
							LOGGER.info("Skip to measure coverage: {}", tc.getFullName());
							continue;
						}
						LOGGER.info("Measure coverage: {}", tc.getFullName());
						// Compile
						MavenUtils.maven(this.projectDir, Arrays.asList("clean", "compile", "test-compile"), this.mavenHome);
						// Run
						MavenUtils.maven(this.projectDir, Arrays.asList("-Dtest=" + method, "org.jacoco:jacoco-maven-plugin:prepare-agent", "test",
								"org.jacoco:jacoco-maven-plugin:report"), this.mavenHome);
						// Copy
						File src = new File(this.projectDir, "target/jacoco.exec");
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
				}
			}
			ji.revert();
		}
		// Not found "pom.xml" meaning not Maven project
		catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (IOException | MavenInvocationException | DocumentException e) {
			e.printStackTrace();
			try {
				if (modified) {
					ji.revert();
				}
			} catch (IOException _e) {
				e.printStackTrace();
				LOGGER.warn("Failed to revert even though test running exception");
			}
		}
	}

}

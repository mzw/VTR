package jp.mzw.vtr.cov;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import jp.mzw.vtr.core.Config;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRunner implements CheckoutConductor.Listener {
	static Logger LOGGER = LoggerFactory.getLogger(TestRunner.class);

	protected String subjectId;
	protected File subject;
	protected Config config;

	protected JacocoInstrumenter ji;

	public TestRunner(String subjectId, File subject, Config config) throws IOException {
		this.subjectId = subjectId;
		this.subject = subject;
		this.config = config;
		this.ji = new JacocoInstrumenter(this.subject);
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
				File subjectDir = new File(this.config.getOutputDir(), this.subjectId);
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
				List<TestSuite> testSuites = MavenUtils.getTestSuites(this.subject);
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
						MavenUtils.maven(this.subject, Arrays.asList("clean", "compile", "test-compile"), this.config.getMavenHome());
						// Run
						MavenUtils.maven(this.subject, Arrays.asList("-Dtest=" + method, "org.jacoco:jacoco-maven-plugin:prepare-agent", "test",
								"org.jacoco:jacoco-maven-plugin:report"), this.config.getMavenHome());
						// Copy
						File src = new File(this.subject, "target/jacoco.exec");
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

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

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRunner implements CheckoutConductor.Listener {
	static Logger LOGGER = LoggerFactory.getLogger(TestRunner.class);

	protected String subjectId;
	protected File mvnPrj;
	protected Config config;

	protected JacocoInstrumenter ji;

	public TestRunner(String subjectId, File mvnPrj, Config config) throws IOException {
		this.subjectId = subjectId;
		this.mvnPrj = mvnPrj;
		this.config = config;
		this.ji = new JacocoInstrumenter(this.mvnPrj);
	}

	/**
	 * Run Maven test cases and measure code coverage
	 * @param commit
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
				List<TestSuite> testSuites = MavenUtils.getTestSuites(this.mvnPrj);
				for (TestSuite ts : testSuites) {
					for (TestCase tc : ts.getTestCases()) {
						// Skip if coverage is already measured
						String method = tc.getClassName() + "#" + tc.getName();
						File dst = new File(commitDir, method + "!jacoco.exec");
						if (dst.exists()) {
							continue;
						}
						// Compile
						InvocationRequest request = new DefaultInvocationRequest();
						request.setPomFile(new File(this.mvnPrj, JacocoInstrumenter.FILENAME_POM));
						request.setGoals(Arrays.asList("clean", "compile", "test-compile"));
						Invoker invoker = new DefaultInvoker();
						invoker.setMavenHome(this.config.getMavenHome());
						invoker.execute(request);
						// Run
						request.setGoals(Arrays.asList("-Dtest=" + method, "jacoco:prepare-agent", "test", "jacoco:report"));
						invoker = new DefaultInvoker();
						invoker.setMavenHome(this.config.getMavenHome());
						invoker.execute(request);
						// Copy
						File src = new File(this.mvnPrj, "target/jacoco.exec");
						if (src.exists()) {
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

				ji.revert();
			}
		}
		// Not found "pom.xml" meaning not Maven project
		catch (FileNotFoundException e) {
			return;
		} catch (IOException | MavenInvocationException e) {
			try {
				if (modified) {
					ji.revert();
				}
			} catch (IOException _e) {
				LOGGER.warn("Failed to revert even though test running exception");
			}
		}
	}

}

package jp.mzw.vtr.validate;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.JavadocUtils;
import jp.mzw.vtr.maven.JavadocUtils.JavadocErrorMessage;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

public class Validator implements CheckoutConductor.Listener {
	protected Logger LOGGER = LoggerFactory.getLogger(Validator.class);

	public interface Listener {
		public void onCheckout(Commit commit, TestCase testcase, Results results);
	}

	private Set<Listener> listeners = new CopyOnWriteArraySet<>();

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	private void notify(Commit commit, TestCase testcase, Results results) {
		for (Listener listener : listeners) {
			listener.onCheckout(commit, testcase, results);
		}
	}

	protected File projectDir;
	protected String projectId;
	protected File outputDir;
	protected File mavenHome;

	public Validator(Project project) {
		this.projectDir = project.getProjectDir();
		this.projectId = project.getProjectId();
		this.outputDir = project.getOutputDir();
		this.mavenHome = project.getMavenHome();
	}

	@Override
	public void onCheckout(Commit commit) {
		try {
			LOGGER.info("Parsing testcases...");
			List<TestSuite> testSuites = MavenUtils.getTestSuitesAtLevel2(projectDir);
			if (testSuites.isEmpty()) {
				LOGGER.info("Not found test suites");
				return;
			}
			// Get compile results
			Results results = null;
			if (Results.is(outputDir, projectId, commit)) {
				LOGGER.info("Parsing existing compile/javadoc results...");
				results = Results.parse(outputDir, projectId, commit);
			} else {
				LOGGER.info("Getting compile results...");
				results = MavenUtils.maven(projectDir,
						Arrays.asList("test-compile", "-Dmaven.compiler.showDeprecation=true", "-Dmaven.compiler.showWarnings=true"), mavenHome);
				LOGGER.info("Getting javadoc results...");
				Map<String, List<JavadocErrorMessage>> javadocErrorMessages = JavadocUtils.getJavadocErrorMessages(projectDir, mavenHome);
				results.setJavadocErrorMessages(javadocErrorMessages);
			}
			// Validate
			LOGGER.info("Validating...");
			for (TestSuite ts : testSuites) {
				for (TestCase tc : ts.getTestCases()) {
					notify(commit, tc, results);
				}
			}
			// Output compile and JavaDoc results
			if (!Results.is(outputDir, projectId, commit)) {
				results.output(outputDir, projectId, commit);
			}
		} catch (IOException | MavenInvocationException | InterruptedException e) {
			LOGGER.warn("Failed to checkout: {}", commit.getId());
		}
	}
}

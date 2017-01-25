package jp.mzw.vtr.validate;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.MavenUtils.JavadocErrorMessage;
import jp.mzw.vtr.maven.MavenUtils.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

public class Validator implements CheckoutConductor.Listener {
	protected Logger LOGGER = LoggerFactory.getLogger(Validator.class);

	public interface Listener {
		public void onCheckout(Commit commit, TestCase testcase, Results results);
	}

	private Set<Listener> listeners = new CopyOnWriteArraySet<>();

	public void addListener(Listener listener) {
		LOGGER.info("Add listener: {}", listener.getClass());
		listeners.add(listener);
	}

	private void notify(Commit commit, TestCase testcase, Results results) {
		for (Listener listener : listeners) {
			LOGGER.info("Notify listener: {}", listener.getClass());
			listener.onCheckout(commit, testcase, results);
		}
	}

	protected File projectDir;
	protected File mavenHome;

	public Validator(Project project) {
		this.projectDir = project.getProjectDir();
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
			LOGGER.info("Getting compile results..."); // TODO: incremental build
			Results results = MavenUtils.maven(projectDir,
					Arrays.asList("clean", "test-compile", "-Dmaven.compiler.showDeprecation=true", "-Dmaven.compiler.showWarnings=true"), mavenHome);
			for (TestSuite ts : testSuites) {
				List<JavadocErrorMessage> javadocErrorMessages = MavenUtils.getJavadocErrorMessages(projectDir, mavenHome, ts.getTestFile(), ts.getPackageName());
				results.setJavadocErrorMessages(javadocErrorMessages);
				for (TestCase tc : ts.getTestCases()) {
					LOGGER.info("Notify Validator.listers at {}", tc.getFullName());
					notify(commit, tc, results);
				}
			}
		} catch (IOException | MavenInvocationException | InterruptedException e) {
			LOGGER.warn("Failed to checkout: {}", commit.getId());
		}
	}
}

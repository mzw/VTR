package jp.mzw.vtr.validate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.JavadocUtils;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

public class Validator implements CheckoutConductor.Listener {
	protected Logger LOGGER = LoggerFactory.getLogger(Validator.class);

	protected File projectDir;
	protected String projectId;
	protected File outputDir;
	protected File mavenHome;
	
	protected List<ValidatorBase> validators;
	
	public Validator(Project project) {
		this.projectDir = project.getProjectDir();
		this.projectId = project.getProjectId();
		this.outputDir = project.getOutputDir();
		this.mavenHome = project.getMavenHome();
	}
	
	public void addValidator(ValidatorBase task) {
		if (validators == null) {
			validators = new ArrayList<>();
		}
		validators.add(task);
	}
	
	protected Results getResults(Commit commit) throws IOException, MavenInvocationException, InterruptedException {
		Results results = null;
		if (Results.is(outputDir, projectId, commit)) {
			LOGGER.info("Parsing existing compile/javadoc results...");
			results = Results.parse(outputDir, projectId, commit);
		} else {
			LOGGER.info("Getting compile results...");
			results = MavenUtils.maven(projectDir,
					Arrays.asList("test-compile", "-Dmaven.compiler.showDeprecation=true", "-Dmaven.compiler.showWarnings=true"), mavenHome);
			LOGGER.info("Getting javadoc results...");
			List<String> javadocResults = JavadocUtils.executeJavadoc(projectDir, mavenHome);
			results.setJavadocResults(javadocResults);
		}
		return results;
	}

	@Override
	public void onCheckout(final Commit commit) {
		try {
			LOGGER.info("Parsing testcases...");
			List<TestSuite> testSuites = MavenUtils.getTestSuitesAtLevel2(projectDir);
			if (testSuites.isEmpty()) {
				LOGGER.info("Not found test suites");
				return;
			}
			// Get compile and JavaDoc results
			final Results results = getResults(commit);
			// Validate
			LOGGER.info("Validating...");
			for (final TestSuite ts : testSuites) {
				for (final TestCase tc : ts.getTestCases()) {
					ExecutorService executor = Executors.newFixedThreadPool(validators.size());
					for (final ValidatorBase validator : validators) {
						executor.submit(new Callable<Long>() {
							@Override
							public Long call() throws Exception {
								validator.validate(commit, tc, results);
								return System.currentTimeMillis();
							}
						});
					}
					executor.shutdown();
					try {
						executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
					} catch (InterruptedException e) {
						executor.shutdownNow();
					}
					executor.shutdownNow();
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

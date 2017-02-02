package jp.mzw.vtr.validate;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

	protected final Project project;

	public static final int NUMBER_OF_THREADS = 500;
	protected ExecutorService executor;

	protected List<Class<? extends ValidatorBase>> validatorClasses;
	protected final List<ValidatorBase> validators;

	public Validator(Project project) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, IOException {
		this.project = project;
		validators = new ArrayList<>();
		validatorClasses = ValidatorBase.getValidatorClasses(project, ValidatorBase.VALIDATORS_LIST);
	}

	public void startup() {
		executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
	}

	public void shutdown() throws IOException, InterruptedException {
		executor.shutdown();
		if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
			executor.shutdownNow();
		}
	}

	public List<ValidatorBase> getValidators() {
		return validators;
	}

	protected Results getResults(Commit commit) throws IOException, MavenInvocationException, InterruptedException {
		Results results = null;
		if (Results.is(project.getOutputDir(), project.getProjectId(), commit)) {
			LOGGER.info("Parsing existing compile/javadoc results...");
			results = Results.parse(project.getOutputDir(), project.getProjectId(), commit);
		} else {
			LOGGER.info("Getting compile results...");
			results = MavenUtils.maven(project.getProjectDir(),
					Arrays.asList("test-compile", "-Dmaven.compiler.showDeprecation=true", "-Dmaven.compiler.showWarnings=true"), project.getMavenHome());
			LOGGER.info("Getting javadoc results...");
			List<String> javadocResults = JavadocUtils.executeJavadoc(project.getProjectDir(), project.getMavenHome());
			results.setJavadocResults(javadocResults);
		}
		return results;
	}

	@Override
	public void onCheckout(final Commit commit) {
		try {
			LOGGER.info("Parsing testcases...");
			List<TestSuite> testSuites = MavenUtils.getTestSuitesAtLevel2(project.getProjectDir());
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
					for (final Class<? extends ValidatorBase> clazz : validatorClasses) {
						executor.submit(new Callable<Long>() {
							@Override
							public Long call() throws Exception {
								Constructor<?> constructor = clazz.getConstructor(Project.class);
								ValidatorBase clone = (ValidatorBase) constructor.newInstance(project);
								clone.validate(commit, tc, results);
								if (clone.hasValidationResults()) {
									validators.add(clone);
								}
								return System.currentTimeMillis();
							}
						});
						shutdown();
						startup();
					}
				}
			}
			// Output compile and JavaDoc results
			if (!Results.is(project.getOutputDir(), project.getProjectId(), commit)) {
				results.output(project.getOutputDir(), project.getProjectId(), commit);
			}
		} catch (IOException | MavenInvocationException | InterruptedException e) {
			LOGGER.warn("Failed to checkout: {}", commit.getId());
		}
	}
}

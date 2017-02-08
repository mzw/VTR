package jp.mzw.vtr.validate;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jp.mzw.vtr.maven.*;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;

public class Validator implements CheckoutConductor.Listener {
	protected Logger LOGGER = LoggerFactory.getLogger(Validator.class);

	protected final Project project;

	public static final int NUMBER_OF_THREADS = 4;
	protected ExecutorService executor;

	protected List<Class<? extends ValidatorBase>> validatorClasses;
	protected final List<ValidationResult> validationResults;

	protected List<TestSuite> prvTestSuites;
	protected List<TestSuite> curTestSuites;
	protected String prvPomContent;
	protected String curPomContent;
	protected final Map<String, List<String>> duplicateMap;

	private CompilerPlugin cp;

	public Validator(Project project) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, IOException {
		this.project = project;
		validatorClasses = ValidatorBase.getValidatorClasses(project, ValidatorBase.VALIDATORS_LIST);
		validationResults = new ArrayList<>();
		prvTestSuites = null;
		curTestSuites = null;
		prvPomContent = null;
		curPomContent = null;
		duplicateMap = new HashMap<>();
		for (Class<? extends ValidatorBase> clazz : validatorClasses) {
			duplicateMap.put(clazz.getName(), new ArrayList<String>());
		}
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

	public List<ValidationResult> getValidationResults() {
		return validationResults;
	}

	protected Results getResults(Commit commit) throws IOException, MavenInvocationException, InterruptedException {
		Results results = null;
		if (Results.is(project.getOutputDir(), project.getProjectId(), commit)) {
			LOGGER.info("Parsing existing compile/javadoc results...");
			results = Results.parse(project.getOutputDir(), project.getProjectId(), commit);
		} else {
			LOGGER.info("Getting compile results...");
			cp = new CompilerPlugin(project.getProjectDir());
			boolean modified = false;
			try {
				modified = cp.instrument();
				results = MavenUtils.maven(project.getProjectDir(),
						Arrays.asList("test-compile"), project.getMavenHome());
				LOGGER.info("Getting javadoc results...");
				List<String> javadocResults = JavadocUtils.executeJavadoc(project.getProjectDir(), project.getMavenHome());
				results.setJavadocResults(javadocResults);
				// Revert
				if (modified) {
					cp.revert();
				}
			} catch (IOException | org.dom4j.DocumentException e) {
				LOGGER.warn("Failed to modify compiler-plugin: {}", commit.getId());
			}
		}
		return results;
	}

	@Override
	public void onCheckout(final Commit commit) {
		try {
			LOGGER.info("Parsing testcases...");
			curTestSuites = MavenUtils.getTestSuitesAtLevel2(project.getProjectDir());
			if (curTestSuites.isEmpty()) {
				LOGGER.info("Not found test suites");
				return;
			}
			curPomContent = MavenUtils.getPomContent(project.getProjectDir());
			// determine whether POM content is updated
			boolean updatePomContent = changePomContent(prvPomContent, curPomContent);
			// Get compile and JavaDoc results
			final Results results = getResults(commit);
			// Validate
			LOGGER.info("Validating...");
			startup();
			for (final TestSuite ts : curTestSuites) {
				for (final TestCase tc : ts.getTestCases()) {
					// determine whether this test case is changed
					boolean changeTestCase = tc.changed(TestSuite.getTestCaseWithClassMethodName(prvTestSuites, tc));
					// skip or not
					if (!updatePomContent && !changeTestCase) {
						continue;
					}
					// validate
					for (final Class<? extends ValidatorBase> clazz : validatorClasses) {
						final List<String> duplicates = duplicateMap.get(clazz.getName());
						if (duplicates.contains(tc.getFullName())) {
							continue;
						}
						executor.submit(new Callable<Long>() {
							@Override
							public Long call() throws Exception {
								Constructor<?> constructor = clazz.getConstructor(Project.class);
								ValidatorBase clone = (ValidatorBase) constructor.newInstance(project);
								ValidationResult result = clone.validate(commit, tc, results);
								if (result != null) {
									validationResults.add(result);
									duplicates.add(tc.getFullName());
									duplicateMap.put(clazz.getName(), duplicates);
								}
								return System.currentTimeMillis();
							}
						});
					}
				}
			}
			shutdown();
			// Output compile and JavaDoc results
			if (!Results.is(project.getOutputDir(), project.getProjectId(), commit)) {
				results.output(project.getOutputDir(), project.getProjectId(), commit);
			}
			prvTestSuites = curTestSuites;
			prvPomContent = curPomContent;
		} catch (IOException | MavenInvocationException | InterruptedException e) {
			LOGGER.warn("Failed to checkout: {}", commit.getId());
		}
	}

	public static boolean changePomContent(String prv, String cur) {
		if (prv == null || cur == null) {
			return true;
		}
		// parse
		Document prvDocument = Jsoup.parse(prv, "", Parser.xmlParser());
		Document curDocument = Jsoup.parse(cur, "", Parser.xmlParser());
		if (prvDocument == null || curDocument == null) {
			return true;
		}
		// junit
		String prvJunitVersion = getJunitVersion(prvDocument);
		String curJunitVersion = getJunitVersion(curDocument);
		if (prvJunitVersion == null || curJunitVersion == null) {
			return true;
		}
		// java
		String prvJavaVersion = getJavaVersion(prvDocument);
		String curJavaVersion = getJavaVersion(curDocument);
		if (prvJavaVersion == null || curJavaVersion == null) {
			return true;
		}
		// compare
		if (prvJunitVersion.equals(curJunitVersion) && prvJavaVersion.equals(curJavaVersion)) {
			return false;
		} else {
			return true;
		}
	}

	public static String getJunitVersion(Document document) {
		for (Element dependency : document.select("dependencies dependency")) {
			boolean junit = false;
			String version = null;
			for (Element child : dependency.children()) {
				if ("artifactId".equalsIgnoreCase(child.tagName()) && "junit".equalsIgnoreCase(child.text())) {
					junit = true;
				} else if ("version".equalsIgnoreCase(child.tagName())) {
					version = child.text();
				}
			}
			if (junit) {
				return version;
			}
		}
		return null;
	}

	public static String getJavaVersion(Document document) {
		{
			Elements elements = document.getElementsByTag("maven.compile.target");
			if (elements != null) {
				if (!elements.isEmpty()) {
					return elements.get(0).text();
				}
			}
		}
		{
			Elements elements = document.getElementsByTag("maven.compiler.target");
			if (elements != null) {
				if (!elements.isEmpty()) {
					return elements.get(0).text();
				}
			}
		}
		{
			for (Element plugin : document.select("plugins plugin")) {
				boolean compiler = false;
				String version = null;
				for (Element child : plugin.children()) {
					if ("artifactId".equalsIgnoreCase(child.tagName()) && "maven-compiler-plugin".equalsIgnoreCase(child.text())) {
						compiler = true;
					} else if ("version".equalsIgnoreCase(child.tagName())) {
						version = child.text();
					}
				}
				if (compiler) {
					return version;
				}
			}
		}
		return null;
	}
}

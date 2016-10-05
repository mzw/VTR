package jp.mzw.vtr.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.dict.Dictionary;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.GitUtils;
import jp.mzw.vtr.git.Tag;

import org.apache.maven.shared.invoker.MavenInvocationException;
import org.dom4j.DocumentException;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRunner implements CheckoutConductor.Listener {
	static Logger LOGGER = LoggerFactory.getLogger(TestRunner.class);

	private String projectId;
	private File projectDir;

	private File outputDir;

	private File mavenHome;

	private JacocoInstrumenter ji;
	private Dictionary dict;
	private Git git;

	public TestRunner(Project project) throws IOException, ParseException {
		this.projectId = project.getProjectId();
		this.projectDir = project.getProjectDir();
		this.outputDir = project.getOutputDir();
		this.mavenHome = project.getMavenHome();
		this.ji = new JacocoInstrumenter(this.projectDir);
		this.dict = new Dictionary(this.outputDir, this.projectId).parse();
		this.git = GitUtils.getGit(this.projectDir);
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
			int before = CheckoutConductor.before(this.projectDir, this.mavenHome);
			if (before == 0) { // succeeded to compile
				// Measure coverage
				List<TestSuite> testSuites = MavenUtils.getTestSuites(this.projectDir);
				BlameCommand blame = new BlameCommand(this.git.getRepository());
				for (TestSuite ts : testSuites) {
					for (TestCase tc : ts.getTestCases()) {
						File src = new File(this.projectDir, "target/jacoco.exec");
						File dst = new File(dir, tc.getFullName() + "!jacoco.exec");
						if (already(dst)) {
							continue;
						}
						if (modified(commit, blame, tc)) {
							run(tc);
							copy(src, dst);
							clean(src);
						}
					}
				}
				// Revert
				if (modified) {
					ji.revert();
				}
			} else {
				LOGGER.warn("Failed to compile subject: {}", commit.getId());
			}
			CheckoutConductor.after(this.projectDir, this.mavenHome);
		}
		// Not found "pom.xml" meaning not Maven project
		catch (FileNotFoundException e) {
			LOGGER.info("Not Maven project because 'pom.xml' was not found");
			LOGGER.debug(e.getMessage());
			return;
		} catch (IOException | MavenInvocationException | DocumentException | GitAPIException e) {
			LOGGER.debug(e.getMessage());
			try {
				if (modified) {
					ji.revert();
				}
			} catch (IOException _e) {
				LOGGER.warn("Failed to revert even though exception while running test cases");
				LOGGER.debug(e.getMessage());
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
	protected boolean already(File dst) {
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
	 * 
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
	 * 
	 * @param src
	 * @throws MavenInvocationException
	 */
	protected void clean(File src) throws MavenInvocationException {
		if (src.exists()) {
			src.delete();
		}
	}

	/**
	 * 
	 * @param commit
	 * @param testSuites
	 * @throws IOException
	 * @throws GitAPIException
	 */
	protected boolean modified(Commit commit, BlameCommand blame, TestCase testCase) throws IOException, GitAPIException {
		// Blame
		String filePath = VtrUtils.getFilePath(this.projectDir, testCase.getTestFile());
		BlameResult result = blame.setFilePath(filePath).call();
		// Determine
		Tag curTag = this.dict.getTagBy(commit);
		for (int lineno = testCase.getStartLineNumber(); lineno <= testCase.getEndLineNumber(); lineno++) {
			Tag tag = this.dict.getTagBy(new Commit(result.getSourceCommit(lineno - 1)));
			if (curTag.getDate().equals(tag.getDate())) {
				LOGGER.info("Detect test modification: {}", testCase.getFullName());
				return true;
			}
		}
		LOGGER.info("Not modified: {}", testCase.getFullName());
		return false;
	}
}

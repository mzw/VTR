package jp.mzw.vtr.detect;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.dict.DictionaryBase;
import jp.mzw.vtr.dict.DictionaryParser;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.GitUtils;
import jp.mzw.vtr.git.Tag;
import jp.mzw.vtr.maven.JacocoInstrumenter;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

public class Detector implements CheckoutConductor.Listener {
	static Logger LOGGER = LoggerFactory.getLogger(Detector.class);

	public static final String DETECT_DIR = "detect";

	protected String projectId;
	protected String pathToProjectDir;
	protected File projectDir;

	protected File outputDir;
	protected File mavenHome;

	protected Git git;
	protected Map<Tag, List<Commit>> dict;

	public Detector(Project project) throws IOException, ParseException {
		this.projectId = project.getProjectId();
		this.pathToProjectDir = project.getPathToProject();
		this.projectDir = project.getProjectDir();
		this.outputDir = project.getOutputDir();
		this.mavenHome = project.getMavenHome();
		this.git = GitUtils.getGit(this.pathToProjectDir);
		this.dict = DictionaryParser.parseDictionary(new File(this.outputDir, this.projectId));
	}

	@Override
	public void onCheckout(Commit commit) {
		try {
			CheckoutConductor.before(projectDir, mavenHome);
			List<TestSuite> ts = setCoverageResults(commit);
			List<TestCaseModification> tcmList = detect(commit, ts);
			output(commit, tcmList);
			CheckoutConductor.after(projectDir, mavenHome);
		} catch (IOException | GitAPIException e) {
			LOGGER.warn(e.toString());
		}
	}

	/**
	 * Output XML file containing test case modifications for
	 * previously-released source programs
	 * 
	 * @param commit
	 * @param tcmList
	 * @throws IOException
	 */
	private void output(Commit commit, List<TestCaseModification> tcmList) throws IOException {
		// No subject test-case modification
		if (tcmList.size() < 1) {
			return;
		}
		// Output
		String xml = this.getXml(tcmList);
		if (xml != null) {
			File file = this.getOutputFile(commit);
			FileUtils.writeStringToFile(file, xml);
		}
	}
	
	/**
	 * 
	 * @param commit
	 * @return
	 */
	protected File getOutputFile(Commit commit) {
		File outputSubjectDir = new File(this.outputDir, this.projectId);
		File outputDetectDir = new File(outputSubjectDir, DETECT_DIR);
		if (!outputDetectDir.exists()) {
			outputDetectDir.mkdirs();
		}
		return new File(outputDetectDir, commit.getId() + ".xml");
	}
	
	/**
	 * 
	 * @param tcmList
	 * @return
	 */
	protected String getXml(List<TestCaseModification> tcmList) {
		boolean output = false;
		Document document = DocumentHelper.createDocument();
		Element root = document.addElement("TestCaseModifications");
		for (TestCaseModification tcm : tcmList) {
			TestCase tc = tcm.getTestCase();
			if (!hasCoveredLines(tc)) {
				continue;
			} else {
				output = true;
			}
			Element tcmElement = root.addElement("TestCaseModification");
			tcmElement.addAttribute("class", tc.getClassName()).addAttribute("method", tc.getName());
			Map<File, List<Integer>> covered = tc.getCoveredClassLinesMap();
			for (File src : covered.keySet()) {
				List<Integer> lines = covered.get(src);
				Element srcElement = tcmElement.addElement("Covered").addAttribute("path", VtrUtils.getFilePath(new File(this.pathToProjectDir), src));
				for (Integer line : lines) {
					srcElement.addElement("Line").addAttribute("number", line.toString());
				}
			}
		}
		if (!output) {
			return null;
		}
		return document.asXML();
	}
	
	/**
	 * Determine whether given test case has covered line(s)
	 * @param testCase
	 * @return
	 */
	protected static boolean hasCoveredLines(TestCase testCase) {
		if (testCase == null) {
			return false;
		}
		Map<File, List<Integer>> covered = testCase.getCoveredClassLinesMap();
		if (covered == null) {
			return false;
		}
		for (File src : covered.keySet()) {
			List<Integer> lines = covered.get(src);
			if (!lines.isEmpty()) { // has
				return true;
			}
		}
		return false;
	}

	/**
	 * Parse coverage results (jacoco.exec) and set them into each test case
	 * 
	 * @param commit
	 * @param projectDir
	 * @throws IOException
	 */
	protected List<TestSuite> setCoverageResults(Commit commit) throws IOException {
		File commitDir = getJacocoCommitDir(this.outputDir, this.projectId, commit);
		List<TestSuite> testSuites = MavenUtils.getTestSuites(this.projectDir);
		for (TestSuite ts : testSuites) {
			for (TestCase tc : ts.getTestCases()) {
				String method = tc.getClassName() + "#" + tc.getName();
				File cov = new File(commitDir, method + "!jacoco.exec");
				if (!cov.exists()) {
					LOGGER.warn("Coverage file does not exist: {}", cov.getAbsolutePath());
					continue;
				}
				// Each source class
				Map<File, List<Integer>> coveredClassLinesMap = new HashMap<>();
				CoverageBuilder cb = JacocoInstrumenter.parse(cov, new File(this.projectDir, "target/classes"));
				for (IClassCoverage cc : cb.getClasses()) {
					// Class
					File dir = new File(this.pathToProjectDir, "src/main/java");
					File src = new File(dir, cc.getName() + ".java");
					if (!src.exists()) {
						LOGGER.warn("File covered does not exit: {}", cc.getName());
						continue;
					}
					// Lines
					List<Integer> coveredLines = new ArrayList<>();
					for (int lineno = cc.getFirstLine(); lineno <= cc.getLastLine(); lineno++) {
						if (JacocoInstrumenter.isCoveredLine(cc.getLine(lineno).getStatus())) {
							coveredLines.add(lineno);
						}
					}
					coveredClassLinesMap.put(src, coveredLines);
				}
				tc.setCoveredClassLinesMap(coveredClassLinesMap);
			}
		}
		return testSuites;
	}

	/**
	 * 
	 * @param commit
	 * @param testSuites
	 * @throws IOException
	 * @throws GitAPIException
	 */
	protected List<TestCaseModification> detect(Commit commit, List<TestSuite> testSuites) throws IOException, GitAPIException {
		List<TestCaseModification> ret = new ArrayList<>();
		File subject = new File(this.pathToProjectDir);
		Tag curTag = DictionaryBase.getTagBy(commit, this.dict);
		BlameCommand bc = new BlameCommand(this.git.getRepository());
		for (TestSuite ts : testSuites) {
			for (TestCase tc : ts.getTestCases()) {
				// If this test case was NOT changed at this commit
				// Skip to detect because we focus on test-case modification
				// towards software release (i.e., tag)
				BlameResult br4test = bc.setFilePath(VtrUtils.getFilePath(subject, tc.getTestFile())).call();
				boolean modified = false;
				for (int lineno = tc.getStartLineNumber(); lineno <= tc.getEndLineNumber(); lineno++) {
					Tag tag = DictionaryBase.getTagBy(new Commit(br4test.getSourceCommit(lineno - 1)), this.dict);
					if (curTag.getDate().equals(tag.getDate())) {
						modified = true;
					}
				}
				if (modified) {
					LOGGER.info("Detect test modification: {}", tc.getFullName());
				} else {
					continue;
				}
				// Determine whether this modified test cases covered only
				// previously-committed source program parts
				Map<File, List<Integer>> coveredClassLinesMap = tc.getCoveredClassLinesMap();
				if (coveredClassLinesMap == null) {
					continue;
				}
				boolean previous = true;
				for (File src : coveredClassLinesMap.keySet()) {
					BlameResult br4src = bc.setFilePath(VtrUtils.getFilePath(subject, src)).call();
					List<Integer> linenoList = coveredClassLinesMap.get(src);
					for (Integer lineno : linenoList) {
						Tag tag = DictionaryBase.getTagBy(new Commit(br4src.getSourceCommit(lineno - 1)), this.dict);
						if (curTag.getDate().equals(tag.getDate())) {
							previous = false;
						}
					}
				}
				if (previous) {
					ret.add(new TestCaseModification(commit, tc));
				}
			}
		}
		return ret;
	}

	/**
	 * Get directory where VTR previously stored coverage results
	 * 
	 * @param output
	 * @param subjectId
	 * @param commit
	 * @return
	 */
	private static File getJacocoCommitDir(File output, String subjectId, Commit commit) {
		File subjectDir = new File(output, subjectId);
		File jacocoDir = new File(subjectDir, "jacoco");
		File commitDir = new File(jacocoDir, commit.getId());
		return commitDir;
	}

}

package jp.mzw.vtr.detect;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.patch.Patch;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.PatchAnalyzer.ModifiedLineRange;
import jp.mzw.vtr.dict.Dictionary;
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
	protected Dictionary dict;

	public Detector(Project project) throws IOException, ParseException {
		this.projectId = project.getProjectId();
		this.pathToProjectDir = project.getPathToProject();
		this.projectDir = project.getProjectDir();
		this.outputDir = project.getOutputDir();
		this.mavenHome = project.getMavenHome();
		this.git = GitUtils.getGit(this.pathToProjectDir);
		this.dict = new Dictionary(this.outputDir, this.projectId).parse().createPrevCommitByCommitIdMap();
	}

	@Override
	public void onCheckout(Commit commit) {
		try {
			CheckoutConductor.before(projectDir, mavenHome);
			dict.setTestSuite(commit, MavenUtils.getTestSuites(this.projectDir));
			List<TestCase> results = detect(commit);
			List<TestCaseModification> tcm = getTestCaseModifications(commit, results);
			output(commit, tcm);
			CheckoutConductor.after(projectDir, mavenHome);
		} catch (IOException | GitAPIException | RevisionSyntaxException | ParseException e) {
			LOGGER.warn(e.toString());
		}
	}

	/**
	 * Detect source code lines that are covered by each test case AND are
	 * finally modified in previous release
	 * 
	 * @param commit
	 * @return
	 * @throws IOException
	 * @throws GitAPIException
	 */
	protected List<TestCase> detect(Commit commit) throws IOException, GitAPIException {
		Tag cur = dict.getTagBy(commit);
		BlameCommand blame = new BlameCommand(this.git.getRepository());
		File dir = JacocoInstrumenter.getJacocoCommitDir(this.outputDir, this.projectId, commit);
		List<TestSuite> testSuites = dict.getTestSuiteBy(commit);
		// For each test case
		List<TestCase> ret = new ArrayList<>();
		for (TestSuite ts : testSuites) {
			for (TestCase tc : ts.getTestCases()) {
				File exec = new File(dir, tc.getFullName() + "!jacoco.exec");
				if (!exec.exists()) {
					LOGGER.info("Coverage file does not exist: {}", exec.getAbsolutePath());
					continue;
				}
				File result = this.getOutputFile(commit, tc);
				if (result.exists()) {
					LOGGER.info("Detection result is found: {}", result.getPath());
					continue;
				}
				if (detect(cur, blame, exec)) {
					ret.add(tc);
					LOGGER.info("Detect subject test-case modification: {} @ {}", tc.getFullName(), commit.getId());
				}
			}
		}
		return ret;
	}

	/**
	 * 
	 * @param cur
	 * @param blame
	 * @param exec
	 * @return
	 * @throws IOException
	 * @throws GitAPIException
	 */
	private boolean detect(Tag cur, BlameCommand blame, File exec) throws IOException, GitAPIException {
		boolean detect = true;
		CoverageBuilder cb = JacocoInstrumenter.getCoverageBuilder(this.projectDir, exec);
		IBundleCoverage bundle = cb.getBundle("VTR.Detector");
		for (IPackageCoverage pkg : bundle.getPackages()) {
			for (ISourceFileCoverage src : pkg.getSourceFiles()) {
				String filePath = "src/main/java/" + pkg.getName() + "/" + src.getName();
				BlameResult result = blame.setFilePath(filePath).call();
				for (int lineno = src.getFirstLine(); lineno <= src.getLastLine(); lineno++) {
					ILine line = src.getLine(lineno);
					if (JacocoInstrumenter.isCoveredLine(line.getStatus())) {
						// Determine covered source-line is modified
						// in previous release
						Tag tag = dict.getTagBy(new Commit(result.getSourceCommit(lineno)));
						if (cur.getDate().after(tag.getDate())) {
							// Previous
						} else {
							// After this test-case modification release
							// i.e., valid
							detect = false;
							break;
						}
					}
				}
				if (!detect) {
					break;
				}
			}
			if (!detect) {
				break;
			}
		}
		return detect;
	}

	/**
	 * 
	 * @param commit
	 * @param testCases
	 * @return
	 * @throws RevisionSyntaxException
	 * @throws AmbiguousObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 * @throws ParseException
	 */
	protected List<TestCaseModification> getTestCaseModifications(Commit commit, List<TestCase> testCases) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException,
			IOException, ParseException {
		// Obtain previous commit contents
		Commit prvCommit = dict.getPrevCommitBy(commit.getId());
		List<TestSuite> prvTestSuites = dict.getTestSuiteBy(prvCommit);
		if (prvTestSuites == null) {
			LOGGER.warn("Commit one before initial releaser? Test suites are not found: {}", prvCommit.getId());
			return new ArrayList<>();
		}
		// Analyze differences between current and previous commits
		List<TestCaseModification> ret = new ArrayList<>();
		Patch patch = GitUtils.getPatch(this.git.getRepository(), prvCommit, commit);
		PatchAnalyzer analyzer = new PatchAnalyzer(this.projectDir, patch).analyze();
		// Each test case
		for (TestCase tc : testCases) {
			List<Integer> methodLineRange = tc.getLineRange();
			List<ModifiedLineRange> modifiedLineRanges = analyzer.getModifiedLineRanges(tc.getTestFile());
			if (modifiedLineRanges == null) { // TODO Need to check why 'modifiedLineRanges' can be 'null'
				LOGGER.warn("TODO Need to check why 'modifiedLineRanges' can be 'null'", tc.getTestFile().getPath());
				continue;
			}
			// Analyze new lines that are in this test case AND are modified in this commit
			///// For new
			List<ModifiedLineRange> lineRanges = new ArrayList<>();
			for (ModifiedLineRange mlr : modifiedLineRanges) {
				List<Integer> lineRange = new ArrayList<>();
				for (Integer lineno : mlr.getNewLineRange()) {
					if (methodLineRange.contains(lineno)) {
						lineRange.add(lineno);
					}
				}
				if (!lineRange.isEmpty()) {
					lineRanges.add(new ModifiedLineRange(lineRange, mlr.getOldLineRange()));
				}
			}
			// Syntax-element nodes added in this commit
			List<ASTNode> newNodes = tc.getAllNodesIn(ModifiedLineRange.getMergedNewLineRange(lineRanges));
			///// For old
			// TODO Need to validate whether the same (class + method) is fine
			TestCase prvTestCase = TestSuite.getTestCaseWithClassMethodName(prvTestSuites, tc);
			List<ASTNode> oldNodes = null;
			if (prvTestCase != null) { // otherwise (partially) test-case addition
				List<Integer> prvMethodLineRange = prvTestCase.getLineRange();
				List<ModifiedLineRange> oldLineRanges = new ArrayList<>();
				for (ModifiedLineRange mlr : lineRanges) {
					List<Integer> lineRange = new ArrayList<>();
					for (Integer lineno : mlr.getOldLineRange()) {
						if (prvMethodLineRange.contains(lineno)) {
							lineRange.add(lineno);
						}
					}
					if (!lineRange.isEmpty()) {
						oldLineRanges.add(new ModifiedLineRange(mlr.getNewLineRange(), lineRange));
					}
				}
				oldNodes = prvTestCase.getAllNodesIn(ModifiedLineRange.getMergedOldLineRange(lineRanges));
			}
			///// For return
			ret.add(new TestCaseModification(commit, tc, newNodes, oldNodes));
		}
		return ret;
	}

	/**
	 * Output results in XML file
	 * @param commit
	 * @param revisedNodes
	 * @param originalNodes
	 * @throws IOException 
	 */
	public void output(Commit commit, List<TestCaseModification> testCaseModifications) throws IOException {
		for (TestCaseModification tcm : testCaseModifications) {
			String content = getXml(tcm.getNewNodes(), tcm.getOldNodes());
			if (content != null) {
				File file = getOutputFile(commit, tcm.getTestCase());
				FileUtils.writeStringToFile(file, content);
			}
		}
	}
	
	/**
	 * Get output file
	 * @param commit
	 * @param testCase
	 * @return
	 */
	protected File getOutputFile(Commit commit, TestCase testCase) {
		File projectDir = new File(this.outputDir, this.projectId);
		File diffDir = new File(projectDir, DETECT_DIR);
		File dir = new File(diffDir, commit.getId());
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return new File(dir, testCase.getFullName() + ".xml");
	}
	
	/**
	 * Get output content
	 * @param revisedNodes
	 * @param originalNodes
	 * @return
	 */
	protected String getXml(List<ASTNode> revisedNodes, List<ASTNode> originalNodes) {
		boolean valid = false;
		Document document = DocumentHelper.createDocument();
		Element root = document.addElement("ModifiedTestCaseNodes");
		// Revised
		Element revised = root.addElement("RevisedNodes");
		for (ASTNode node : revisedNodes) {
			valid = true;
			Element element = revised.addElement("Node");
			element.addAttribute("startPosition", String.valueOf(node.getStartPosition()));
			element.addAttribute("length", String.valueOf(node.getLength()));
			element.addAttribute("class", node.getClass().getName());
			element.addText(StringEscapeUtils.escapeXml10(node.toString()));
		}
		// Original
		if (originalNodes != null) {
			Element original = root.addElement("OriginalNodes");
			for (ASTNode node : originalNodes) {
				valid = true;
				Element element = original.addElement("Node");
				element.addAttribute("startPosition", String.valueOf(node.getStartPosition()));
				element.addAttribute("length", String.valueOf(node.getLength()));
				element.addAttribute("class", node.getClass().getName());
				element.addText(StringEscapeUtils.escapeXml10(node.toString()));
			}
		}
		if (!valid) {
			return null;
		}
		return document.asXML();
	}
}

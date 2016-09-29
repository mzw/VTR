package jp.mzw.vtr.detect.difflib;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import difflib.Chunk;
import difflib.Delta;
import difflib.Patch;
import jp.mzw.vtr.cluster.difflib.ChunkTagRest;
import jp.mzw.vtr.cluster.difflib.DiffUtils;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.detect.TestCaseModification;
import jp.mzw.vtr.detect.TestCaseModificationParser;
import jp.mzw.vtr.dict.DictionaryBase;
import jp.mzw.vtr.dict.DictionaryParser;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.GitUtils;
import jp.mzw.vtr.git.Tag;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

public class DiffAnalyzer implements CheckoutConductor.Listener {
	protected static Logger LOGGER = LoggerFactory.getLogger(DiffAnalyzer.class);

	protected String projectId;
	protected String pathToProjectDir;

	protected File projectDir;
	protected File outputDir;
	protected File mavenHome;

	protected Git git;
	protected Map<Tag, List<Commit>> dict;
	protected TestCaseModificationParser tcmParser;
	protected Map<String, Commit> prevCommitByCommitId;
	protected Map<String, List<TestSuite>> testSuitesByCommitId;

	/**
	 * Constructor
	 * @param project
	 * @throws IOException
	 * @throws ParseException
	 */
	public DiffAnalyzer(Project project) throws IOException, ParseException {
		this.projectId = project.getProjectId();
		this.pathToProjectDir = project.getPathToProject();
		this.projectDir = project.getProjectDir();
		this.outputDir = project.getOutputDir();
		this.mavenHome = project.getMavenHome();
		// Instantiate
		this.git = GitUtils.getGit(this.pathToProjectDir);
		this.dict = DictionaryParser.parseDictionary(new File(this.outputDir, this.projectId));
		this.tcmParser = new TestCaseModificationParser(project);
		this.prevCommitByCommitId = this.createPrevCommitByCommitIdMap();
		this.testSuitesByCommitId = new HashMap<>();
	}
	
	/**
	 * 
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	protected Map<String, Commit> createPrevCommitByCommitIdMap() throws IOException, ParseException {
		Map<String, Commit> ret = new HashMap<>();
		File dir = new File(this.outputDir, this.projectId);
		List<Commit> commits = DictionaryParser.parseCommits(dir);
		if (commits.size() < 3) {
			return ret;
		}
		Commit prv = commits.get(0);
		for (int i = 1; i < commits.size(); i++) {
			Commit cur = commits.get(i);
			ret.put(cur.getId(), prv);
			prv = cur;
		}
		return ret;
	}

	/**
	 * Get modified test suites
	 * 
	 * @param tsList
	 * @param tcmList
	 * @return
	 * @throws IOException 
	 * @throws GitAPIException 
	 */
	protected List<TestSuite> getModifiedTestSuites(List<TestSuite> tsList, List<TestCaseModification> tcmList) throws GitAPIException, IOException {
		List<TestSuite> ret = new ArrayList<>();
		for (TestSuite ts : tsList) {
			List<TestCase> testCases = new ArrayList<>();
			for (TestCase tc : ts.getTestCases()) {
				for (TestCaseModification tcm : tcmList) {
					if (tc.getFullName().equals(tcm.getTestCase().getFullName())) {
						setTestCaseModificationContent(tcm.getCommit(), tc);
						testCases.add(tc);
					}
				}
			}
			if (!testCases.isEmpty()) {
				ts.setTestCases(testCases);
				ret.add(ts);
			}
		}
		return ret;
	}
	
	/**
	 * Set old and new lines of test cases meaning test-case modifications
	 * @param commit
	 * @param testSuite
	 * @param testCase
	 * @throws GitAPIException
	 * @throws IOException
	 */
	protected void setTestCaseModificationContent(Commit commit, TestCase testCase) throws GitAPIException, IOException {
		List<Integer> modifiedLines = this.getModifiedLines(commit, testCase);
		if (!modifiedLines.isEmpty()) {
			this.setPatchDelta(commit, modifiedLines, testCase);
		}
	}
	
	/**
	 * Get line numbers of modified files corresponding test cases
	 * @param commit
	 * @param testSuite
	 * @param testCase
	 * @return modified file line numbers or empty list if no modification
	 * @throws GitAPIException
	 */
	protected List<Integer> getModifiedLines(Commit commit, TestCase testCase) throws GitAPIException {
		Tag curTag = DictionaryBase.getTagBy(commit, this.dict);
		BlameCommand bc = new BlameCommand(this.git.getRepository());
		BlameResult br = bc.setFilePath(VtrUtils.getFilePath(this.projectDir, testCase.getTestSuite().getTestFile())).call();
		List<Integer> modifiedLines = new ArrayList<>();
		for (int lineno = testCase.getStartLineNumber(); lineno <= testCase.getEndLineNumber(); lineno++) {
			Tag tag = DictionaryBase.getTagBy(new Commit(br.getSourceCommit(lineno - 1)), this.dict);
			if (curTag.getDate().equals(tag.getDate())) {
				modifiedLines.add(new Integer(lineno - 1));
			}
		}
		return modifiedLines;
	}
	
	/**
	 * Set old and new chunks to test cases
	 * @param commit
	 * @param modifiedLines
	 * @param testCase
	 * @throws RevisionSyntaxException
	 * @throws AmbiguousObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	protected void setPatchDelta(Commit commit, List<Integer> modifiedLines, TestCase testCase) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException, IOException {
		// Get current content to determine whether a patch delta corresponds to this test case
		List<String> lines = FileUtils.readLines(testCase.getTestFile());
		// Set
		Commit prv = this.getPrevCommitBy(commit.getId());
		Map<Patch<ChunkTagRest>, String> patches = this.getPatches(prv, commit);
		for (Patch<ChunkTagRest> patch : patches.keySet()) {
			Delta<ChunkTagRest> delta = null;
			for (Delta<ChunkTagRest> _delta : patch.getDeltas()) {
				// Determine whether this patch delta is that corresponding to this test-case modification
				boolean corr = false;
				Chunk<ChunkTagRest> revised = _delta.getRevised();
				List<ChunkTagRest> revisedLines = revised.getLines();
				for(int offset = 0; offset < revisedLines.size(); offset++) {
					int pos = revised.getPosition() + offset + 1;
					if (lines.size() < pos) {
						corr = false;
						break;
					}
					String line = lines.get(pos - 1);
					String revisedLine = revisedLines.get(offset).getRest();
					if (line.equals(revisedLine)) {
						corr = true;
					}
				}
				if (corr) {
					delta = _delta;
					break;
				}
			}
			if (delta != null) {
				testCase.setDelta(delta);
				break;
			}
		}
	}

	/**
	 * Get patch between previous and current commits
	 * 
	 * @param prv
	 *            Previous commit
	 * @param cur
	 *            Current commit
	 * @return Patch
	 * @throws RevisionSyntaxException
	 * @throws AmbiguousObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	protected Map<Patch<ChunkTagRest>, String> getPatches(Commit prv, Commit cur) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException,
			IOException {
		Map<Patch<ChunkTagRest>, String> ret = new HashMap<>();
		// Find RevCommits
		RevCommit prvCommit = GitUtils.getCommit(this.git.getRepository(), prv);
		RevCommit curCommit = GitUtils.getCommit(this.git.getRepository(), cur);
		// Get diff
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DiffFormatter df = new DiffFormatter(baos);
		df.setRepository(this.git.getRepository());
		if (prvCommit != null && curCommit != null) {
			List<DiffEntry> changes = df.scan(prvCommit.getTree(), curCommit.getTree());
			for (DiffEntry change : changes) {
				df.format(df.toFileHeader(change));
				String raw = baos.toString();
				Patch<ChunkTagRest> patch = DiffUtils.parseUnifiedDiff(Arrays.asList(raw.split("\n")));
				ret.put(patch, raw);
			}
		}
		return ret;
	}

	public Commit getPrevCommitBy(String commitId) {
		return this.prevCommitByCommitId.get(commitId);
	}
	public List<TestSuite> getTestSuiteBy(Commit commit) {
		return this.testSuitesByCommitId.get(commit.getId());
	}
	public void setTestSuite(Commit commit, List<TestSuite> testSuites) {
		this.testSuitesByCommitId.put(commit.getId(), testSuites);
	}

	@Override
	public void onCheckout(Commit commit) {
		LOGGER.info("onCheckout: {}", commit.getId());
		try {
			// Set current test suites
			Commit prvCommit = getPrevCommitBy(commit.getId()); // Need to be older commit first
			List<TestSuite> testSuites = MavenUtils.getTestSuites(this.projectDir);
			setTestSuite(commit, testSuites);
			LOGGER.info("Put test suites: {}", commit.getId());
			// Get current and previous test suites
			List<TestSuite> curTestSuites = getTestSuiteBy(commit);
			List<TestSuite> prvTestSuites = getTestSuiteBy(prvCommit);
			if (prvTestSuites == null) {
				LOGGER.warn("Commit one before initial releaser? Test suites are not found: {}", prvCommit.getId());
				return;
			}
			// Set test-case modifications
			List<TestCaseModification> testCaseModifications = this.tcmParser.parse(commit);
			List<TestSuite> modifiedTestSuites = this.getModifiedTestSuites(curTestSuites, testCaseModifications);
			for (TestSuite ts : modifiedTestSuites) {
				for (TestCase tc : ts.getTestCases()) {
					if (tc.getDelta() == null) {
						LOGGER.info("Found test-case modification but no patch delta: {}", tc.getFullName());
						continue;
					}
					// Revised
					List<ASTNode> revisedNodes = tc.getRevisedNodes();
					// Original
					TestCase prvTestCase = MavenUtils.getTestCaseInBy(prvTestSuites, tc);
					List<ASTNode> originalNodes = new ArrayList<>();
					if (prvTestCase != null) { // not test-case addition but modification
						originalNodes = prvTestCase.getOriginalNodes(tc.getDelta());
					}
					// Output
					output(commit, tc, revisedNodes, originalNodes);
				}
			}
			
		} catch (IOException | GitAPIException e) {
			LOGGER.warn(e.getMessage());
		}
	}
	
	/**
	 * Output results in XML file
	 * @param commit
	 * @param revisedNodes
	 * @param originalNodes
	 * @throws IOException 
	 */
	public void output(Commit commit, TestCase testCase, List<ASTNode> revisedNodes, List<ASTNode> originalNodes) throws IOException {
		String content = getXml(revisedNodes, originalNodes);
		if (content != null) {
			File file = getOutputFile(commit, testCase);
			FileUtils.writeStringToFile(file, content);
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
		File diffDir = new File(projectDir, "diff");
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
		Element original = root.addElement("OriginalNodes");
		for (ASTNode node : originalNodes) {
			valid = true;
			Element element = original.addElement("Node");
			element.addAttribute("startPosition", String.valueOf(node.getStartPosition()));
			element.addAttribute("length", String.valueOf(node.getLength()));
			element.addAttribute("class", node.getClass().getName());
			element.addText(StringEscapeUtils.escapeXml10(node.toString()));
		}
		if (!valid) {
			return null;
		}
		return document.asXML();
	}
}

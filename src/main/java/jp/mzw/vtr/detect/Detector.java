package jp.mzw.vtr.detect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
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
import org.eclipse.jgit.patch.Patch;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.IPackageCoverage;
import org.jacoco.core.analysis.ISourceFileCoverage;
import org.jacoco.core.data.ExecutionDataReader;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
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
	
	protected Map<String, Commit> prevCommitByCommitId;
	protected Map<String, List<TestSuite>> testSuitesByCommitId;

	public Detector(Project project) throws IOException, ParseException {
		this.projectId = project.getProjectId();
		this.pathToProjectDir = project.getPathToProject();
		this.projectDir = project.getProjectDir();
		this.outputDir = project.getOutputDir();
		this.mavenHome = project.getMavenHome();
		this.git = GitUtils.getGit(this.pathToProjectDir);
		this.dict = DictionaryParser.parseDictionary(new File(this.outputDir, this.projectId));
		this.prevCommitByCommitId = createPrevCommitByCommitIdMap();
		this.testSuitesByCommitId = new HashMap<>();
	}

	@Override
	public void onCheckout(Commit commit) {
		try {
			CheckoutConductor.before(projectDir, mavenHome);
			setTestSuite(commit);
			List<TestCase> results = detect(commit);
			for (TestCase tc : results) {
				System.out.println(tc.getFullName());
			}
//			output(commit, tcmList);
			CheckoutConductor.after(projectDir, mavenHome);
		} catch (IOException | GitAPIException e) {
			LOGGER.warn(e.toString());
		}
	}
	

//	/**
//	 * Output XML file containing test case modifications for
//	 * previously-released source programs
//	 * 
//	 * @param commit
//	 * @param tcmList
//	 * @throws IOException
//	 */
//	private void output(Commit commit, List<TestCaseModification> tcmList) throws IOException {
//		// No subject test-case modification
//		if (tcmList.size() < 1) {
//			return;
//		}
//		// Output
//		String xml = this.getXml(tcmList);
//		if (xml != null) {
//			File file = this.getOutputFile(commit);
//			FileUtils.writeStringToFile(file, xml);
//		}
//	}
//	
//	/**
//	 * 
//	 * @param commit
//	 * @return
//	 */
//	protected File getOutputFile(Commit commit) {
//		File outputSubjectDir = new File(this.outputDir, this.projectId);
//		File outputDetectDir = new File(outputSubjectDir, DETECT_DIR);
//		if (!outputDetectDir.exists()) {
//			outputDetectDir.mkdirs();
//		}
//		return new File(outputDetectDir, commit.getId() + ".xml");
//	}
//	
//	/**
//	 * 
//	 * @param tcmList
//	 * @return
//	 */
//	protected String getXml(List<TestCaseModification> tcmList) {
//		boolean output = false;
//		Document document = DocumentHelper.createDocument();
//		Element root = document.addElement("TestCaseModifications");
//		for (TestCaseModification tcm : tcmList) {
//			TestCase tc = tcm.getTestCase();
//			if (!hasCoveredLines(tc)) {
//				continue;
//			} else {
//				output = true;
//			}
//			Element tcmElement = root.addElement("TestCaseModification");
//			tcmElement.addAttribute("class", tc.getClassName()).addAttribute("method", tc.getName());
//			Map<File, List<Integer>> covered = tc.getCoveredClassLinesMap();
//			for (File src : covered.keySet()) {
//				List<Integer> lines = covered.get(src);
//				Element srcElement = tcmElement.addElement("Covered").addAttribute("path", VtrUtils.getFilePath(new File(this.pathToProjectDir), src));
//				for (Integer line : lines) {
//					srcElement.addElement("Line").addAttribute("number", line.toString());
//				}
//			}
//		}
//		if (!output) {
//			return null;
//		}
//		return document.asXML();
//	}
//	
//	/**
//	 * Determine whether given test case has covered line(s)
//	 * @param testCase
//	 * @return
//	 */
//	protected static boolean hasCoveredLines(TestCase testCase) {
//		if (testCase == null) {
//			return false;
//		}
//		Map<File, List<Integer>> covered = testCase.getCoveredClassLinesMap();
//		if (covered == null) {
//			return false;
//		}
//		for (File src : covered.keySet()) {
//			List<Integer> lines = covered.get(src);
//			if (!lines.isEmpty()) { // has
//				return true;
//			}
//		}
//		return false;
//	}

	/**
	 * 
	 * @param commit
	 * @return
	 * @throws IOException
	 * @throws GitAPIException 
	 */
	protected List<TestCase> detect(Commit commit) throws IOException, GitAPIException {
		List<TestCase> ret = new ArrayList<>();
		// For blame
		Tag cur = DictionaryBase.getTagBy(commit, this.dict);
		BlameCommand blame = new BlameCommand(this.git.getRepository());
		// Analyze coverage
		File dir = getJacocoCommitDir(this.outputDir, this.projectId, commit);
		List<TestSuite> testSuites = MavenUtils.getTestSuites(this.projectDir);
		for (TestSuite ts : testSuites) {
			for (TestCase tc : ts.getTestCases()) {
				File exec = new File(dir, tc.getFullName() + "!jacoco.exec");
				if (!exec.exists()) {
					LOGGER.info("Coverage file does not exist: {}", exec.getAbsolutePath());
					continue;
				}
				// Each source line
				boolean detect = true;
				CoverageBuilder cb = getCoverageBuilder(exec);
				IBundleCoverage bundle = cb.getBundle("VTR.Detector");
				for (IPackageCoverage pkg : bundle.getPackages()) {
					for (ISourceFileCoverage src : pkg.getSourceFiles()) {
						String filePath = "src/main/java/" + pkg.getName() + "/" + src.getName();
						BlameResult result = blame.setFilePath(filePath).call();
						for (int lineno = src.getFirstLine(); lineno <= src.getLastLine(); lineno++) {
							ILine line = src.getLine(lineno);
							if (JacocoInstrumenter.isCoveredLine(line.getStatus())) {
								// Determine covered source-line is modified in previous release
								Tag tag = DictionaryBase.getTagBy(new Commit(result.getSourceCommit(lineno)), this.dict);
								if (cur.getDate().after(tag.getDate())) {
									// Previous
								} else {
									// Modified this source-line after this test-case modification
									// i.e., this test-case modification is valid
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
				// No covered source-line is test-case addition so fine
				if (detect) {
					ret.add(tc);
				}
			}
		}
		return ret;
	}
	
	/**
	 * 
	 * @param exec
	 * @return
	 * @throws IOException
	 */
	protected CoverageBuilder getCoverageBuilder(File exec) throws IOException {
		// Prepare
	    FileInputStream fis = new FileInputStream(exec);
	    ExecutionDataStore eds = new ExecutionDataStore();
	    SessionInfoStore sis = new SessionInfoStore();
	    // Read
	    ExecutionDataReader edr = new ExecutionDataReader(fis);
	    edr.setExecutionDataVisitor(eds);
	    edr.setSessionInfoVisitor(sis);
	    while (edr.read());
	    fis.close();
	    // Build
	    CoverageBuilder builder = new CoverageBuilder();
	    Analyzer analyzer = new Analyzer(eds, builder);
	    analyzer.analyzeAll(new File(this.projectDir, "target/classes"));
	    // Return
	    return builder;
	}

//	/**
//	 * 
//	 * @param commit
//	 * @param testSuites
//	 * @throws IOException
//	 * @throws GitAPIException
//	 */
//	protected List<TestCaseModification> detect(Commit commit, List<TestSuite> testSuites) throws IOException, GitAPIException {
//		List<TestCaseModification> ret = new ArrayList<>();
//		Tag curTag = DictionaryBase.getTagBy(commit, this.dict);
//		BlameCommand blame = new BlameCommand(this.git.getRepository());
//		for (TestSuite ts : testSuites) {
//			for (TestCase tc : ts.getTestCases()) {
//				// Determine whether this modified test cases covered only
//				// previously-committed source program parts
//				Map<File, List<Integer>> coveredClassLinesMap = tc.getCoveredClassLinesMap();
//				if (coveredClassLinesMap == null) {
//					continue;
//				}
//				boolean previous = true;
//				for (File src : coveredClassLinesMap.keySet()) {
//					BlameResult result = blame.setFilePath(VtrUtils.getFilePath(this.projectDir, src)).call();
//					List<Integer> linenoList = coveredClassLinesMap.get(src);
//					for (Integer lineno : linenoList) {
//						Tag tag = DictionaryBase.getTagBy(new Commit(result.getSourceCommit(lineno - 1)), this.dict);
//						if (curTag.getDate().equals(tag.getDate())) {
//							previous = false;
//						}
//					}
//				}
//				if (previous) {
//					ret.add(new TestCaseModification(commit, tc));
//				}
//			}
//		}
//		return ret;
//	}

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
	 * Get previous commit by given commit
	 * @param commitId
	 * @return
	 */
	public Commit getPrevCommitBy(String commitId) {
		return this.prevCommitByCommitId.get(commitId);
	}
	
	/**
	 * Get test suites by given commit.
	 * Note: need to traverse Git repository in older commit first manner
	 * 
	 * @param commit
	 * @return
	 */
	public List<TestSuite> getTestSuiteBy(Commit commit) {
		return this.testSuitesByCommitId.get(commit.getId());
	}
	
	/**
	 * Set test suits at given commit
	 * @param commit
	 * @throws IOException
	 */
	protected void setTestSuite(Commit commit) throws IOException {
		List<TestSuite> testSuites = MavenUtils.getTestSuites(this.projectDir);
		this.testSuitesByCommitId.put(commit.getId(), testSuites);
	}
	
	protected void analyzeTestCaseModiticationContent(Commit commit, List<TestCase> testCases) {
		Commit prvCommit = getPrevCommitBy(commit.getId());
		List<TestSuite> prvTestSuites = getTestSuiteBy(prvCommit);
		List<Patch<ChunkTagRest>> patches = getPatches(prvCommit, commit);
		if (prvTestSuites == null) {
			LOGGER.warn("Commit one before initial releaser? Test suites are not found: {}", prvCommit.getId());
			return;
		}
		// Set test-case modifications
		for (TestCase tc : testCases) {
			TestSuite ts = tc.getTestSuite();
			for (Patch patch : patches) {
				
			}
		}
		
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
	}

	/**
	 * Get patches between given previous and current commits
	 * @param prv
	 * @param cur
	 * @return
	 * @throws RevisionSyntaxException
	 * @throws AmbiguousObjectException
	 * @throws IncorrectObjectTypeException
	 * @throws IOException
	 */
	protected List<Patch<ChunkTagRest>> getPatches(Commit prv, Commit cur) throws RevisionSyntaxException, AmbiguousObjectException, IncorrectObjectTypeException,
			IOException {
		List<Patch<ChunkTagRest>> ret = new ArrayList<>();
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
				ret.add(patch);
			}
		}
		return ret;
	}
}

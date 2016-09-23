package jp.mzw.vtr.cluster;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
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

public class Cluster implements CheckoutConductor.Listener {
	protected static Logger LOGGER = LoggerFactory.getLogger(Cluster.class);

	protected String projectId;
	protected String pathToProjectDir;

	protected File projectDir;
	protected File outputDir;
	protected File mavenHome;

	protected Git git;
	protected Map<Tag, List<Commit>> dict;
	protected TestCaseModificationParser tcmParser;
	protected Map<String, Commit> prevCommitByCommitId;

	/**
	 * Constructor
	 * @param project
	 * @throws IOException
	 * @throws ParseException
	 */
	public Cluster(Project project) throws IOException, ParseException {
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
	 */
	protected List<TestSuite> getModifiedTestSuites(List<TestSuite> tsList, List<TestCaseModification> tcmList) {
		List<TestSuite> ret = new ArrayList<>();
		for (TestSuite ts : tsList) {
			boolean detect = false;
			List<TestCase> testCases = new ArrayList<>();
			for (TestCase tc : ts.getTestCases()) {
				for (TestCaseModification tcm : tcmList) {
					if (tc.getFullName().equals(tcm.getTestCase().getFullName())) {
						detect = true;
						testCases.add(tc);
					}
				}
			}
			if (detect) {
				ts.setTestCases(testCases);
				ret.add(ts);
			}
		}
		return ret;
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

	protected void getTestCaseModificationContents(Commit commit, List<TestSuite> modifiedTestSuites) throws GitAPIException, IOException {
		Tag curTag = DictionaryBase.getTagBy(commit, this.dict);
		BlameCommand bc = new BlameCommand(this.git.getRepository());
		for (TestSuite ts : modifiedTestSuites) {
			for (TestCase tc : ts.getTestCases()) {
				BlameResult br = bc.setFilePath(VtrUtils.getFilePath(this.projectDir, tc.getTestFile())).call();
				List<Integer> modifiedLines = new ArrayList<>();
				for (int lineno = tc.getStartLineNumber(); lineno <= tc.getEndLineNumber(); lineno++) {
					Tag tag = DictionaryBase.getTagBy(new Commit(br.getSourceCommit(lineno - 1)), this.dict);
					if (curTag.getDate().equals(tag.getDate())) {
						modifiedLines.add(new Integer(lineno - 1));
					}
				}
				// Get content of test-case modification
				if (!modifiedLines.isEmpty()) {
					Commit prv = this.prevCommitByCommitId.get(commit.getId());
					Map<Patch<ChunkTagRest>, String> patches = this.getPatches(prv, commit);
					for (Patch<ChunkTagRest> patch : patches.keySet()) {
						for (Delta<ChunkTagRest> delta : patch.getDeltas()) {
							// Determine whether this patch delta is that corresponding to this test-case modification
							boolean corr = false;
							Chunk<ChunkTagRest> revised = delta.getRevised();
							List<ChunkTagRest> revisedLines = revised.getLines();
							for(int offset = 0; offset < revisedLines.size(); offset++) {
								int pos = revised.getPosition() + offset + 1;
								if (modifiedLines.contains(pos)) {
									corr = true;
									break;
								}
							}
							if (!corr) { // other patch delta
								continue;
							}
							for (ChunkTagRest tr : revised.getLines()) {
								String tag = tr.getTag();
								String line = tr.getRest();
								if (tag.equals("+") && !"".equals(line.trim())) {
									System.out.println("R+: " + line);
								}
							}
							System.out.println("=====");
							
							Chunk<ChunkTagRest> original = delta.getOriginal();
							for (ChunkTagRest tr : original.getLines()) {
								String tag = tr.getTag();
								String line = tr.getRest();
								if (tag.equals("-") && !"".equals(line.trim())) {
									System.out.println("R+: " + line);
								}
							}
							System.out.println("=====");
						}
					}
					

//					List<String> lines = FileUtils.readLines(tc.getTestFile());
//					for (Integer line : modifiedLines) {
//						System.out.println("Koko: " + lines.get(line));
//					}
				} else {
					continue;
				}
			}
		}

	}

	@Override
	public void onCheckout(Commit commit) {
		try {
			List<TestSuite> tsList = MavenUtils.getTestSuites(this.projectDir);
			List<TestCaseModification> tcmList = this.tcmParser.parse(commit);
			List<TestSuite> modifiedTestSuites = this.getModifiedTestSuites(tsList, tcmList);
			
		} catch (IOException e) {
			LOGGER.warn(e.getMessage());
		}
	}
}

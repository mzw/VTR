package jp.mzw.vtr.dict;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.Tag;
import jp.mzw.vtr.maven.TestSuite;

public class Dictionary {
	protected static Logger LOGGER = LoggerFactory.getLogger(Dictionary.class);

	protected File outputDir;
	protected String projectId;
	
	protected Map<Tag, List<Commit>> contents;
	protected Map<String, Commit> prevCommitByCommitId;
	protected Map<String, List<TestSuite>> testSuitesByCommitId;
	
	public Dictionary(File outputDir, String projectId) {
		this.outputDir = outputDir;
		this.projectId = projectId;
	}
	
	public Dictionary parse() throws IOException, ParseException {
		this.contents = DictionaryParser.parseDictionary(new File(this.outputDir, this.projectId));
		return this;
	}
	
	/**
	 * Create previous commit by given commit ID
	 * 
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public Dictionary createPrevCommitByCommitIdMap() throws IOException, ParseException {
		this.prevCommitByCommitId = new HashMap<>();
		File dir = new File(this.outputDir, this.projectId);
		List<Commit> commits = DictionaryParser.parseCommits(dir);
		if (commits.size() < 3) {
			return null;
		}
		Commit prv = commits.get(0);
		for (int i = 1; i < commits.size(); i++) {
			Commit cur = commits.get(i);
			this.prevCommitByCommitId.put(cur.getId(), prv);
			prv = cur;
		}
		return this;
	}

	/**
	 * Get tag relevant to given commit
	 * 
	 * @param commit
	 * @param dict
	 * @return
	 */
	public Tag getTagBy(Commit commit) {
		for (Tag tag : this.contents.keySet()) {
			for (Commit _commit : this.contents.get(tag)) {
				if (_commit.getId().equals(commit.getId())) {
					return tag;
				}
			}
		}
		return null;
	}

	/**
	 * Get previous commit by given commit
	 * 
	 * @param commitId
	 * @return
	 * @throws ParseException 
	 * @throws IOException 
	 */
	public Commit getPrevCommitBy(String commitId) throws IOException, ParseException {
		if (this.prevCommitByCommitId == null) {
			this.createPrevCommitByCommitIdMap();
		}
		return this.prevCommitByCommitId.get(commitId);
	}

	/**
	 * Get test suites by given commit. Note: need to traverse Git repository in
	 * older commit first manner
	 * 
	 * @param commit
	 * @return
	 */
	public List<TestSuite> getTestSuiteBy(Commit commit) {
		if (this.testSuitesByCommitId == null) {
			LOGGER.warn("Need to set test suites");
		}
		return this.testSuitesByCommitId.get(commit.getId());
	}

	/**
	 * Set test suits at given commit
	 * 
	 * @param commit
	 * @throws IOException
	 */
	public void setTestSuite(Commit commit, List<TestSuite> testSuites) {
		if (this.testSuitesByCommitId == null) {
			this.testSuitesByCommitId = new HashMap<>();
		}
		this.testSuitesByCommitId.put(commit.getId(), testSuites);
	}
	
}

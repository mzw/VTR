package jp.mzw.vtr.detect;

import java.util.List;
import java.util.Map;

import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.TestCase;

public class TestCaseModification {

	protected Commit commit;
	protected TestCase testCase;

	public TestCaseModification(Commit commit, TestCase testCase) {
		this.commit = commit;
		this.testCase = testCase;
	}

	public Commit getCommit() {
		return this.commit;
	}

	public TestCase getTestCase() {
		return this.testCase;
	}

	protected Map<String, List<Integer>> covered;

	public void setCoveredLines(Map<String, List<Integer>> covered) {
		this.covered = covered;
	}

	public Map<String, List<Integer>> getCoveredLines() {
		return this.covered;
	}

}

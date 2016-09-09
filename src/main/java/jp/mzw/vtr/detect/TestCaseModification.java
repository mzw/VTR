package jp.mzw.vtr.detect;

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

}

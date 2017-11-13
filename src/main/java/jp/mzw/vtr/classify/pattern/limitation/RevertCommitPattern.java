package jp.mzw.vtr.classify.pattern.limitation;

import jp.mzw.vtr.detect.TestCaseModification;

public class RevertCommitPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        return testCaseModification.getCommitMessage().toLowerCase().startsWith("revert");
    }
}

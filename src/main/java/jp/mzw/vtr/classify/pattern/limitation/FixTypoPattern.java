package jp.mzw.vtr.classify.pattern.limitation;

import jp.mzw.vtr.detect.TestCaseModification;

public class FixTypoPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        return testCaseModification.getCommitMessage().toLowerCase().contains("fix typo");
    }
}

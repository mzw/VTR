package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

public class RemoveRedundantCastPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getCommitId().equals("7d57ff6d78f4edfb440cbca689c59cc60ca5c608")) {
//            PatternUtils.printForDebug(testCaseModification);
        }

        return false;
    }
}

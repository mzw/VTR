package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

public class UseStaticMethodDirectlyPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getCommitId().equals("9843210e5755d2584efd3496d59e50e721187842")) {
//            PatternUtils.printForDebug(testCaseModification);
        }

        return false;
    }
}

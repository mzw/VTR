package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

public class UseProperFileConstructorPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getCommitId().equals("8296784dab7104e30cf1b1527ea20926a33eebcf")) {
//            PatternUtils.printForDebug(testCaseModification);
        }
        return false;
    }
}

package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;
import static jp.mzw.vtr.classify.pattern.PatternUtils.countNumOfValueOfMethod;


public class RemoveRedundantCastPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getCommitId().equals("4660148be3bffbaf6490ebb80fb5dcf90a32b44d")) {
//            PatternUtils.printForDebug(testCaseModification);
        }

        return (!testCaseModification.getOriginalNodeClassesWithText().isEmpty())
                && (0 < countNumOfValueOfMethod(testCaseModification.getOriginalNodeClassesWithText()))
                && (countNumOfValueOfMethod(testCaseModification.getRevisedNodeClassesWithText()) == 0);
    }
}

package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import static jp.mzw.vtr.classify.pattern.PatternUtils.countNumOfCloseMethodInvocation;

public class CloseResourcesPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (!testCaseModification.getCommitMessage().toLowerCase().contains("close")) {
            return false;
        }

        return ((countNumOfCloseMethodInvocation(testCaseModification.getOriginalNodeClassesWithText()) == 0)
                    && (0 < countNumOfCloseMethodInvocation(testCaseModification.getRevisedNodeClassesWithText())));
    }
}

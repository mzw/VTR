package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import static jp.mzw.vtr.classify.pattern.PatternUtils.countNumOfCloseMethodInvocation;

public class UseTryWithResourcesPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (!(testCaseModification.getCommitMessage().toLowerCase().contains("java7 language features")
                || testCaseModification.getCommitMessage().toLowerCase().contains("try with resources")
                || testCaseModification.getCommitMessage().toLowerCase().contains("try-with-resources"))) {
            return false;
        }

        // TODO: Try-with statement isn't included in Ast.
        return ((0 < countNumOfCloseMethodInvocation(testCaseModification.getOriginalNodeClassesWithText()))
                    && (countNumOfCloseMethodInvocation(testCaseModification.getRevisedNodeClassesWithText())
                            < countNumOfCloseMethodInvocation(testCaseModification.getOriginalNodeClassesWithText())));
    }
}

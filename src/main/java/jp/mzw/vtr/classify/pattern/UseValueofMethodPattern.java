package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import static jp.mzw.vtr.classify.pattern.PatternUtils.countNumOfValueOfMethod;


public class UseValueofMethodPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        return (!testCaseModification.getOriginalNodeClassesWithText().isEmpty())
                && (countNumOfValueOfMethod(testCaseModification.getOriginalNodeClassesWithText()) == 0)
                && (0 < countNumOfValueOfMethod(testCaseModification.getRevisedNodeClassesWithText()));
    }
}

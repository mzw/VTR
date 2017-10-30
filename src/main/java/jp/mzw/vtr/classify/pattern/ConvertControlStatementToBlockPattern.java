package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

public class ConvertControlStatementToBlockPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (!(testCaseModification.getCommitMessage().toLowerCase().contains("control statement")
                && testCaseModification.getCommitMessage().toLowerCase().contains("block"))) {
            return false;
        }

        return PatternUtils.sameAstNodes(testCaseModification.getOriginalNodeClassesWithText(),
                testCaseModification.getRevisedNodeClassesWithText());
    }
}

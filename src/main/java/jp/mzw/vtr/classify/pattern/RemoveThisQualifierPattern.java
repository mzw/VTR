package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class RemoveThisQualifierPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (!(testCaseModification.getCommitMessage().toLowerCase().contains("remove")
                && testCaseModification.getCommitMessage().toLowerCase().contains("this"))) {
            return false;
        }

        return ((0 < countNumOfThisExpression(testCaseModification.getOriginalNodeClassesWithText()))
                && (countNumOfThisExpression(testCaseModification.getRevisedNodeClassesWithText()) == 0));
    }

    private static int countNumOfThisExpression(List<String> nodes) {
        int count = 0;
        for (String node : nodes) {
            if (node.contains("org.eclipse.jdt.core.dom.ThisExpression:this")) {
                count++;
            }
        }

        return count;
    }
}

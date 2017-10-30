package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class RemoveUnnecessaryCastPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (countNumOfCastExpression(testCaseModification.getOriginalNodeClassesWithText())
                <= countNumOfCastExpression(testCaseModification.getRevisedNodeClassesWithText())) {
            return false;
        }
        return testCaseModification.getCommitMessage().toLowerCase().contains("cast");
    }

    static private int countNumOfCastExpression(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.CastExpression")) {
                cnt++;
            }
        }
        return cnt;
    }
}

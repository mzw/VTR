package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class RemoveObsoleteVariableAssignmentPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (countNumOfVariableDecralationFragment(testCaseModification.getOriginalNodeClassesWithText())
                <= countNumOfVariableDecralationFragment(testCaseModification.getRevisedNodeClassesWithText())) {
            return false;
        }

        return testCaseModification.getCommitMessage().toLowerCase().contains("assignment");
    }

    static private int countNumOfVariableDecralationFragment(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.contains("org.eclipse.jdt.core.dom.VariableDeclarationFragment")) {
                cnt++;
            }
        }
        return cnt;
    }
}

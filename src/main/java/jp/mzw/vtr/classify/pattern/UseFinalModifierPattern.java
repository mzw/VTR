package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class UseFinalModifierPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        return (testCaseModification.getCommitMessage().contains("final")
                && (countNumOfFinalModifiers(testCaseModification.getOriginalNodeClassesWithText())
                        < countNumOfFinalModifiers(testCaseModification.getRevisedNodeClassesWithText())));
    }

    static private int countNumOfFinalModifiers(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.contains("org.eclipse.jdt.core.dom.Modifier:final")) {
                cnt++;
            }
        }
        return cnt;
    }

    static private int countNumOfSingleVariableDeclaration(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.contains("org.eclipse.jdt.core.dom.SingleVariableDeclaration:")) {
                cnt++;
            }
        }
        return cnt;
    }
}

package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class RemoveUnusedExceptionsPattern {
    public static boolean match(TestCaseModification testCaseModification) {
        // In this pattern, developers remove unnecessary exceptions from test method.
        return countNumOfExceptions(testCaseModification.getRevisedNodeClassesWithText())
                < countNumOfExceptions(testCaseModification.getOriginalNodeClassesWithText());
    }

    private static int countNumOfExceptions(List<String> nodes) {
        int num = 0;
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.SimpleType")
                    && node.endsWith("Exception")) {
                num++;
            }
        }
        return num;
    }
}

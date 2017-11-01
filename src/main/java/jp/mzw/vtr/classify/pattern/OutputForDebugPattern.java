package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class OutputForDebugPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (!(onlySystemOutPrintExpressionStatment(testCaseModification.getOriginalNodeClassesWithText())
                && onlySystemOutPrintExpressionStatment(testCaseModification.getRevisedNodeClassesWithText()))) {
            return false;
        }

        return ((countNumOfSystemOutPrint(testCaseModification.getOriginalNodeClassesWithText()) == 0
                        && 0 < countNumOfSystemOutPrint(testCaseModification.getRevisedNodeClassesWithText()))
                || (countNumOfSystemOutPrint(testCaseModification.getRevisedNodeClassesWithText()) == 0
                        && 0 < countNumOfSystemOutPrint(testCaseModification.getOriginalNodeClassesWithText())));
    }


    static private int countNumOfSystemOutPrint(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:System.out.print")) {
                cnt++;
            }
        }
        return cnt;
    }

    static private boolean onlySystemOutPrintExpressionStatment(List<String> nodes) {
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:")) {
                if (node.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:System.out.print")) {
                    continue;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

}

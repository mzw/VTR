package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

public class UseProcessWaitForPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        boolean busyWait = false;
        for (String node : testCaseModification.getOriginalNodeClassesWithText()) {
            if (node.startsWith("org.eclipse.jdt.core.dom.WhileStatement:")
                    && node.contains("Thread.sleep")) {
                busyWait = true;
                break;
            }
        }
        if (!busyWait) {
            return false;
        }
        for (String node : testCaseModification.getRevisedNodeClassesWithText()) {
            if (node.startsWith("org.eclipse.jdt.core.dom.MethodInvocation:")
                    && node.endsWith("waitFor()")) {
                return true;
            }
        }
        return false;
    }
}

package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class CloseResourcesPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (!testCaseModification.getCommitMessage().toLowerCase().contains("close")) {
            return false;
        }

        return ((countNumOfCloseMethodInvocation(testCaseModification.getOriginalNodeClassesWithText()) == 0)
                    && (0 < countNumOfCloseMethodInvocation(testCaseModification.getRevisedNodeClassesWithText())));
    }

    static private int countNumOfCloseMethodInvocation(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.MethodInvocation:") && node.endsWith("close()")) {
                cnt++;
            }
        }
        return cnt;
    }
}

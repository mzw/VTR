package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class ImportAssertNonStaticPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        for (String revisedNode : testCaseModification.getRevisedNodeClassesWithText()) {
            if (revisedNode.startsWith("org.eclipse.jdt.core.dom.MarkerAnnotation:@Test")) {
                // prevent below false positives
                // https://github.com/apache/commons-math/commit/5fbeb731b9d26a6f340fd3772e86cd23ba61c65a
                return false;
            }
        }

        return (0 < countNumOfNonStaticAssertInvocation(testCaseModification.getOriginalNodeClassesWithText()))
                && (0 == countNumOfNonStaticAssertInvocation(testCaseModification.getRevisedNodeClassesWithText()))
                && (0 == countNumOfStaticAssertInvocation(testCaseModification.getOriginalNodeClassesWithText()))
                && (0 < countNumOfStaticAssertInvocation(testCaseModification.getRevisedNodeClassesWithText()))
                && (countNumOfNonStaticAssertInvocation(testCaseModification.getOriginalNodeClassesWithText())
                        == countNumOfStaticAssertInvocation(testCaseModification.getRevisedNodeClassesWithText()));
    }

    static private int countNumOfStaticAssertInvocation(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.MethodInvocation:Assert")) {
                cnt++;
            }
        }
        return cnt;
    }

    static private int countNumOfNonStaticAssertInvocation(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.MethodInvocation:assert")) {
                cnt++;
            }
        }
        return cnt;
    }

}

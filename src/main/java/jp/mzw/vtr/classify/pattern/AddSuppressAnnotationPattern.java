package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class AddSuppressAnnotationPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        return ((countNumOfSuppressWaningsAnnotations(testCaseModification.getOriginalNodeClassesWithText()) == 0)
                && (0 < countNumOfSuppressWaningsAnnotations(testCaseModification.getRevisedNodeClassesWithText())));
    }

    static private int countNumOfSuppressWaningsAnnotations(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.SingleMemberAnnotation:@SuppressWarnings")) {
                cnt++;
            }
        }
        return cnt;
    }

}

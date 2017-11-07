package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class AddTestAnnotationPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        for (String keyword : KEYWORDS) {
            if (testCaseModification.getCommitMessage().toLowerCase().contains(keyword)) {
                return true;
            }
        }

        if (testCaseModification.getOriginalNodeClassesWithText().isEmpty()
                && (0 < countNumOfTestAnnotation(testCaseModification.getRevisedNodeClassesWithText()))
                && onlyTestAnnotation(testCaseModification.getRevisedNodeClassesWithText())) {
            return true;
        }
        return false;
    }

    static private String[] KEYWORDS = {
            "junit 4 annotation style"
    };

    static private long countNumOfTestAnnotation(List<String> nodes) {
        return nodes.stream().filter(node -> node.startsWith("org.eclipse.jdt.core.dom.MarkerAnnotation:@Test")).count();
    }

    static private boolean onlyTestAnnotation(List<String> nodes) {
        for (String node : nodes) {
            if (!(node.startsWith("org.eclipse.jdt.core.dom.MarkerAnnotation:@Test")
                    || node.startsWith("org.eclipse.jdt.core.dom.SimpleName:Test"))) {
                return false;
            }
        }
        return true;
    }
}

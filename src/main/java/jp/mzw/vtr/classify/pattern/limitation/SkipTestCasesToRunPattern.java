package jp.mzw.vtr.classify.pattern.limitation;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class SkipTestCasesToRunPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getOriginalNodeClassesWithText().isEmpty()
                && testCaseModification.getCommitMessage().toLowerCase().contains("@ignore")) {
            return true;
        }

        return (countNumOfIgnoreAnnotation(testCaseModification.getOriginalNodeClassesWithText()) == 0)
                && (0 < countNumOfIgnoreAnnotation(testCaseModification.getRevisedNodeClassesWithText()));
    }

    static private long countNumOfIgnoreAnnotation(List<String> nodes) {
        return nodes.stream().filter(node -> (
                node.startsWith("org.eclipse.jdt.core.dom.SingleMemberAnnotation:@Ignore")
                || node.startsWith("org.eclipse.jdt.core.dom.MarkerAnnotation:@Ignore")
        )).count();
    }

}

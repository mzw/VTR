package jp.mzw.vtr.classify.pattern.limitation;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class NewTestCasesPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        return testCaseModification.getOriginalNodeClassesWithText().isEmpty()
                && (0 < countNumOfMethodDeclaration(testCaseModification.getRevisedNodeClassesWithText()));
    }

    static private long countNumOfMethodDeclaration(List<String> nodes) {
        return nodes.stream().filter(node -> node.startsWith("org.eclipse.jdt.core.dom.MethodDeclaration:")).count();
    }

}

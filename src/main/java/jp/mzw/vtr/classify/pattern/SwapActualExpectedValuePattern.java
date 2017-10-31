package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class SwapActualExpectedValuePattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (!testCaseModification.getCommitMessage().toLowerCase().contains("assert")) {
            return false;
        }

        List<String> originalNodes = testCaseModification.getOriginalNodeClassesWithText();
        List<String> revisedNodes  = testCaseModification.getRevisedNodeClassesWithText();
        if (originalNodes.size() != revisedNodes.size()) {
            return false;
        }

        for (int i = 0; i < originalNodes.size(); i++) {
            String originalNode = originalNodes.get(i);
            if (!(originalNode.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:Assert.assert")
                    || (originalNode.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:assert")))) {
                continue;
            }
            String revisedNode = originalNodes.get(i);
            if (!(revisedNode.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:Assert.assert")
                    || (revisedNode.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:assert")))) {
                return false;
            }
        }
        return true;
    }
}

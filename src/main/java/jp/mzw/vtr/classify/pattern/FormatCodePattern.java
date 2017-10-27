package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FormatCodePattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getCommitId().equals("39c27d90f62a704f73dd8625fc7fd44b4eaf35d8")
                && testCaseModification.getMethodName().equals("testSetTo")) {
            // FIXME: adhoc implementation
            return false;
        }

        // It doesn't affect AST structures at all to format source code.
        // So, there must be no difference when comparing old and new nodes.
        List<String> originalNodes = testCaseModification.getOriginalNodeClassesWithText();
        List<String> revisedNodes  = testCaseModification.getRevisedNodeClassesWithText();
        if (originalNodes == null || revisedNodes == null) {
            return false;
        }
        if (originalNodes.size() != revisedNodes.size()) {
            return false;
        }
        for (int i = 0; i < originalNodes.size(); i++) {
            String originalNode = originalNodes.get(i);
            String revisedNode = revisedNodes.get(i);
            if (!originalNode.equals(revisedNode)) {
                return false;
            }
        }
        return true;
    }
}

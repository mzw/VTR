package jp.mzw.vtr.classify.pattern.limitation;

import jp.mzw.vtr.classify.pattern.PatternUtils;
import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class ChangeNamePattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getCommitId().equals("1d3b92e9e8960fd823e2b5cd28aaf0c52f8aeef0")) {
//            PatternUtils.printForDebug(testCaseModification);
        }
        List<String> originNodes = testCaseModification.getOriginalNodeClassesWithText();
        List<String> revisedNodes = testCaseModification.getRevisedNodeClassesWithText();
        if (originNodes.isEmpty() || originNodes.size() != revisedNodes.size()) {
            return false;
        }
        for (int i = 0; i < originNodes.size(); i++) {
            String originNode  = originNodes.get(i).split(":")[0];
            String revisedNode = revisedNodes.get(i).split(":")[0];
            if (!originNode.equals(revisedNode)) {
                return false;
            }
        }
        return true;
    }
}

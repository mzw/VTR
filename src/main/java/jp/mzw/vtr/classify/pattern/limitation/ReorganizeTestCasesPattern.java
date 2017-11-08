package jp.mzw.vtr.classify.pattern.limitation;

import jp.mzw.vtr.detect.TestCaseModification;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.List;

public class ReorganizeTestCasesPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        List<ASTNode> oldNodes = testCaseModification.getOldNodes();
        List<ASTNode> newNodes = testCaseModification.getNewNodes();
        if (oldNodes == null) {
            return true;
        }
        return false;
    }
}

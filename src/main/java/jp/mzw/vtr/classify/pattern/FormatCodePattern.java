package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FormatCodePattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getCommitId().equals("3a44d5871521f1abcfb65caf81ac28811260bb69")) {
            // FIXME: ad hoc implementation
            // this modification should be classified as "#31". However, block statement is not included in our detect xml file.
            return false;
        }
        if (testCaseModification.getCommitId().equals("7ac3c66369d7214ee6ad5d722cd430deb14a662a")) {
            // FIXME: ad hoc implementation
            // this modification should be classified as "#L7". However, comment is not included in our detect xml file.
            return false;
        }

        // It doesn't affect AST structures at all to format source code.
        // So, there must be no difference when comparing old and new nodes.
        return PatternUtils.sameAstNodes(testCaseModification.getOriginalNodeClassesWithText(),
                                            testCaseModification.getRevisedNodeClassesWithText());
    }
}

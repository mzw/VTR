package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FormatCodePattern {
    static Logger LOGGER = LoggerFactory.getLogger(FormatCodePattern.class);

    public FormatCodePattern() {}

    static public boolean match(TestCaseModification testCaseModification) {
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
        if (!fomattingRelatedMessage(testCaseModification.getCommitMessage())) {
            return false;
        }
        return true;
    }

    static private String[] FORMATTING_RELATED_KEYWORDS = {
            "escape", "whitespace", "formatting"
    };

    static private boolean fomattingRelatedMessage(String message) {
        message = message.toLowerCase();
        for (String keyword : FORMATTING_RELATED_KEYWORDS) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

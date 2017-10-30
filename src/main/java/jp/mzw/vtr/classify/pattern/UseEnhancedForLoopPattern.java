package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

public class UseEnhancedForLoopPattern {
    public static boolean match(TestCaseModification testCaseModification) {
        // TODO: AST nodes don't include for statement node and enhanced for statement node.
        // So, I used only commit messages.
        if (testCaseModification.getCommitMessage().toLowerCase().contains("foreach loops")
                || testCaseModification.getCommitMessage().toLowerCase().contains("enhanced for loops")) {
            return true;
        }
        return false;
    }
}

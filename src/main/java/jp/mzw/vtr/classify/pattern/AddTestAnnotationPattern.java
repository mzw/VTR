package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

public class AddTestAnnotationPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        for (String keyword : KEYWORDS) {
            if (testCaseModification.getCommitMessage().toLowerCase().contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String[] KEYWORDS = {
            "junit 4 annotation style"
    };
}

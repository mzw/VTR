package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

public class FixJavadocErrorsPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getCommitId().equals("466a16575d4db6eb942fa2fb53bdae3936cfd066")) {
//            PatternUtils.printForDebug(testCaseModification);
        }
        for (String keyword : KEYWORDS) {
            if (testCaseModification.getCommitMessage().toLowerCase().contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String[] KEYWORDS = {
            "fix javadoc", "@throws"
    };
}

package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

public class FixJavadocErrorsPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        for (String keyword : KEYWORDS) {
            if (testCaseModification.getCommitMessage().toLowerCase().contains(keyword)) {
                return true;
            }
        }

//        System.out.println("commit message");
//        System.out.println(testCaseModification.getCommitMessage());
//        System.out.println("original nodes");
//        for (String originalNode : testCaseModification.getOriginalNodeClassesWithText()) {
//            System.out.println(originalNode);
//        }
//        System.out.println("revised nodes");
//        for (String revisedNode : testCaseModification.getRevisedNodeClassesWithText()) {
//            System.out.println(revisedNode);
//        }

        return false;
    }

    private static String[] KEYWORDS = {
            "fix javadoc"
    };
}

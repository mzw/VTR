package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

public class PatternUtils {
    public static void printForDebug(TestCaseModification testCaseModification) {
        System.out.println("================== test case modification name ==================");
        System.out.println(testCaseModification.getMethodName());
        System.out.println("================== commit message ==================");
        System.out.println(testCaseModification.getCommitMessage());
        System.out.println("================== original nodes ==================");
        for (String originNode : testCaseModification.getOriginalNodeClassesWithText()) {
            System.out.println(originNode);
        }
        System.out.println("================== revised nodes ==================");
        for (String originNode : testCaseModification.getRevisedNodeClassesWithText()) {
            System.out.println(originNode);
        }
    }
}

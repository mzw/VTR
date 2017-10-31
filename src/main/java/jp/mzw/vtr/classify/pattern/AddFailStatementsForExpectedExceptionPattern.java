package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class AddFailStatementsForExpectedExceptionPattern {

    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getCommitId().equals("5e34e05fd8e4ce1204e19eaf34e55aa95df4c5c0")) {
            PatternUtils.printForDebug(testCaseModification);
        }

        if (!testCaseModification.getCommitMessage().toLowerCase().contains("fail")) {
            return false;
        }

        return ((countNumOfFailStatement(testCaseModification.getOriginalNodeClassesWithText()) == 0)
                    && (0 < countNumOfFailStatement(testCaseModification.getRevisedNodeClassesWithText())));
    }

    static private int countNumOfFailStatement(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:fail")
                    || node.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:Assert.fail")) {
                cnt++;
            }
        }
        return cnt;
    }
}

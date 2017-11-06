package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class UseAssertNotSamePattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getCommitId().equals("439ee3bc05432ca2d980894b35b8b73e4a9d2932")) {
            PatternUtils.printForDebug(testCaseModification);
        }

        return (0 < countNumOfAssertTrueNotEqual(testCaseModification.getOriginalNodeClassesWithText()))
                && (0 == countNumOfAssertTrueNotEqual(testCaseModification.getRevisedNodeClassesWithText()))
                && (0 < countNumOfAssertNotSame(testCaseModification.getRevisedNodeClassesWithText()));
    }

    static private long countNumOfAssertTrueNotEqual(List<String> nodes) {
        return nodes.stream().filter(
                node -> ((node.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:assertTrue")
                            || node.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:Assert.assertTrue"))
                        && node.contains("!="))).count();
    }

    static private long countNumOfAssertNotSame(List<String> nodes) {
        return nodes.stream().filter(
                node -> (node.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:assertNotSame")
                            || node.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:Assert.assertNotSame"))).count();
    }
}

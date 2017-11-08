package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class UseAssertNullProperlyPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        return (0 < countNumOfAssertEqualsNull(testCaseModification.getOriginalNodeClassesWithText()))
                && (0 == countNumOfAssertEqualsNull(testCaseModification.getRevisedNodeClassesWithText()))
                && (0 < countNumOfAssertNull(testCaseModification.getRevisedNodeClassesWithText()));
    }

    static private long countNumOfAssertEqualsNull(List<String> nodes) {
        return nodes.stream().filter(
                node -> ((node.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:assertEquals")
                            || node.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:assertArrayEquals"))
                        && node.contains("null"))).count();
    }

    static private long countNumOfAssertNull(List<String> nodes) {
        return nodes.stream().filter(
                node -> node.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:assertNull")
        ).count();
    }
}

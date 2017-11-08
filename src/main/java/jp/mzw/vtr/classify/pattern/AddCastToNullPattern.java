package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class AddCastToNullPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getOriginalNodeClassesWithText().isEmpty()) {
            return false;
        }

        return (0 == countNumOfNullCastExpression(testCaseModification.getOriginalNodeClassesWithText()))
                && (0 < countNumOfNullCastExpression(testCaseModification.getRevisedNodeClassesWithText()));
    }

    static private long countNumOfNullCastExpression(List<String> nodes) {
        return nodes.stream().filter(node ->
                (node.startsWith("org.eclipse.jdt.core.dom.CastExpression") && node.endsWith("null"))).count();
    }
}

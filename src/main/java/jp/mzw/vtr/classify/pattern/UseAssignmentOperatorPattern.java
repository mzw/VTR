package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class UseAssignmentOperatorPattern {

    static final String[] ASSIGNMENT_OPERATOR = {"+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>="};

    static public boolean match(TestCaseModification testCaseModification) {
        return (countNumOfInfixExpression(testCaseModification.getRevisedNodeClassesWithText())
                    < countNumOfInfixExpression(testCaseModification.getOriginalNodeClassesWithText()))
                && (countNumOfAssignmentOperator(testCaseModification.getOriginalNodeClassesWithText())
                    < countNumOfAssignmentOperator(testCaseModification.getRevisedNodeClassesWithText()));
    }

    static private long countNumOfInfixExpression(List<String> nodes) {
        return nodes.stream().filter(node -> node.startsWith("org.eclipse.jdt.core.dom.InfixExpression:")).count();
    }

    static private long countNumOfAssignmentOperator(List<String> nodes) {
        long cnt = 0;
        for (String node : nodes) {
            if (!node.startsWith("org.eclipse.jdt.core.dom.Assignment:")) {
                continue;
            }
            for (String operator : ASSIGNMENT_OPERATOR) {
                if (node.contains(operator)) {
                    cnt++;
                }
            }
        }
        return cnt;
    }

}

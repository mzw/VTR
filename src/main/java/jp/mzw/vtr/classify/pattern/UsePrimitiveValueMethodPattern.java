package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class UsePrimitiveValueMethodPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        return countNumOfTypeValueMethod(testCaseModification.getOriginalNodeClassesWithText())
                < countNumOfTypeValueMethod(testCaseModification.getRevisedNodeClassesWithText());
    }

    static private long countNumOfTypeValueMethod(List<String> nodes) {
        return nodes.stream().filter(node -> (
                node.startsWith("org.eclipse.jdt.core.dom.SimpleName:booleanValue")
                        || node.startsWith("org.eclipse.jdt.core.dom.SimpleName:byteValue")
                        || node.startsWith("org.eclipse.jdt.core.dom.SimpleName:charValue")
                        || node.startsWith("org.eclipse.jdt.core.dom.SimpleName:doubleValue")
                        || node.startsWith("org.eclipse.jdt.core.dom.SimpleName:floatValue")
                        || node.startsWith("org.eclipse.jdt.core.dom.SimpleName:intValue")
                        || node.startsWith("org.eclipse.jdt.core.dom.SimpleName:longValue")
                        || node.startsWith("org.eclipse.jdt.core.dom.SimpleName:shortValue")
                )).count();
    }
}

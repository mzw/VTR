package jp.mzw.vtr.classify.pattern.limitation;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class AddJavadocPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        return testCaseModification.getOriginalNodeClassesWithText().isEmpty()
                && (0 < countNumOfJavadoc(testCaseModification.getRevisedNodeClassesWithText()));
    }

    static private long countNumOfJavadoc(List<String> nodes) {
        return nodes.stream().filter(node -> node.startsWith("org.eclipse.jdt.core.dom.Javadoc")).count();
    }
}

package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class AddSuppressAnnotationPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        return ((countNumOfSuppressWarningsAnnotations(testCaseModification.getOriginalNodeClassesWithText()) == 0)
                && (0 < countNumOfSuppressWarningsAnnotations(testCaseModification.getRevisedNodeClassesWithText())));
    }

    static private long countNumOfSuppressWarningsAnnotations(List<String> nodes) {
        return nodes.stream().filter(node -> node.startsWith("org.eclipse.jdt.core.dom.SingleMemberAnnotation:@SuppressWarnings")).count();
    }

}

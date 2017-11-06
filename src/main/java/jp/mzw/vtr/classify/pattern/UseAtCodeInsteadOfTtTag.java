package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class UseAtCodeInsteadOfTtTag {
    static public boolean match(TestCaseModification testCaseModification) {
        return (0 < countNumOfTtTag(testCaseModification.getOriginalNodeClassesWithText()))
                    && (0 < countNumOfAtCode(testCaseModification.getRevisedNodeClassesWithText()));
    }

    static private long countNumOfTtTag(List<String> nodes) {
        return nodes.stream()
                .filter(node -> (node.startsWith("org.eclipse.jdt.core.dom.TextElement:") && node.contains("&lt;tt&gt;") && node.contains("&lt;/tt&gt;"))).count();
    }

    static private long countNumOfAtCode(List<String> nodes) {
        return nodes.stream()
                .filter(node -> (node.startsWith("org.eclipse.jdt.core.dom.TagElement:")) && node.contains("@code")).count();
    }

}

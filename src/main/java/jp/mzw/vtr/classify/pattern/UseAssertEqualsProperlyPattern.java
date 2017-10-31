package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

public class UseAssertEqualsProperlyPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (!testCaseModification.getCommitMessage().toLowerCase().contains("assert")) {
            return false;
        }
        boolean assertTrue = false;
        for (String originalNode : testCaseModification.getOriginalNodeClassesWithText()) {
            if (originalNode.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:Assert.assertTrue")
                    || originalNode.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:assertTrue")) {
                assertTrue = true;
                break;
            }
        }
        if (!assertTrue) {
            return false;
        }
        for (String revisedNode : testCaseModification.getRevisedNodeClassesWithText()) {
            if (revisedNode.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:Assert.assertEquals")
                    || revisedNode.startsWith("org.eclipse.jdt.core.dom.ExpressionStatement:assertEquals")) {
                return true;
            }
        }
        return false;
    }
}

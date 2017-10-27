package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

public class UseStringContainsPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        // In this pattern, developers replaced `str.indexOf(target) == -1` with `str.contains(target)`.
        // So, there must be `indexOf` in old nodes and `contains` in revised nodes.
        boolean indexOf = false;
        for (String oldNode : testCaseModification.getOriginalNodeClassesWithText()) {
            if (oldNode.equals("org.eclipse.jdt.core.dom.SimpleName:indexOf")) {
                indexOf = true;
                break;
            }
        }
        if (!indexOf) {
            return false;
        }
        for (String revisedNode : testCaseModification.getRevisedNodeClassesWithText()) {
            if (revisedNode.equals("org.eclipse.jdt.core.dom.SimpleName:contains")) {
                return true;
            }
        }
        return false;
    }
}

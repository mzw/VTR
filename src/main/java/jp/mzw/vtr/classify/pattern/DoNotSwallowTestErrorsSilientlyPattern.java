package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;


public class DoNotSwallowTestErrorsSilientlyPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        // In this pattern, developers fix test cases that don't throw exception at proper timing.
        // Here are some examples...
        // https://github.com/apache/commons-email/commit/addc61dee9caa1a651a2aa8a9cfd2644b6b66a0d

        boolean tryState = false;
        boolean catchClause = false;
        for (String originalNode : testCaseModification.getOriginalNodeClassesWithText()) {
            if (originalNode.contains("org.eclipse.jdt.core.dom.TryStatement")) {
                tryState = true;
            }
            if (tryState && originalNode.contains("org.eclipse.jdt.core.dom.CatchClause")) {
                catchClause = true;
                break;
            }
        }
        if (!(tryState && catchClause)) {
            return false;
        }
        for (String revisedNode : testCaseModification.getRevisedNodeClassesWithText()) {
            if (revisedNode.contains("org.eclipse.jdt.core.dom.TryStatement")) {
                return false;
            }
            if (revisedNode.contains("org.eclipse.jdt.core.dom.CatchClause")) {
                return false;
            }
            if (revisedNode.contains("org.eclipse.jdt.core.dom.MemberValuePair:expected=")) {
                // this is HandleExpectedExceptionProperlyPattern
                return false;
            }
        }
        return true;
    }
}

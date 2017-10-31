package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;


public class DoNotSwallowTestErrorsSilientlyPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        // In this pattern, developers fix test cases that don't throw exception at proper timing.
        // Here are some examples...
        // https://github.com/apache/commons-dbutils/commit/bdf0b337ea209c7e3fd7de52f4da3b1afa0678aa
        // https://github.com/apache/commons-email/commit/addc61dee9caa1a651a2aa8a9cfd2644b6b66a0d

        if (testCaseModification.getCommitId().equals("bdf0b337ea209c7e3fd7de52f4da3b1afa0678aa")) {
            PatternUtils.printForDebug(testCaseModification);
        }

        boolean catchClause = false;
        for (String originalNode : testCaseModification.getOriginalNodeClassesWithText()) {
            if (originalNode.contains("org.eclipse.jdt.core.dom.CatchClause")) {
                catchClause = true;
                break;
            }
        }
        if (!catchClause) {
            return false;
        }
        for (String revisedNode : testCaseModification.getRevisedNodeClassesWithText()) {
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

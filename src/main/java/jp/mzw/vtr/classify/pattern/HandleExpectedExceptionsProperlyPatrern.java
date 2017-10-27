package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class HandleExpectedExceptionsProperlyPatrern {
    static public boolean match(TestCaseModification testCaseModification) {
        // In this pattern, developers use `@Test(expected=ExpectedException.class)`
        // to check whether expected exception is thrown.
        // So, there must be `@Test(expected=ExpectedException.class)` in revised nodes.

        if (testCaseModification.getOriginalNodeClassesWithText().isEmpty()) {
            // original nodes == empty -> add new tests
            return false;
        }

        List<String> revisedNodes = testCaseModification.getRevisedNodeClassesWithText();
        int size = revisedNodes.size();
        for (int i = 0; i < size; i++) {
            String revisedNode = revisedNodes.get(i);
            if (revisedNode.contains("org.eclipse.jdt.core.dom.SimpleName:Test") && (i + 1 < size)) {
                String nodeAfterTest = revisedNodes.get(i + 1);
                if (nodeAfterTest.contains("org.eclipse.jdt.core.dom.MemberValuePair:expected=")) {
                    return true;
                }
            }
        }
        return false;
    }
}

package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

public class ReplaceAtTodoWithTODOPattern {
    public static boolean match(TestCaseModification testCaseModification) {
        boolean atTodo = false;
        for (String originalNode : testCaseModification.getOriginalNodeClassesWithText()) {
            if ((originalNode.startsWith("org.eclipse.jdt.core.dom.TextElement:")
                    || originalNode.startsWith("org.eclipse.jdt.core.dom.TagElement:"))
                    && originalNode.contains("@todo")) {
                atTodo = true;

                break;
            }
        }
        if (!atTodo) {
            return false;
        }
        for (String revisedNode : testCaseModification.getRevisedNodeClassesWithText()) {
            if ((revisedNode.startsWith("org.eclipse.jdt.core.dom.TextElement:")
                    || revisedNode.startsWith("org.eclipse.jdt.core.dom.TagElement:"))
                    && revisedNode.contains("TODO")) {
                return true;
            }
        }
        return false;
    }
}

package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class AddSerialVersionUidPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        return (countNumOfSerialVersionUidDeclaration(testCaseModification.getOriginalNodeClassesWithText()) == 0
                    && 0 < countNumOfSerialVersionUidDeclaration(testCaseModification.getRevisedNodeClassesWithText()));
    }

    static private int countNumOfSerialVersionUidDeclaration(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.FieldDeclaration:private static final long serialVersionUID")) {
                cnt++;
            }
        }
        return cnt;
    }
}

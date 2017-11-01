package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class UseDiamondOperatorPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        return (countNumOfDiamondOperator(testCaseModification.getOriginalNodeClassesWithText()) == 0
                && 0 < countNumOfDiamondOperator(testCaseModification.getRevisedNodeClassesWithText()));
    }

    static private int countNumOfDiamondOperator(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.ClassInstanceCreation:")
                    && node.endsWith("&lt;&gt;()")) { // &lt;&gt; == `<>`
                cnt++;
            }
        }
        return cnt;
    }
}

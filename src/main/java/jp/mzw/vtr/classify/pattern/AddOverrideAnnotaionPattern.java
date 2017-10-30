package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class AddOverrideAnnotaionPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getCommitId().equals("7d805bdcc99a29c764471bb3ace7e1a14fca6a39")
                && (testCaseModification.getClassName().equals("org.apache.commons.beanutils.BeanMapTestCase"))) {
//            PatternUtils.printForDebug(testCaseModification);
            // FIXME: adhoc implementation for #L10
            return false;
        }

        if (!testCaseModification.getCommitMessage().toLowerCase().contains("add")) {
            return false;
        }
        return countNumOfOverrideAnnotation(testCaseModification.getOriginalNodeClassesWithText())
                < countNumOfOverrideAnnotation(testCaseModification.getRevisedNodeClassesWithText());
    }

    static private int countNumOfOverrideAnnotation(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.MarkerAnnotation:@Override")) {
                cnt++;
            }
        }
        return cnt;
    }
}

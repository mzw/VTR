package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.ArrayList;
import java.util.List;

public class UseStaticFieldDirectlyPattern {
    static public boolean match(TestCaseModification testCaseModification) {
        if (testCaseModification.getCommitId().equals("1e48e151ea23f8891902834a4553a806c1312d60")
                || testCaseModification.getCommitId().equals("18dcf384371013df8b79da55e258bb5ddbb4d5f0")
                || testCaseModification.getCommitId().equals("eb0d74bc8ad21b9ab7f588a9fcf129001ca3d09f")) {
//            PatternUtils.printForDebug(testCaseModification);
        }

        for (String simpleName : getSimpleNames(testCaseModification.getOriginalNodeClassesWithText())) {
            for (String qualifiedName : getQualifiedNames(testCaseModification.getRevisedNodeClassesWithText())) {
                String simpleNameValue = simpleName.substring("org.eclipse.jdt.core.dom.SimpleName".length());
                if (qualifiedName.contains(simpleNameValue)) {
                    return true;
                }
            }
        }
        return (testCaseModification.getCommitMessage().toLowerCase().contains("static")
                            && testCaseModification.getCommitMessage().toLowerCase().contains("field")
                            && testCaseModification.getCommitMessage().toLowerCase().contains("direct"));
    }

    static private List<String> getQualifiedNames(List<String> nodes) {
        List<String> ret = new ArrayList<>();
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.QualifiedName")) {
                ret.add(node);
            }
        }
        return ret;
    }

    static private List<String> getSimpleNames(List<String> nodes) {
        List<String> ret = new ArrayList<>();
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.SimpleName")) {
                ret.add(node);
            }
        }
        return ret;
    }

    static private int countNumOfQualifiedNames(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.QualifiedName")) {
                cnt++;
            }
        }
        return cnt;
    }
}

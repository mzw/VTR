package jp.mzw.vtr.classify.pattern;

import jp.mzw.vtr.detect.TestCaseModification;

import java.util.List;

public class PatternUtils {
    public static void printForDebug(TestCaseModification testCaseModification) {
        System.out.println("================== test case modification name ==================");
        System.out.println(testCaseModification.getMethodName());
        System.out.println("================== commit message ==================");
        System.out.println(testCaseModification.getCommitMessage());
        System.out.println("================== original nodes ==================");
        for (String originNode : testCaseModification.getOriginalNodeClassesWithText()) {
            System.out.println(originNode);
        }
        System.out.println("================== revised nodes ==================");
        for (String originNode : testCaseModification.getRevisedNodeClassesWithText()) {
            System.out.println(originNode);
        }
    }

    static protected boolean sameAstNodes(List<String> originalNodes, List<String> revisedNodes) {
        if (originalNodes.size() != revisedNodes.size()) {
            return false;
        }
        for (int i = 0; i < originalNodes.size(); i++) {
            String originalNode = originalNodes.get(i);
            String revisedNode = revisedNodes.get(i);
            if (!originalNode.equals(revisedNode)) {
                return false;
            }
        }
        return true;
    }

    static protected int countNumOfCloseMethodInvocation(List<String> nodes) {
        int cnt = 0;
        for (String node : nodes) {
            if (node.startsWith("org.eclipse.jdt.core.dom.MethodInvocation:") && node.endsWith("close()")) {
                cnt++;
            }
        }
        return cnt;
    }

}

package jp.mzw.vtr.cluster.gumtreediff;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.matchers.heuristic.LcsMatcher;
import com.github.gumtreediff.matchers.heuristic.XyBottomUpMatcher;
import com.github.gumtreediff.tree.ITree;

import com.github.gumtreediff.tree.TreeUtils;
import jp.mzw.vtr.maven.TestCase;

import java.util.List;

public class GumTreeEngine {

    public GumTreeEngine() {
        super();
    }

    protected List<Action> getEditActions(TestCase prevTC, TestCase curTC) {
        JdtVisitor prevJdtVisitor = new JdtVisitor();
        prevTC.getMethodDeclaration().accept(prevJdtVisitor);
        ITree prevITree = prevJdtVisitor.getTreeContext().getRoot();
        prevITree.refresh();;
        TreeUtils.preOrderNumbering(prevITree);
        if (prevTC.getName().equals("testLoad")) {
            System.out.println("prevMethod");
            System.out.println(prevTC.getMethodDeclaration());
            System.out.println("prevITree");
            System.out.println(prevITree.toShortString());
        }

        JdtVisitor curJdtVisitor = new JdtVisitor();
        curTC.getMethodDeclaration().accept(curJdtVisitor);
        ITree curITree = curJdtVisitor.getTreeContext().getRoot();
        curITree.refresh();
        TreeUtils.preOrderNumbering(curITree);
        if (curTC.getName().equals("testLoad")) {
            System.out.println("curITree");
            System.out.println(curTC.getMethodDeclaration());
            System.out.println("curITree");
            System.out.println(curITree.toShortString());
        }

        // You can use any matcher you like.
        Matcher matcher = new XyBottomUpMatcher(prevITree, curITree, new MappingStore());
//        Matcher matcher = new LcsMatcher(prevITree, curITree, new MappingStore());
//        Matcher matcher = Matchers.getInstance().getMatcher(prevITree, curITree);
        matcher.match();
        ActionGenerator generator = new ActionGenerator(prevITree, curITree, matcher.getMappings());
        generator.generate();
        if (curTC.getName().equals("testLoad")) {
            for (Action action : generator.getActions()) {
//                System.out.println("Action: " + action);
            }
        }

        return generator.getActions();
    }

}

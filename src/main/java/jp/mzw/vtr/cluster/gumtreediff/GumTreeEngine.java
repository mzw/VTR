package jp.mzw.vtr.cluster.gumtreediff;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.matchers.heuristic.LcsMatcher;
import com.github.gumtreediff.tree.ITree;

import com.github.gumtreediff.tree.TreeUtils;
import jp.mzw.vtr.maven.TestCase;

import java.util.List;

public class GumTreeEngine {

    public GumTreeEngine() {
        super();
    }

    protected List<Action> getEditActions(TestCase prevTC, TestCase curTC) {
        JdtVisitor jdtVisitor = new JdtVisitor();

        prevTC.getMethodDeclaration().accept(jdtVisitor);
        ITree prevITree = jdtVisitor.getTreeContext().getRoot();
        prevITree.refresh();;
        TreeUtils.postOrderNumbering(prevITree);

        jdtVisitor = new JdtVisitor();
        curTC.getMethodDeclaration().accept(jdtVisitor);
        ITree curITree = jdtVisitor.getTreeContext().getRoot();
        curITree.refresh();
        TreeUtils.postOrderNumbering(curITree);

        // You can use any matcher you like.
//        LcsMatcher matcher = new LcsMatcher(prevITree, curITree, new MappingStore());
        Matcher matcher = Matchers.getInstance().getMatcher(prevITree, curITree);
        matcher.match();
        ActionGenerator generator = new ActionGenerator(prevITree, curITree, matcher.getMappings());
        generator.generate();
        return generator.getActions();
    }
}

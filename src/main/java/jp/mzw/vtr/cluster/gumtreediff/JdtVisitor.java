package jp.mzw.vtr.cluster.gumtreediff;

import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayDeque;
import java.util.Deque;


public class JdtVisitor extends ASTVisitor {
    protected TreeContext context = new TreeContext();

    private Deque<ITree> trees = new ArrayDeque<>();

    public JdtVisitor() {
        super(true);
    }

    public TreeContext getTreeContext() {
        return context;
    }

    protected void pushNode(ASTNode n, String label) {
        int type = n.getNodeType();
        String typeName = n.getClass().getSimpleName();
        push(type, typeName, label, n.getStartPosition(), n.getLength());
    }

    protected void pushFakeNode(EntityType n, int startPosition, int length) {
        int type = -n.ordinal(); // Fake types have negative types (but does it matter ?)
        String typeName = n.name();
        push(type, typeName, "", startPosition, length);
    }

    private void push(int type, String typeName, String label, int startPosition, int length) {
        ITree t = context.createTree(type, label, typeName);
        t.setPos(startPosition);
        t.setLength(length);

        if (trees.isEmpty())
            context.setRoot(t);
        else {
            ITree parent = trees.peek();
            t.setParentAndUpdateChildren(parent);
        }

        trees.push(t);
    }

    protected ITree getCurrentParent() {
        return trees.peek();
    }

    protected void popNode() {
        trees.pop();
    }

    @Override
    public void preVisit(ASTNode n) {
        pushNode(n, getLabel(n));
    }

    protected String getLabel(ASTNode n) {
        if (n instanceof Name) return ((Name) n).getFullyQualifiedName();
        if (n instanceof Type) return n.toString();
        if (n instanceof Modifier) return n.toString();
        if (n instanceof StringLiteral) return ((StringLiteral) n).getEscapedValue();
        if (n instanceof NumberLiteral) return ((NumberLiteral) n).getToken();
        if (n instanceof CharacterLiteral) return ((CharacterLiteral) n).getEscapedValue();
        if (n instanceof BooleanLiteral) return ((BooleanLiteral) n).toString();
        if (n instanceof InfixExpression) return ((InfixExpression) n).getOperator().toString();
        if (n instanceof PrefixExpression) return ((PrefixExpression) n).getOperator().toString();
        if (n instanceof PostfixExpression) return ((PostfixExpression) n).getOperator().toString();
        if (n instanceof Assignment) return ((Assignment) n).getOperator().toString();
        if (n instanceof TextElement) return n.toString();
        if (n instanceof TagElement) return ((TagElement) n).getTagName();

        return "";
    }

    @Override
    public boolean visit(TagElement e) {
        return true;
    }

    @Override
    public boolean visit(QualifiedName name) {
        return false;
    }

    @Override
    public void postVisit(ASTNode n) {
        popNode();
    }
}

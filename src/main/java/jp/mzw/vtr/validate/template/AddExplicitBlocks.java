package jp.mzw.vtr.validate.template;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.AllElementsFindVisitor;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 2017/02/06.
 */
public class AddExplicitBlocks extends SimpleValidatorBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(AddExplicitBlocks.class);

    public AddExplicitBlocks(Project project) {
        super(project);
    }

    @Override
    protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
        final List<ASTNode> ret = new ArrayList<>();
        AllElementsFindVisitor visitor = new AllElementsFindVisitor();
        tc.getMethodDeclaration().accept(visitor);
        List<ASTNode> targets = visitor.getNodes();
        for (ASTNode node: targets) {
            if (targetIfStatement(node)) {
                ret.add((IfStatement) node);
            } else if (targetForStatement(node)) {
                ret.add((ForStatement) node);
            } else if (targetWhileStatement(node)) {
                ret.add((WhileStatement) node);
            }
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
        // prepare
        CompilationUnit cu = tc.getCompilationUnit();
        AST ast = cu.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);
        // detect
        for (ASTNode node: detect(commit, tc, results)) {
            if (node instanceof IfStatement) {
                IfStatement target = (IfStatement) node;
                IfStatement replace = addExplicitBlocksToIfStatement(ast, target);
                rewrite.replace(target, replace, null);
            } else if (node instanceof ForStatement) {
                ForStatement target = (ForStatement) node;
                ForStatement replace = (ForStatement) ASTNode.copySubtree(ast, target);
                Block body = ast.newBlock();
                body.statements().add(replace.getBody());
                replace.setBody(body);
                rewrite.replace(target, replace, null);
            } else if (node instanceof WhileStatement) {
                WhileStatement target = (WhileStatement) node;
                WhileStatement replace = (WhileStatement) ASTNode.copySubtree(ast, target);
                Block body = ast.newBlock();
                body.statements().add(replace.getBody());
                replace.setBody(body);
                rewrite.replace(target, replace, null);
            }
        }
        // modify
        Document document = new Document(origin);
        TextEdit edit = rewrite.rewriteAST(document, null);
        edit.apply(document);
        return document.get();
    }

    private boolean targetIfStatement(ASTNode node) {
        if (!(node instanceof IfStatement)) {
            return false;
        }
        IfStatement ifStatement = (IfStatement) node;
        Statement thenStatement = ifStatement.getThenStatement();
        if (!(thenStatement instanceof Block)) {
            return true;
        }
        Statement elseStatement = ifStatement.getElseStatement();
        return ((elseStatement != null) && !(elseStatement instanceof Block) && !(elseStatement instanceof IfStatement));
    }
    private boolean targetForStatement(ASTNode node) {
        if (!(node instanceof ForStatement)) {
            return false;
        }
        ForStatement forStatement = (ForStatement) node;
        return !(forStatement.getBody() instanceof Block);
    }
    private boolean targetWhileStatement(ASTNode node) {
        if (!(node instanceof WhileStatement)) {
            return false;
        }
        WhileStatement whileStatement = (WhileStatement) node;
        return !(whileStatement.getBody() instanceof Block);
    }

    private IfStatement addExplicitBlocksToIfStatement(AST ast, IfStatement target) {
        IfStatement replace = (IfStatement) ASTNode.copySubtree(ast, target);
        // add blocks to then statement
        Statement thenStatement = replace.getThenStatement();
        if (!(thenStatement instanceof Block)) {
            Block thenBlock = ast.newBlock();
            thenBlock.statements().add((Statement) ASTNode.copySubtree(ast, thenStatement));
            replace.setThenStatement(thenBlock);
        }
        // add blocks to else statement
        Statement elseStatement = replace.getElseStatement();
        if ((elseStatement != null) && !(elseStatement instanceof Block)) {
            if (elseStatement instanceof IfStatement) {
                // else-if statement
                replace.setElseStatement(addExplicitBlocksToIfStatement(ast, (IfStatement) elseStatement));
            } else {
                // else statement
                Block elseBlock = ast.newBlock();
                elseBlock.statements().add((Statement) ASTNode.copySubtree(ast, elseStatement));
                replace.setElseStatement(elseBlock);
            }
        }
        return replace;
    }
}

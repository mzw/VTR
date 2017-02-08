package jp.mzw.vtr.validate.output;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;
import org.eclipse.jdt.core.CompletionContext;
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
 * Created by TK on 2017/02/08.
 */
public class RemoveUnnecessaryCasts extends SimpleValidatorBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(RemoveUnnecessaryCasts.class);

    public RemoveUnnecessaryCasts(Project project) {
        super(project);
    }

    @Override
    public List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
        final List<ASTNode> ret = new ArrayList<>();
        final List<CastExpression> targets = new ArrayList<>();
        tc.getMethodDeclaration().accept(new ASTVisitor() {
            @Override
            public boolean visit(CastExpression node) {
                targets.add(node);
                return super.visit(node);
            }
        });
        for (CastExpression target : targets) {
            if (unnecessaryCast(target)) {
                ret.add(target);
            }
        }

        return ret;
    }

    @Override
    public String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
        // prepare
        CompilationUnit cu = tc.getCompilationUnit();
        AST ast = cu.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);
        // detect
        for (ASTNode node : detect(commit, tc, results)) {
            CastExpression target = (CastExpression) node;
            Expression replace = (Expression) ASTNode.copySubtree(ast, target.getExpression());
            if (target.getParent() instanceof ParenthesizedExpression) {
                rewrite.replace(target.getParent(), replace, null);
            } else {
                rewrite.replace(target, replace, null);
            }
        }
        // modify
        Document document = new Document(origin);
        TextEdit edit = rewrite.rewriteAST(document, null);
        edit.apply(document);
        return document.get();
    }

    public static boolean unnecessaryCast(CastExpression cast) {
        return cast.getType().toString().equals(cast.getExpression().resolveTypeBinding().getName());
    }
}

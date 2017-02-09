package jp.mzw.vtr.validate.suppress_warnings;

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
import java.util.Arrays;
import java.util.List;

/**
 * Created by TK on 2017/02/07.
 */
public class IntroduceAutoBoxing extends SimpleValidatorBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(IntroduceAutoBoxing.class);

    public IntroduceAutoBoxing(Project project) {
        super(project);
    }

    protected static final String[] ARRAY_WRAPPERS = {
            "Boolean",
            "Byte",
            "Character",
            "Double",
            "Float",
            "Integer",
            "Long",
            "Short"
    };

    protected static final List<String> WRAPPERS = Arrays.asList(ARRAY_WRAPPERS);

    @Override
    public List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
        final List<ASTNode> ret = new ArrayList<>();
        AllElementsFindVisitor visitor = new AllElementsFindVisitor();
        tc.getMethodDeclaration().accept(visitor);
        final List<ASTNode> nodes = visitor.getNodes();
        for (ASTNode node : nodes) {
            if (!(node instanceof MethodInvocation)) {
                continue;
            }
            MethodInvocation method = (MethodInvocation) node;
            if (((MethodInvocation) node).getExpression() == null) {
                continue;
            }
            if (WRAPPERS.contains(method.getExpression().toString()) &&
                    "valueOf".equals(method.getName().toString())) {
                ret.add(method);
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
            MethodInvocation target = (MethodInvocation) node;
            // TODO: Integerで引数の数が2の場合は未対応
            if (target.arguments().size() > 1) {
                continue;
            }
            if (!(target.arguments().get(0) instanceof Expression)) {
                continue;
            }
            Expression argument = (Expression) target.arguments().get(0);
            Expression replace = (Expression) ASTNode.copySubtree(ast, argument);
            rewrite.replace(target, replace, null);
        }
        // modify
        Document document = new Document(origin);
        TextEdit edit = rewrite.rewriteAST(document, null);
        edit.apply(document);
        return document.get();
    }
}

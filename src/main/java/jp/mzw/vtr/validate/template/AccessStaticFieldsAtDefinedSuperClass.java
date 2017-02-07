package jp.mzw.vtr.validate.template;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
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

import javax.naming.MalformedLinkException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 2017/02/07.
 */
public class AccessStaticFieldsAtDefinedSuperClass extends SimpleValidatorBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(AccessStaticFieldsAtDefinedSuperClass.class);

    public AccessStaticFieldsAtDefinedSuperClass(Project project) {
        super(project);
    }

    @Override
    public List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
        final List<ASTNode> ret = new ArrayList<>();
        final List<QualifiedName> targets = new ArrayList<>();
        tc.getMethodDeclaration().accept(new ASTVisitor() {
            @Override
            public boolean visit(QualifiedName node) {
                targets.add(node);
                return super.visit(node);
            }
        });

        for (QualifiedName target : targets) {
            if (!target.getQualifier().toString().equals(definedSuperClassName(target))) {
                ret.add(target);
            }
        }
        return ret;
    }

    @Override
    public String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException{
        // prepare
        CompilationUnit cu = tc.getCompilationUnit();
        AST ast = cu.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);
        // detect
        for (ASTNode node: detect(commit, tc, results)) {
            QualifiedName target = (QualifiedName) node;
            QualifiedName replace = (QualifiedName) ASTNode.copySubtree(ast, target);
            String definedSuperClassName = definedSuperClassName(target);
            replace.setQualifier(ast.newName(definedSuperClassName));
            rewrite.replace(target, replace, null);
        }
        // modify
        Document document = new Document(origin);
        TextEdit edit = rewrite.rewriteAST(document, null);
        edit.apply(document);
        return document.get();
    }

    private String definedSuperClassName(QualifiedName target) {
        ITypeBinding binding = target.getQualifier().resolveTypeBinding().getSuperclass();
        while (binding != null) {
            for (IVariableBinding variable : binding.getDeclaredFields()) {
                if (variable.getName().equals(target.getName().toString())) {
                    return binding.getName();
                }
            }
            binding = binding.getSuperclass();
        }
        return target.getQualifier().toString();
    }
}
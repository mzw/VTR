package jp.mzw.vtr.validate.template;

import jp.mzw.vtr.core.Project;
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
 * Created by TK on 2017/02/02.
 */
public class AddOverrideAnnotation extends SimpleValidatorBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(AddOverrideAnnotation.class);

    public AddOverrideAnnotation(Project project) {
        super(project);
    }

    @Override
    protected List<ASTNode> detect(TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
        final List<ASTNode> ret = new ArrayList<>();
        IMethodBinding thisMethodBinding = tc.getMethodDeclaration().resolveBinding();
        ITypeBinding typeBinding = thisMethodBinding.getDeclaringClass();
        while (typeBinding != null) {
            boolean overrides = false;
            for (IMethodBinding method: typeBinding.getDeclaredMethods()) {
                if (thisMethodBinding.overrides(method)) {
                    overrides = true;
                    ret.add(tc.getMethodDeclaration());
                    break;
                }
            }
            if (overrides) {
                break;
            }
            typeBinding = typeBinding.getSuperclass();
        }
        return ret;
    }

    @Override
    protected String getModified(String origin, TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
        // prepare
        CompilationUnit cu = tc.getCompilationUnit();
        AST ast = cu.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);
        // detect
        for (ASTNode node: detect(tc)) {
            MethodDeclaration target = (MethodDeclaration) node;
            MethodDeclaration replace = (MethodDeclaration) ASTNode.copySubtree(ast, node);
            // create new annotation
            MarkerAnnotation annotation = ast.newMarkerAnnotation();
            annotation.setTypeName(ast.newName("Override"));
            // insert annotation
            replace.modifiers().add(0, annotation);
            rewrite.replace(target, replace, null);
        }
        // modify
        Document document = new Document(origin);
        TextEdit edit = rewrite.rewriteAST(document, null);
        edit.apply(document);
        return document.get();
    }
}

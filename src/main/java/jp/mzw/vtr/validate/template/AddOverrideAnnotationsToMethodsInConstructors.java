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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 2017/02/08.
 */
public class AddOverrideAnnotationsToMethodsInConstructors extends AddOverrideAnnotationsBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(AddOverrideAnnotationsToMethodsInConstructors.class);

    public AddOverrideAnnotationsToMethodsInConstructors(Project project) {
        super(project);
    }

    @Override
    public List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
        final List<ASTNode> ret = new ArrayList<>();
        final List<ClassInstanceCreation> targets = new ArrayList<>();

        tc.getMethodDeclaration().accept(new ASTVisitor() {
            @Override
            public boolean visit(ClassInstanceCreation node) {
                targets.add(node);
                return super.visit(node);
            }
        });

        for (ClassInstanceCreation target : targets) {
            AnonymousClassDeclaration acd = target.getAnonymousClassDeclaration();
            if (acd != null) {
                for (Object arg : acd.bodyDeclarations()) {
                    if (!(arg instanceof MethodDeclaration)) {
                        continue;
                    }
                    MethodDeclaration method = (MethodDeclaration) arg;
                    if (!(hasOverrideAnnotation(method))) {
                        ret.add(method);
                    }
                }
            }
        }
        return ret;
    }
}

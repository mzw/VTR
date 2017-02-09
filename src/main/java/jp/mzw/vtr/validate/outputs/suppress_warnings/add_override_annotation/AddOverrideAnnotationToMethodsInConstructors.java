package jp.mzw.vtr.validate.outputs.suppress_warnings.add_override_annotation;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 2017/02/08.
 */
public class AddOverrideAnnotationToMethodsInConstructors extends AddOverrideAnnotationBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(AddOverrideAnnotationToMethodsInConstructors.class);

	public AddOverrideAnnotationToMethodsInConstructors(Project project) {
		super(project);
	}

	@Override
	public List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
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

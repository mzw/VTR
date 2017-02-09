package jp.mzw.vtr.validate.outputs.suppress_warnings.add_override_annotation;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TK on 2017/02/02.
 */
public class AddOverrideAnnotationToTestCase extends AddOverrideAnnotationBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(AddOverrideAnnotationToTestCase.class);

	public AddOverrideAnnotationToTestCase(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		if (overrideMethod(tc.getMethodDeclaration()) && !hasOverrideAnnotation(tc.getMethodDeclaration())) {
			ret.add(tc.getMethodDeclaration());
		}
		return ret;
	}
}

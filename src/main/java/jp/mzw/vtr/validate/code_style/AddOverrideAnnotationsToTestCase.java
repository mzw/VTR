package jp.mzw.vtr.validate.code_style;

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
public class AddOverrideAnnotationsToTestCase extends AddOverrideAnnotationsBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(AddOverrideAnnotationsToTestCase.class);

	public AddOverrideAnnotationsToTestCase(Project project) {
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

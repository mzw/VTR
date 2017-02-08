package jp.mzw.vtr.validate.coding_style;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;
import jp.mzw.vtr.validate.ValidationResult;

public class FormatCode extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(FormatCode.class);

	public FormatCode(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		String origin = FileUtils.readFileToString(tc.getTestFile());
		String modified = format(origin);
		List<String> patch = genPatch(getTestCaseSource(origin, tc.getName()), getTestCaseSource(modified, tc.getName()), tc.getTestFile(), tc.getTestFile(),
				(tc.getStartLineNumber() - 1) * -1);
		if (patch.size() != 0) {
			ret.add(tc.getMethodDeclaration());
		}
		return ret;
	}

	@Override
	public void generate(ValidationResult result) {
		try {
			TestCase tc = getTestCase(result, projectDir);
			String origin = FileUtils.readFileToString(tc.getTestFile());
			String modified = getModified(origin, null, tc, null);
			List<String> patch = genPatch(getTestCaseSource(origin, tc.getName()), getTestCaseSource(modified, tc.getName()), tc.getTestFile(),
					tc.getTestFile(), (tc.getStartLineNumber() - 1) * -1);
			output(result, tc, patch);
		} catch (IOException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}

	@Override
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		return format(origin);
	}

}

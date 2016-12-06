package jp.mzw.vtr.validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.TryStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandleExpectedExecptionsProperly extends ValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(HandleExpectedExecptionsProperly.class);

	public HandleExpectedExecptionsProperly(Project project) {
		super(project);
	}

	@Override
	public void onCheckout(Commit commit) {
		try {
			for (TestSuite ts : MavenUtils.getTestSuites(this.projectDir)) {
				for (TestCase tc : ts.getTestCases()) {
					if (this.dupulicates.contains(tc.getFullName())) {
						continue;
					}
					detect(commit, tc);
				}
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to checkout: {}", commit.getId());
		}
	}

	private void detect(Commit commit, TestCase tc) throws IOException {
		String content = FileUtils.readFileToString(tc.getTestFile());
		// try {...Assert...} catch(FooException e) { /* expected */ }
		for (ASTNode node : tc.getNodes()) {
			if (node instanceof TryStatement) {
				TryStatement tryStatement = (TryStatement) node;
				if (this.hasAssertMethodInvocation(tryStatement.getBody())) {
					catchExpectedException(commit, tc, content, (TryStatement) node);
				}
			}
		}
	}

	private void catchExpectedException(Commit commit, TestCase tc, String content, TryStatement tryStatement) {
		// ...} catch (FooException e) {...expect...}
		for (Object object : tryStatement.catchClauses()) {
			CatchClause cc = (CatchClause) object;
			for (Comment comment : getComments(tc, cc)) {
				String[] split = getContent(content, comment).split(" ");
				for (String expect : split) {
					if (expect.contains("expect")) {
						// Prevent duplicated detection
						this.dupulicates.add(tc.getFullName());
						// Register detection result
						ValidationResult vr = new ValidationResult(this.projectId, commit, tc,
								tc.getStartLineNumber(cc), tc.getEndLineNumber(cc), this);
						this.validationResultList.add(vr);
						LOGGER.info("Detect {} in {} at {}", this.getClass().getName(), tc.getFullName(), commit.getId());
					}
				}
			}
		}
	}

	private List<Comment> getComments(TestCase tc, ASTNode node) {
		int start = tc.getStartLineNumber(node);
		int end = tc.getEndLineNumber(node);
		List<Comment> comments = new ArrayList<>();
		for (Object object : tc.getCompilationUnit().getCommentList()) {
			Comment comment = (Comment) object;
			if (start <= tc.getStartLineNumber(comment) && tc.getEndLineNumber(comment) <= end) {
				comments.add(comment);
			}
		}
		return comments;
	}

	private String getContent(String content, Comment comment) {
		char[] charArray = content.toCharArray();
		char[] ret = new char[comment.getLength()];
		for (int offset = 0; offset < comment.getLength(); offset++) {
			ret[offset] = charArray[comment.getStartPosition() + offset];
		}
		return String.valueOf(ret);
	}

	@Override
	public void generate(ValidationResult result) {
		// @Test(expected=FooException.class)
	}

}

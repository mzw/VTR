package jp.mzw.vtr.validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

public class DoNotSwallowTestErrorsSilently extends ValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(DoNotSwallowTestErrorsSilently.class);

	private List<String> invalidTestCases;

	public DoNotSwallowTestErrorsSilently(Project project) {
		super(project);
		invalidTestCases = new ArrayList<>();
	}

	@Override
	public void onCheckout(Commit commit) {
		try {
			List<TestSuite> testSuites = MavenUtils.getTestSuites(this.projectDir);
			for (TestSuite ts : testSuites) {
				for (TestCase tc : ts.getTestCases()) {
					validate(commit, tc);
				}
			}
		} catch (IOException e) {
			LOGGER.warn(e.getMessage());
		}
	}

	/**
	 * Detect invalid test cases according to this pattern
	 * 
	 * @param commit
	 * @param tc
	 */
	protected void validate(Commit commit, TestCase tc) {
		if (this.invalidTestCases.contains(tc.getFullName())) {
			return;
		}
		for (ASTNode node : tc.getNodes()) {
			if (node instanceof TryStatement) {
				TryStatement tryStatement = (TryStatement) node;
				for (Object object : tryStatement.catchClauses()) {
					if (object instanceof CatchClause) {
						CatchClause catchClause = (CatchClause) object;
						if (hasAssertMethodInvocation(((CatchClause) catchClause).getBody())) {
							LOGGER.info("Find try-statement but its catch-block contains assertion(s): {} @ {}", tc.getFullName(), commit.getId());
						} else {
							// Prevent duplicated detection
							this.invalidTestCases.add(tc.getFullName());
							// Register detection result
							ValidationResult vr = new ValidationResult(this.projectId, commit, tc, tc.getStartLineNumber(catchClause),
									tc.getEndLineNumber(catchClause), this);
							this.validationResultList.add(vr);
						}
					}
				}
			}
		}
	}

	/**
	 * Determine whether given node has JUnit assert method invocation
	 * 
	 * @param node
	 * @return
	 */
	protected boolean hasAssertMethodInvocation(ASTNode node) {
		if (node instanceof MethodInvocation) {
			MethodInvocation method = (MethodInvocation) node;
			for (String name : JUNIT_ASSERT_METHODS) {
				if (name.equals(method.getName().toString())) {
					return true;
				}
			}
		}
		for (Object child : MavenUtils.getChildren(node)) {
			boolean has = hasAssertMethodInvocation((ASTNode) child);
			if (has) {
				return true;
			}
		}
		return false;
	}

	/**
	 * List of JUnit assert method names
	 */
	public static final String[] JUNIT_ASSERT_METHODS = { "assertArrayEquals", "assertEquals", "assertFalse", "assertNotNull", "assertNotSame", "assertNull",
			"assertSame", "assertThat", "assertTrue", "fail", };

}

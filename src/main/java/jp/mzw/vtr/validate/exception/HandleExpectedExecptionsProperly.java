package jp.mzw.vtr.validate.exception;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;
import jp.mzw.vtr.validate.RewriterUtils;
import jp.mzw.vtr.validate.ValidationResult;
import jp.mzw.vtr.validate.ValidatorBase;
import jp.mzw.vtr.validate.ValidatorUtils;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
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

	/**
	 * try {...Assert...} catch(FooException e) {...expected...}
	 * 
	 * @param commit
	 * @param tc
	 * @throws IOException
	 */
	private void detect(Commit commit, TestCase tc) throws IOException {
		String content = FileUtils.readFileToString(tc.getTestFile());
		for (ASTNode node : tc.getNodes()) {
			if (node instanceof TryStatement) {
				TryStatement tryStatement = (TryStatement) node;
				if (ValidatorUtils.hasAssertMethodInvocation(tryStatement.getBody())) {
					List<CatchClause> ccList = detectExpectedException(tc, content, (TryStatement) node);
					for (CatchClause cc : ccList) {
						// Prevent duplicated detection
						this.dupulicates.add(tc.getFullName());
						// Register detection result
						ValidationResult vr = new ValidationResult(this.projectId, commit, tc, tc.getStartLineNumber(cc), tc.getEndLineNumber(cc), this);
						this.validationResultList.add(vr);
						LOGGER.info("Detect {} in {} at {}", this.getClass().getName(), tc.getFullName(), commit.getId());
					}
				}
			}
		}
	}

	/**
	 * ...} catch (FooException e) {...expect...}
	 * 
	 * @param commit
	 * @param tc
	 * @param content
	 * @param tryStatement
	 */
	private List<CatchClause> detectExpectedException(TestCase tc, String content, TryStatement tryStatement) {
		List<CatchClause> ret = new ArrayList<>();
		for (Object object : tryStatement.catchClauses()) {
			CatchClause cc = (CatchClause) object;
			for (Comment comment : ValidatorUtils.getComments(tc, cc)) {
				String[] split = ValidatorUtils.getCommentContent(content, comment).split(" ");
				for (String expect : split) {
					if (expect.contains("expect")) {
						ret.add(cc);
					}
				}
			}
		}
		return ret;
	}

	@Override
	public void generate(ValidationResult result) {
		try {
			TestCase tc = getTestCase(result);
			String origin = FileUtils.readFileToString(tc.getTestFile());
			CatchClause expects = RewriterUtils.getCatchClause(tc, result.getStartLineNumber(), result.getEndLineNumber());
			String modified = setExpectedExceptionAtTestAnnotation(origin, tc, expects);
			List<String> patch = genPatch(origin, modified, tc.getTestFile(), tc.getTestFile());
			output(result, tc, patch);
		} catch (IOException | ParseException | GitAPIException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}

	/**
	 * AtTest(expected=FooException.class) public void testFoo() throws
	 * FooException {...}
	 * 
	 * @param origin
	 * @param tc
	 * @return
	 * @throws MalformedTreeException
	 * @throws BadLocationException
	 */
	private String setExpectedExceptionAtTestAnnotation(String origin, TestCase tc, CatchClause cc) throws MalformedTreeException, BadLocationException {
		ASTRewrite rewriter = ASTRewrite.create(tc.getCompilationUnit().getAST());
		// public void testFoo throws "exception, exception, ..., exception"
		List<ASTNode> nodes = tc.getNodes();
		List<SimpleType> exceptions = ValidatorUtils.getThrowedExceptions(nodes);
		List<CatchClause> expects = detectExpectedException(tc, origin, (TryStatement) cc.getParent());
		for (CatchClause expect : expects) {
			RewriterUtils.insertException(rewriter, expect, exceptions, tc.getMethodDeclaration());
		}
		// AtTest(expected=FooException.class)
		Annotation annot = ValidatorUtils.getTestAnnotation(tc);
		insertExpectedExceptions(rewriter, expects, annot, tc.getMethodDeclaration());
		// Remove unnecessary catches
		RewriterUtils.removeCatches(rewriter, (TryStatement) cc.getParent(), expects, tc.getMethodDeclaration());
		// Rewrite
		org.eclipse.jface.text.Document document = new org.eclipse.jface.text.Document(origin);
		TextEdit edit = rewriter.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

	private void insertExpectedExceptions(ASTRewrite rewriter, List<CatchClause> expects, Annotation annot, MethodDeclaration method) {
		StringBuilder classes = new StringBuilder();
		String delim = "";
		for (CatchClause cc : expects) {
			classes.append(delim).append(cc.getException().getType()).append(".class");
			delim = ", ";
		}
		String code = annot.toString();
		if (annot.getProperty("expected") == null) {
			code += "(expected = " + classes.toString() + ")";
		} else {
			code.replace("expected = ", "expected = " + classes.toString());
		}
		ASTNode placeholder = rewriter.createStringPlaceholder(code, ASTNode.ANNOTATION_TYPE_DECLARATION);
		rewriter.replace(annot, placeholder, null);
	}

}

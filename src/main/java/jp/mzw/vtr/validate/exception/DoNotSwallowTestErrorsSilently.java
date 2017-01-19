package jp.mzw.vtr.validate.exception;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.CLI;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;
import jp.mzw.vtr.validate.ValidationResult;
import jp.mzw.vtr.validate.ValidatorBase;
import jp.mzw.vtr.validate.ValidatorUtils;

public class DoNotSwallowTestErrorsSilently extends ValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(DoNotSwallowTestErrorsSilently.class);

	public DoNotSwallowTestErrorsSilently(Project project) {
		super(project);
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
	 * Detect try statements
	 * 
	 * @param commit
	 * @param tc
	 */
	protected void validate(Commit commit, TestCase tc) {
		if (this.dupulicates.contains(tc.getFullName())) {
			return;
		}
		for (ASTNode node : tc.getNodes()) {
			if (node instanceof TryStatement) {
				validate(commit, tc, (TryStatement) node);
			}
		}
	}

	/**
	 * Detect catch clauses
	 * 
	 * @param commit
	 * @param tc
	 * @param tryStatement
	 */
	protected void validate(Commit commit, TestCase tc, TryStatement tryStatement) {
		for (Object catchClause : tryStatement.catchClauses()) {
			if (catchClause instanceof CatchClause) {
				validate(commit, tc, (CatchClause) catchClause);
			}
		}
	}

	/**
	 * Detect catch clauses without JUnit assert invocations in try statements
	 * 
	 * @param commit
	 * @param tc
	 * @param catchClause
	 */
	protected void validate(Commit commit, TestCase tc, CatchClause catchClause) {
		if (ValidatorUtils.hasAssertMethodInvocation(catchClause.getBody())) {
			LOGGER.info("Find try-statement but its catch-block contains assertion(s): {} @ {}", tc.getFullName(), commit.getId());
		} else {
			// Prevent duplicated detection
			this.dupulicates.add(tc.getFullName());
			// Register detection result
			ValidationResult vr = new ValidationResult(this.projectId, commit, tc, tc.getStartLineNumber(catchClause), tc.getEndLineNumber(catchClause), this);
			this.validationResultList.add(vr);
		}
	}

	@Override
	public void generate(ValidationResult result) {
		try {
			// Pattern
			String pattern = result.getValidatorName();
			// Checkout
			String projectId = result.getProjectId();
			Project project = new Project(projectId).setConfig(CLI.CONFIG_FILENAME);
			CheckoutConductor cc = new CheckoutConductor(project);
			// Commit
			String commitId = result.getCommitId();
			Commit commit = new Commit(commitId, null);
			cc.checkout(commit);
			// Detect test case
			String clazz = result.getTestCaseClassName();
			String method = result.getTestCaseMathodName();
			List<TestSuite> testSuites = MavenUtils.getTestSuites(project.getProjectDir());
			for (TestSuite ts : testSuites) {
				TestCase tc = ts.getTestCaseBy(clazz, method);
				if (tc != null) {
					File file = ts.getTestFile();
					String origin = FileUtils.readFileToString(file);
					List<String> content = genPatch(origin, commit, tc, result.getStartLineNumber(), result.getEndLineNumber());
					if (content != null) {
						File projectDir = new File(project.getOutputDir(), projectId);
						File validateDir = new File(projectDir, ValidatorBase.VALIDATOR_DIRNAME);
						File commitDir = new File(validateDir, commitId);
						File patternDir = new File(commitDir, pattern);
						if (!patternDir.exists()) {
							patternDir.mkdirs();
						}
						File patchFile = new File(patternDir, tc.getFullName() + ".patch");
						FileUtils.writeLines(patchFile, content);
						LOGGER.warn("Succeeded to generate patch: {}", file.getPath());
					}
					break;
				}
			}
		} catch (IOException | ParseException | GitAPIException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}

	protected List<String> genPatch(String origin, Commit commit, TestCase tc, int start, int end) {
		List<ASTNode> nodes = tc.getNodes();
		// public void testFoo throws "exception, exception, ..., exception"
		List<SimpleType> exceptions = new ArrayList<>();
		for (ASTNode node : nodes) {
			if (node instanceof SimpleType && node.getParent() instanceof MethodDeclaration) {
				exceptions.add((SimpleType) node);
			}
		}
		// try { ... } catch {...}
		for (ASTNode node : nodes) {
			if (node instanceof TryStatement) {
				TryCatchExceptions tce = detectTryCatch(commit, tc, (TryStatement) node, start, end).setExceptions(exceptions);
				if (tce.getCatchClause() != null) { // target
					String modified = genModifiedContent(origin, tc, tce);
					if (modified != null) {
						return genPatch(origin, modified, tc.getTestFile(), tc.getTestFile());
					}
					LOGGER.warn("Failed to generate patch file: {} @ {}", tc.getFullName(), commit.getId());
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * Generate modified test case
	 * 
	 * @param origin
	 * @param tc
	 * @param tce
	 * @return
	 */
	private String genModifiedContent(String origin, TestCase tc, TryCatchExceptions tce) {
		try {
			ASTRewrite rewriter = ASTRewrite.create(tc.getCompilationUnit().getAST());
			insertException(rewriter, tce.getCatchClause(), tce.getExceptions(), tc.getMethodDeclaration());
			for (CatchClause other : tce.getOtherCatchClauses()) {
				insertException(rewriter, other, tce.getExceptions(), tc.getMethodDeclaration());
			}
			removeTryStatement(rewriter, tce.getTryStatement());
			// Write
			Document document = new Document(origin);
			TextEdit edit = rewriter.rewriteAST(document, null);
			edit.apply(document);
			return document.get();
		} catch (MalformedTreeException | IllegalArgumentException | BadLocationException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Insert given exception at last of given method's exceptions, if not
	 * existing
	 * 
	 * @param rewriter
	 * @param cc
	 * @param exceptions
	 * @param method
	 */
	private void insertException(ASTRewrite rewriter, CatchClause cc, List<SimpleType> exceptions, MethodDeclaration method) {
		boolean exist = false;
		for (SimpleType exception : exceptions) {
			if (cc.getException().getType().toString().equals(exception.toString())) {
				exist = true;
			}
		}
		if (!exist) {
			ListRewrite lr = rewriter.getListRewrite(method, MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY);
			lr.insertLast(cc.getException().getType(), null);
		}
	}

	/**
	 * Remove given try-statement from given AST
	 * 
	 * @param rewriter
	 *            Representing given AST
	 * @param ts
	 *            Try-statement to be removed
	 */
	private void removeTryStatement(ASTRewrite rewriter, TryStatement ts) {
		rewriter.replace(ts, ts.getBody(), null);
	}

	/**
	 * Detect target try-catch statements
	 * 
	 * @param commit
	 * @param tc
	 * @param tryStatement
	 * @param start
	 * @param end
	 * @return
	 */
	private TryCatchExceptions detectTryCatch(Commit commit, TestCase tc, TryStatement tryStatement, int start, int end) {
		CatchClause target = null;
		List<CatchClause> others = new ArrayList<>();
		for (Object catchClause : tryStatement.catchClauses()) {
			if (catchClause instanceof CatchClause) {
				CatchClause cc = (CatchClause) catchClause;
				if (start == tc.getStartLineNumber(cc) && end == tc.getEndLineNumber(cc)) {
					target = cc;
				} else {
					others.add(cc);
				}
			}
		}
		return new TryCatchExceptions(tryStatement, target).setOthers(others);
	}

	/**
	 * Representing try-catch detection result
	 * 
	 * @author Yuta Maezawa
	 *
	 */
	public static class TryCatchExceptions {
		private TryStatement ts;
		private CatchClause cc;
		private List<CatchClause> others;
		private List<SimpleType> exceptions;

		public TryCatchExceptions(TryStatement ts, CatchClause cc) {
			this.ts = ts;
			this.cc = cc;
			this.others = new ArrayList<>();
			this.exceptions = new ArrayList<>();
		}

		public TryCatchExceptions setExceptions(List<SimpleType> exceptions) {
			this.exceptions.addAll(exceptions);
			return this;
		}

		public List<SimpleType> getExceptions() {
			return this.exceptions;
		}

		public TryCatchExceptions setOthers(List<CatchClause> others) {
			this.others.addAll(others);
			return this;
		}

		public List<CatchClause> getOtherCatchClauses() {
			return this.others;
		}

		public boolean hasOtherCatchClauses() {
			if (this.others.isEmpty()) {
				return false;
			} else {
				return true;
			}
		}

		public TryStatement getTryStatement() {
			return this.ts;
		}

		public CatchClause getCatchClause() {
			return this.cc;
		}
	}

}

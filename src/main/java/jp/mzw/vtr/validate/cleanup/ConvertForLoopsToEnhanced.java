package jp.mzw.vtr.validate.cleanup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.fix.ConvertForLoopOperation;
import org.eclipse.jdt.internal.corext.fix.ConvertLoopFix;
import org.eclipse.jdt.internal.corext.fix.ConvertLoopOperation;
import org.eclipse.jdt.internal.corext.fix.CompilationUnitRewriteOperationsFix.CompilationUnitRewriteOperation;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertForLoopsToEnhanced extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(ConvertForLoopsToEnhanced.class);

	public ConvertForLoopsToEnhanced(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> ret = new ArrayList<>();
		ICleanUpFix fix = ConvertLoopFix.createCleanUp(tc.getCompilationUnit(), true, true, true);
		if (fix == null) {
			return ret;
		}
		for (CompilationUnitRewriteOperation operation : ((ConvertLoopFix) fix).getOperations()) {
			ConvertLoopOperation change = (ConvertLoopOperation) operation;
			ForStatement node = change.getForStatement();
			int start = tc.getCompilationUnit().getLineNumber(node.getStartPosition());
			int end = tc.getCompilationUnit().getLineNumber(node.getStartPosition() + node.getLength());
			if (tc.getStartLineNumber() <= start && end <= tc.getEndLineNumber()) {
				ret.add(node);
			}
		}
		return ret;
	}

	@Override
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		final CompilationUnit cu = tc.getCompilationUnit();
		final AST ast = cu.getAST();
		final ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode detect : detect(commit, tc, results)) {
			ForStatement node = (ForStatement) detect;
			ConvertLoopFix fix = ConvertLoopFix.createConvertForLoopToEnhancedFix(cu, node);
			if (fix == null) {
				continue;
			}
			for (CompilationUnitRewriteOperation operation : fix.getOperations()) {
				if (operation instanceof ConvertForLoopOperation) {
					try {
						EnhancedForStatement statement = ((ConvertForLoopOperation) operation).convert(origin, cu);
						if (statement != null) {
							rewrite.replace(node, statement, null);
						}
					} catch (CoreException e) {
						LOGGER.warn("Failed to convert: {}", e.getMessage());
					}
				} else {
					LOGGER.error("Unknown type operation: {}", operation.getClass());
				}
			}
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

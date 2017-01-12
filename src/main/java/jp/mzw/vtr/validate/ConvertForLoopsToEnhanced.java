package jp.mzw.vtr.validate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
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

public class ConvertForLoopsToEnhanced extends EclipseCleanUpValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(ConvertForLoopsToEnhanced.class);

	public ConvertForLoopsToEnhanced(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(TestCase tc, CompilationUnit cu) throws CoreException {
		List<ASTNode> ret = new ArrayList<>();
		if (cu == null) {
			return ret;
		}
		ICleanUpFix fix = ConvertLoopFix.createCleanUp(cu, true, true, true);
		if (fix == null) {
			return ret;
		}
		for (CompilationUnitRewriteOperation operation : ((ConvertLoopFix) fix).getOperations()) {
			ConvertLoopOperation change = (ConvertLoopOperation) operation;
			ForStatement node = change.getForStatement();
			int start = cu.getLineNumber(node.getStartPosition());
			int end = cu.getLineNumber(node.getStartPosition() + node.getLength());
			if (tc.getStartLineNumber() <= start && end <= tc.getEndLineNumber()) {
				ret.add(node);
			}
		}
		return ret;
	}

	public static class ForStatementFinder extends GenericVisitor {

		private List<ForStatement> nodes;

		public ForStatementFinder() {
			super();
			nodes = new ArrayList<>();
		}

		@Override
		public boolean visit(ForStatement node) {
			nodes.add(node);
			return super.visit(node);
		}

		public List<ForStatement> getNodes() {
			return nodes;
		}
	}

	@Override
	protected String getModified(String origin, TestCase tc) throws IOException, CoreException, MalformedTreeException, BadLocationException {
		CompilationUnit cu = getCompilationUnit(tc.getTestFile());

		ForStatementFinder finder = new ForStatementFinder();
		cu.accept(finder);

		ASTRewrite rewrite = ASTRewrite.create(cu.getAST());
		for (ForStatement node : finder.getNodes()) {
			ConvertLoopFix fix = ConvertLoopFix.createConvertForLoopToEnhancedFix(cu, node);
			if (fix != null) {
				for (CompilationUnitRewriteOperation operation : fix.getOperations()) {
					if (operation instanceof ConvertForLoopOperation) {
						EnhancedForStatement statement = ((ConvertForLoopOperation) operation).convert(origin, cu);
						if (statement != null) {
							rewrite.replace(node, statement, null);
						}
					} else {
						LOGGER.error("Unknown type operation: {}", operation.getClass());
					}
				}
			}
		}

		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

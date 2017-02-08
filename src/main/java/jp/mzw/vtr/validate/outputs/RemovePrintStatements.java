package jp.mzw.vtr.validate.outputs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

public class RemovePrintStatements extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(RemovePrintStatements.class);

	public RemovePrintStatements(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(Commit commit, TestCase testcase, Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		testcase.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				String objectName = node.getExpression().resolveTypeBinding().getQualifiedName();
				String methodName = node.resolveMethodBinding().getName();
				if ("java.io.PrintStream".equals(objectName) && methodName.startsWith("print")) {
					ret.add(node);
				}
				return super.visit(node);
			}
		});
		return ret;
	}

	@Override
	protected String getModified(String origin, Commit commit, TestCase testcase, Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = testcase.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode detect : detect(commit, testcase, results)) {
			MethodInvocation node = (MethodInvocation) detect;
			rewrite.remove(node.getParent(), null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}

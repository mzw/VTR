package jp.mzw.vtr.validate.coding_style;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
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

public class AccessFilesProperly extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(AccessFilesProperly.class);

	public AccessFilesProperly(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(Commit commit, TestCase testcase, Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		testcase.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(ClassInstanceCreation node) {
				String name = node.resolveTypeBinding().getQualifiedName();
				if ("java.io.File".equals(name) && node.arguments().size() == 1) {
					if (node.arguments().get(0) instanceof InfixExpression) {
						InfixExpression infix = (InfixExpression) node.arguments().get(0);
						if ("+".equals(infix.getOperator().toString())) {
							Expression right = infix.getRightOperand();
							String type = right.resolveTypeBinding().getQualifiedName();
							if ("java.lang.String".equals(type)) {
								ret.add(node);
							}
 						}
					}
				}
				return super.visit(node);
			}
		});
		return ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getModified(String origin, Commit commit, TestCase testcase, Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = testcase.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode detect : detect(commit, testcase, results)) {
			ClassInstanceCreation node = (ClassInstanceCreation) detect;
			InfixExpression infix = (InfixExpression) node.arguments().get(0);
			ClassInstanceCreation copy = (ClassInstanceCreation) ASTNode.copySubtree(ast, node);
			copy.arguments().clear();
			copy.arguments().add(ASTNode.copySubtree(ast, infix.getLeftOperand()));
			copy.arguments().add(ASTNode.copySubtree(ast, infix.getRightOperand()));
			rewrite.replace(node, copy, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}

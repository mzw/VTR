package jp.mzw.vtr.validate.junit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;
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
import jp.mzw.vtr.validate.ValidatorUtils;

public class SwapActualExpectedValues extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(SwapActualExpectedValues.class);

	public SwapActualExpectedValues(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(Commit commit, TestCase testcase, Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		testcase.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				String name = node.getName().toString();
				boolean is = false;
				for (String junit : ValidatorUtils.JUNIT_ASSERT_METHODS) {
					if (junit.equals(name)) {
						is = true;
						break;
					}
				}
				if (is) {
					boolean target = false;
					ASTNode expect = null;
					ASTNode actual = null;
					if (node.arguments().size() == 2) {
						target = true;
						expect = (ASTNode) node.arguments().get(0);
						actual = (ASTNode) node.arguments().get(1);
					} else if (node.arguments().size() == 3) {
						target = true;
						expect = (ASTNode) node.arguments().get(1);
						actual = (ASTNode) node.arguments().get(2);
					}
					if (target && !isExpect(expect) && isExpect(actual)) {
						ret.add(node);
					}

				}
				return super.visit(node);
			}
		});
		return ret;
	}

	/**
	 * TODO Array
	 * 
	 * @param node
	 * @return
	 */
	public static boolean isExpect(ASTNode node) {
		if (node instanceof BooleanLiteral) {
			return true;
		} else if (node instanceof CharacterLiteral) {
			return true;
		} else if (node instanceof NullLiteral) {
			return true;
		} else if (node instanceof NumberLiteral) {
			return true;
		} else if (node instanceof StringLiteral) {
			return true;
		} else if (node instanceof TypeLiteral) {
			return true;
		}
		return false;
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
			MethodInvocation node = (MethodInvocation) detect;
			MethodInvocation copy = (MethodInvocation) ASTNode.copySubtree(ast, node);
			copy.arguments().clear();
			if (node.arguments().size() == 2) {
				copy.arguments().add(ASTNode.copySubtree(ast, (ASTNode) node.arguments().get(1)));
				copy.arguments().add(ASTNode.copySubtree(ast, (ASTNode) node.arguments().get(0)));
			} else if (node.arguments().size() == 3) {
				copy.arguments().add(ASTNode.copySubtree(ast, (ASTNode) node.arguments().get(0)));
				copy.arguments().add(ASTNode.copySubtree(ast, (ASTNode) node.arguments().get(2)));
				copy.arguments().add(ASTNode.copySubtree(ast, (ASTNode) node.arguments().get(1)));
			}
			rewrite.replace(node, copy, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}

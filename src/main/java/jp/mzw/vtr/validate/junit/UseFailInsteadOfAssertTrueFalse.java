package jp.mzw.vtr.validate.junit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BooleanLiteral;
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

public class UseFailInsteadOfAssertTrueFalse extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(UseFailInsteadOfAssertTrueFalse.class);
	
	public UseFailInsteadOfAssertTrueFalse(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				if ("assertTrue".equals(node.getName().toString())) {
					ASTNode target = null;
					if (node.arguments().size() == 1) {
						target = (ASTNode) node.arguments().get(0);
					} else if (node.arguments().size() == 2) {
						target = (ASTNode) node.arguments().get(1);
					}
					if (target instanceof BooleanLiteral) {
						BooleanLiteral expect = (BooleanLiteral) target;
						if (expect.booleanValue() == false) {
							ret.add(node);
						}
					}
				}
				if ("assertFalse".equals(node.getName().toString())) {
					ASTNode target = null;
					if (node.arguments().size() == 1) {
						target = (ASTNode) node.arguments().get(0);
					} else if (node.arguments().size() == 2) {
						target = (ASTNode) node.arguments().get(1);
					}
					if (target instanceof BooleanLiteral) {
						BooleanLiteral expect = (BooleanLiteral) target;
						if (expect.booleanValue() == true) {
							ret.add(node);
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
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> detects = detect(commit, tc, results);
		if (detects.isEmpty()) {
			return origin;
		}
		AST ast = detects.get(0).getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		for (ASTNode detect : detects) {
			MethodInvocation node = (MethodInvocation) detect;
			MethodInvocation replacement = ast.newMethodInvocation();
			replacement.setName(ast.newSimpleName("fail"));
			if (node.arguments().size() == 2) {
				ASTNode arg = ASTNode.copySubtree(ast, (ASTNode) node.arguments().get(0));
				replacement.arguments().add(arg);
			}
			rewrite.replace(node, replacement, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}

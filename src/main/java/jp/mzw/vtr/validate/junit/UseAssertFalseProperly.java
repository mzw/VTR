package jp.mzw.vtr.validate.junit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseAssertFalseProperly extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(UseAssertFalseProperly.class);

	public UseAssertFalseProperly(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> ret = new ArrayList<>();
		final List<MethodInvocation> targets = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				targets.add(node);
				return super.visit(node);
			}
		});
		for (MethodInvocation target : targets) {
			if ("assertEquals".equals(target.getName().toString())) {
				Object arg = null;
				if (2 == target.arguments().size()) {
					arg = target.arguments().get(0);
				} else if (3 == target.arguments().size()) {
					arg = target.arguments().get(1);
				}
				if (arg != null) {
					ASTNode node = (ASTNode) arg;
					if ((node.getNodeType() == ASTNode.BOOLEAN_LITERAL && ((BooleanLiteral) node).booleanValue() == false)) {
						ret.add(target);
					}
				}
			}
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode detect : detect(commit, tc, results)) {
			MethodInvocation target = (MethodInvocation) detect;
			MethodInvocation replace = ast.newMethodInvocation();
			replace.setName(ast.newSimpleName(target.getName().toString().replace("assertEquals", "assertFalse")));
			if (target.arguments().size() == 2) {
				replace.arguments().add(ASTNode.copySubtree(ast, (ASTNode) target.arguments().get(1)));
			} else if (target.arguments().size() == 3) {
				replace.arguments().add(ASTNode.copySubtree(ast, (ASTNode) target.arguments().get(0)));
				replace.arguments().add(ASTNode.copySubtree(ast, (ASTNode) target.arguments().get(2)));
			}
			rewrite.replace(target, replace, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

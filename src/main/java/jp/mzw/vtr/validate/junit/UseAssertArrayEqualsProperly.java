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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseAssertArrayEqualsProperly extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(UseAssertArrayEqualsProperly.class);

	public UseAssertArrayEqualsProperly(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				if (!"assertTrue".equals(node.getName().toString())) {
					return super.visit(node);
				}
				for (Object object : node.arguments()) {
					if (object instanceof MethodInvocation) {
						MethodInvocation method = (MethodInvocation) object;
						if (!"equals".equals(method.getName().toString())) {
							continue;
						}
						Expression expression = method.getExpression();
						if (expression == null) {
							continue;
						}
						ITypeBinding binding = expression.resolveTypeBinding();
						if (binding == null) {
							continue;
						}
						String qualifiedName = expression.resolveTypeBinding().getQualifiedName();
						if ("java.util.Arrays".equals(qualifiedName)) {
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
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode node : detect(commit, tc, results)) {
			MethodInvocation target = (MethodInvocation) node;
			MethodInvocation equals = null;
			if (target.arguments().size() == 1) {
				equals = (MethodInvocation) target.arguments().get(0);
			} else if (target.arguments().size() == 2) {
				equals = (MethodInvocation) target.arguments().get(1);
			} else {
				System.out.println("Unexpected # of arguments at " + target);
				return origin;
			}
			MethodInvocation replace = (MethodInvocation) ASTNode.copySubtree(ast, target);
			replace.setName(ast.newSimpleName("assertArrayEquals"));
			replace.arguments().clear();
			if (target.arguments().size() == 2) {
				replace.arguments().add(ASTNode.copySubtree(ast, (ASTNode) target.arguments().get(0)));
			}
			replace.arguments().add(ASTNode.copySubtree(ast, (ASTNode) equals.arguments().get(0)));
			replace.arguments().add(ASTNode.copySubtree(ast, (ASTNode) equals.arguments().get(1)));
			rewrite.replace(target, replace, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

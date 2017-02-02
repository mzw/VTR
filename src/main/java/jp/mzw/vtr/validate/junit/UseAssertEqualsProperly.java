package jp.mzw.vtr.validate.junit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseAssertEqualsProperly extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(UseAssertEqualsProperly.class);

	public UseAssertEqualsProperly(Project project) {
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
			if ("assertTrue".equals(target.getName().toString())) {
				for (Object object : target.arguments()) {
					if (object instanceof MethodInvocation) {
						MethodInvocation method = (MethodInvocation) object;
						if ("equals".equals(method.getName().toString())) {
							if (method.arguments().size() != 1) {
								Expression expression = method.getExpression();
								if (expression != null) {
									String qualifiedName = expression.resolveTypeBinding().getQualifiedName();
									String packageName = MavenUtils.getPackageName(projectDir);
									if (qualifiedName.startsWith(packageName)) {
										// Specific 'equals' method invocation
									} else if ("java.util.Arrays".equals(qualifiedName)) {
										// Use assertArrayEquals properly
									} else {
										System.out.println("Unknown 'equals' method invocation: " + method);
									}
								}
								continue;
							}
							ret.add(target);
						}
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
		for (ASTNode node : detect(commit, tc, results)) {
			MethodInvocation target = (MethodInvocation) node;
			MethodInvocation equals = null;
			for (Object object : target.arguments()) {
				if (object instanceof MethodInvocation) {
					equals = (MethodInvocation) object;
					break;
				}
			}
			MethodInvocation replace = ast.newMethodInvocation();
			replace.setName(ast.newSimpleName(target.getName().toString().replace("assertTrue", "assertEquals")));
			for (Object object : target.arguments()) {
				if (object.equals(equals)) {
					replace.arguments().add(ASTNode.copySubtree(ast, equals.getExpression()));
					replace.arguments().add(ASTNode.copySubtree(ast, (ASTNode) equals.arguments().get(0)));
				} else {
					replace.arguments().add(ASTNode.copySubtree(ast, (ASTNode) object));
				}
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

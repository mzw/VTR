package jp.mzw.vtr.validate.template;

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
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessStaticMethodsAtDefinedSuperClass extends SimpleValidatorBase {
	
	protected static Logger LOGGER = LoggerFactory.getLogger(AccessStaticMethodsAtDefinedSuperClass.class);

	public AccessStaticMethodsAtDefinedSuperClass(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(Commit commit, TestCase tc, Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		final List<MethodInvocation> methods = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				methods.add(node);
				return super.visit(node);
			}
		});
		
		for (MethodInvocation method: methods) {
			if (method.getExpression() == null) continue; // ぬるぽが出るので
			if (!(method.getExpression() instanceof SimpleName)) continue;
			if (method.resolveMethodBinding() == null) continue; // ぬるぽが出るので
			if (!Modifier.isStatic(method.resolveMethodBinding().getModifiers())) continue;
			if (method.resolveMethodBinding().getDeclaringClass() == null) continue; // ぬるぽが出るので
			ITypeBinding binding = method.resolveMethodBinding().getDeclaringClass();
			if (!method.getExpression().toString().equals(binding.getName())) {
				ret.add(method);
				continue;
			}
		}
		return ret;
	}

	@Override
	protected String getModified(String origin, Commit commit, TestCase tc, Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode node: detect(commit, tc, results)) {
			MethodInvocation target = (MethodInvocation) node;
			MethodInvocation replace= (MethodInvocation) ASTNode.copySubtree(ast, target);
			ITypeBinding binding = target.resolveMethodBinding().getDeclaringClass();
			replace.setExpression(ast.newSimpleName(binding.getName()));
			rewrite.replace(target, replace, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

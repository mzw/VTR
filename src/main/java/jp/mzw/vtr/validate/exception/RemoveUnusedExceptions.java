package jp.mzw.vtr.validate.exception;

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
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveUnusedExceptions extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(RemoveUnusedExceptions.class);

	public RemoveUnusedExceptions(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(Commit commit, TestCase tc, Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		// Collect throwable exceptions
		final List<ITypeBinding> exceptions = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				IMethodBinding binding = node.resolveMethodBinding();
				if (binding == null) {
					return super.visit(node);
				}
				for (ITypeBinding exception : binding.getExceptionTypes()) {
					if (!exceptions.contains(exception)) {
						exceptions.add(exception);
					}
				}
				return super.visit(node);
			}
		});
		// Determine whether thrown exceptions are used
		for (Object object : tc.getMethodDeclaration().thrownExceptionTypes()) {
			SimpleType thrown = (SimpleType) object;
			ITypeBinding type = thrown.resolveBinding();
			boolean used = false;
			for (ITypeBinding exception : exceptions) {
				if (exception.equals(type)) {
					used = true;
					break;
				}
			}
			if (!used) {
				ret.add(thrown);
			}
		}
		return ret;
	}

	@Override
	protected String getModified(String origin, Commit commit, TestCase tc, Results results) throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> detects = detect(commit, tc, results);
		if (detects.isEmpty()) {
			return origin;
		}
		AST ast = detects.get(0).getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		for (ASTNode detect : detects) {
			rewrite.remove(detect, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

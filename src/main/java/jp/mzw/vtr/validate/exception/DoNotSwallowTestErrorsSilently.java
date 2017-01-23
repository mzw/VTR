package jp.mzw.vtr.validate.exception;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;
import jp.mzw.vtr.validate.ValidatorUtils;

public class DoNotSwallowTestErrorsSilently extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(DoNotSwallowTestErrorsSilently.class);

	public DoNotSwallowTestErrorsSilently(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(TryStatement node) {
				boolean has = false;
				for (Object object : node.catchClauses()) {
					if (object instanceof CatchClause) {
						CatchClause catchClause = (CatchClause) object;
						if (ValidatorUtils.hasAssertMethodInvocation(catchClause.getBody())) {
							has = true;
							break;
						}
					}
				}
				if (!has) {
					ret.add(node);
				}
				return super.visit(node);
			}
		});
		return ret;
	}

	@Override
	protected String getModified(String origin, TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		List<CatchClause> catches = new ArrayList<>();
		for (ASTNode detect : detect(tc)) {
			TryStatement node = (TryStatement) detect;
			for (Object object : node.catchClauses()) {
				catches.add((CatchClause) object);
			}
			ListRewrite listRewrite = rewrite.getListRewrite(node.getBody(), Block.STATEMENTS_PROPERTY);
			ASTNode previous = null;
			for (Object object : node.getBody().statements()) {
				ASTNode statement = (ASTNode) object;
				ASTNode copy = ASTNode.copySubtree(ast, statement);
				if (previous == null) {
					rewrite.replace(node, copy, null);
				} else {
					listRewrite.insertAfter(copy, previous, null);
				}
				previous = copy;
			}
		}
		// Add exception throws if necessary
		List<ITypeBinding> adds = new ArrayList<>();
		for (CatchClause cc : catches) {
			ITypeBinding removed = cc.getException().getType().resolveBinding();
			boolean throwed = false;
			for (Object object : tc.getMethodDeclaration().thrownExceptionTypes()) {
				SimpleType thrown = (SimpleType) object;
				ITypeBinding type = thrown.resolveBinding();
				if (removed.equals(type)) {
					throwed = true;
					break;
				}
			}
			if (!throwed) {
				adds.add(removed);
			}
		}
		ListRewrite listRewrite = rewrite.getListRewrite(tc.getMethodDeclaration(), MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY);
		for (ITypeBinding add : adds) {
			SimpleType type = ast.newSimpleType(ast.newSimpleName(add.getName()));
			listRewrite.insertLast(type, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

	protected List<ITypeBinding> getExceptionTypesToBeAdded(TestCase tc) {
		final List<ITypeBinding> ret = new ArrayList<>();
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
		// Detect exception types that this method does not throw
		for (ITypeBinding exception : exceptions) {
			boolean throwed = false;
			for (Object object : tc.getMethodDeclaration().thrownExceptionTypes()) {
				SimpleType thrown = (SimpleType) object;
				ITypeBinding type = thrown.resolveBinding();
				if (exception.equals(type)) {
					throwed = true;
					break;
				}
			}
			if (!throwed) {
				ret.add(exception);
			}
		}
		return ret;
	}

}

package jp.mzw.vtr.validate.exception_handling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Comment;
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
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;
import jp.mzw.vtr.validate.ValidatorBase;
import jp.mzw.vtr.validate.ValidatorUtils;

public class DoNotSwallowTestErrorsSilently extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(DoNotSwallowTestErrorsSilently.class);

	public DoNotSwallowTestErrorsSilently(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		if (Version.parse("4").compareTo(ValidatorBase.getJunitVersion(projectDir)) < 0) {
			return ret;
		}
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(TryStatement node) {
				List<?> catchClauses = node.catchClauses();
				if (catchClauses.isEmpty()) {
					return super.visit(node);
				}
				boolean has = false;
				for (Object object : catchClauses) {
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
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		List<CatchClause> catches = new ArrayList<>();
		for (ASTNode detect : detect(commit, tc, results)) {
			TryStatement node = (TryStatement) detect;
			// skip if try-with-resource
			if (!node.resources().isEmpty()) {
				return origin;
			}
			if (node.getFinally() != null && ValidatorUtils.hasAssertMethodInvocation(node.getFinally())) {
				// skip if finally body has an assert method(including fail).
				return origin;
			}
			// skip if no catch clauses
			if (node.catchClauses() == null) {
				return origin;
			}
			for (CatchClause cc : (List<CatchClause>) node.catchClauses()) {
				for (Comment comment : tc.getComments()) {
					// no statements in this catch clauses
					if (!(cc.getBody().statements() != null ||
							// there are some statements in this catch clause, but all of them are print statement
							(!cc.getBody().statements().isEmpty() && ValidatorUtils.onlyPrintMethodInvocation(cc)) ||
							// catch clauses have exception expecting comment.
							(ValidatorUtils.thisNodeHasThisComments(cc, comment) && todoComment(ValidatorUtils.comment(comment, origin))))) {
						return origin;
					}
				}
			}
			for (Object object : node.catchClauses()) {
				catches.add((CatchClause) object);
			}
			ListRewrite listRewrite = rewrite.getListRewrite(getNearestParentBlock(node), Block.STATEMENTS_PROPERTY);
			for (Object object : node.getBody().statements()) {
				ASTNode statement = (ASTNode) object;
				ASTNode copy = ASTNode.copySubtree(ast, statement);
				listRewrite.insertBefore(copy, node, null);
			}
			rewrite.remove(node, null);
		}
		// Add exception throws if necessary
		List<ITypeBinding> adds = new ArrayList<>();
		for (CatchClause cc : catches) {
			ITypeBinding removed = cc.getException().getType().resolveBinding();
			boolean throwed = false;
			boolean runtime = ValidatorUtils.isRuntimeException(removed);
			for (Object object : tc.getMethodDeclaration().thrownExceptionTypes()) {
				SimpleType thrown = (SimpleType) object;
				ITypeBinding type = thrown.resolveBinding();
				if (removed.equals(type)) {
					throwed = true;
					break;
				}
			}
			if (!(runtime || throwed)) {
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

	public static Block getNearestParentBlock(ASTNode node) {
		if (node == null) {
			return null;
		}
		if (node instanceof Block) {
			return (Block) node;
		}
		ASTNode parent = node.getParent();
		while (parent != null) {
			if (parent instanceof Block) {
				return (Block) parent;
			}
			parent = parent.getParent();
		}
		return null;
	}
	public boolean todoComment(String comment) {
		String content = comment.toLowerCase();
		return content.contains("todo") || comment.contains("fix");
	}

}

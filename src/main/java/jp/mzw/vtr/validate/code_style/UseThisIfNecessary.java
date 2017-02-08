package jp.mzw.vtr.validate.code_style;

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
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.Bindings;
import org.eclipse.jdt.internal.corext.dom.GenericVisitor;
import org.eclipse.jdt.internal.corext.dom.ScopeAnalyzer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseThisIfNecessary extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(UseThisIfNecessary.class);

	public UseThisIfNecessary(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		final CompilationUnit cu = tc.getCompilationUnit();
		ThisQualifierVisitor visitor = new ThisQualifierVisitor(cu);
		cu.accept(visitor);
		for (FieldAccess field : visitor.getReplacableFields()) {
			int start = cu.getLineNumber(field.getStartPosition());
			int end = cu.getLineNumber(field.getStartPosition() + field.getLength());
			if (tc.getStartLineNumber() <= start && end <= tc.getEndLineNumber()) {
				ret.add(field);
			}
		}
		for (Expression expression : visitor.getRemovableExpressions()) {
			int start = cu.getLineNumber(expression.getStartPosition());
			int end = cu.getLineNumber(expression.getStartPosition() + expression.getLength());
			if (tc.getStartLineNumber() <= start && end <= tc.getEndLineNumber()) {
				ret.add(expression);
			}
		}
		return ret;
	}

	public static class ThisQualifierVisitor extends GenericVisitor {

		private CompilationUnit cu;
		private List<FieldAccess> fields;
		private List<Expression> expressions;

		public ThisQualifierVisitor(CompilationUnit cu) {
			this.cu = cu;
			this.fields = new ArrayList<>();
			this.expressions = new ArrayList<>();
		}

		public List<FieldAccess> getReplacableFields() {
			return fields;
		}

		public List<Expression> getRemovableExpressions() {
			return expressions;
		}

		@Override
		public boolean visit(final FieldAccess node) {
			Expression expression = node.getExpression();
			if (!(expression instanceof ThisExpression))
				return true;

			final SimpleName name = node.getName();
			if (hasConflict(expression.getStartPosition(), name, ScopeAnalyzer.VARIABLES | ScopeAnalyzer.CHECK_VISIBILITY))
				return true;

			Name qualifier = ((ThisExpression) expression).getQualifier();
			if (qualifier != null) {
				ITypeBinding outerClass = (ITypeBinding) qualifier.resolveBinding();
				if (outerClass == null)
					return true;

				IVariableBinding nameBinding = (IVariableBinding) name.resolveBinding();
				if (nameBinding == null)
					return true;

				ITypeBinding variablesDeclaringClass = nameBinding.getDeclaringClass();
				if (outerClass != variablesDeclaringClass)
					// be conservative: We have a reference to a field of an
					// outer type, and this type inherited
					// the field. It's possible that the inner type inherits the
					// same field. We must not remove
					// the qualifier in this case.
					return true;

				ITypeBinding enclosingTypeBinding = Bindings.getBindingOfParentType(node);
				if (enclosingTypeBinding == null || Bindings.isSuperType(variablesDeclaringClass, enclosingTypeBinding))
					// We have a reference to a field of an outer type, and this
					// type inherited
					// the field. The inner type inherits the same field. We
					// must not remove
					// the qualifier in this case.
					return true;
			}

			fields.add(node);
			return super.visit(node);
		}

		@Override
		public boolean visit(final MethodInvocation node) {
			Expression expression = node.getExpression();
			if (!(expression instanceof ThisExpression))
				return true;

			final SimpleName name = node.getName();
			if (name.resolveBinding() == null)
				return true;

			if (hasConflict(expression.getStartPosition(), name, ScopeAnalyzer.METHODS | ScopeAnalyzer.CHECK_VISIBILITY))
				return true;

			Name qualifier = ((ThisExpression) expression).getQualifier();
			if (qualifier != null) {
				ITypeBinding declaringClass = ((IMethodBinding) name.resolveBinding()).getDeclaringClass();
				if (declaringClass == null)
					return true;

				ITypeBinding caller = getDeclaringType(node);
				if (caller == null)
					return true;

				ITypeBinding callee = (ITypeBinding) qualifier.resolveBinding();
				if (callee == null)
					return true;

				if (callee.isAssignmentCompatible(declaringClass) && caller.isAssignmentCompatible(declaringClass))
					return true;
			}

			expressions.add(node.getExpression());
			return super.visit(node);
		}

		private ITypeBinding getDeclaringType(MethodInvocation node) {
			ASTNode p = node;
			while (p != null) {
				p = p.getParent();
				if (p instanceof AbstractTypeDeclaration) {
					return ((AbstractTypeDeclaration) p).resolveBinding();
				}
			}
			return null;
		}

		private boolean hasConflict(int startPosition, SimpleName name, int flag) {
			ScopeAnalyzer analyzer = new ScopeAnalyzer(cu);
			IBinding[] declarationsInScope = analyzer.getDeclarationsInScope(startPosition, flag);
			for (int i = 0; i < declarationsInScope.length; i++) {
				IBinding decl = declarationsInScope[i];
				if (decl.getName().equals(name.getIdentifier()) && name.resolveBinding() != decl)
					return true;
			}
			return false;
		}

	}

	@Override
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		CompilationUnit cu = tc.getCompilationUnit();
		ThisQualifierVisitor visitor = new ThisQualifierVisitor(cu);
		cu.accept(visitor);
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		for (FieldAccess field : visitor.getReplacableFields()) {
			rewrite.replace(field, rewrite.createCopyTarget(field.getName()), null);
		}
		for (Expression expression : visitor.getRemovableExpressions()) {
			rewrite.remove(expression, null);
		}
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}

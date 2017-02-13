package jp.mzw.vtr.validate.exception_handling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;
import jp.mzw.vtr.validate.ValidatorBase;
import jp.mzw.vtr.validate.ValidatorUtils;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
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

public class HandleExpectedExecptionsProperly extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(HandleExpectedExecptionsProperly.class);

	public HandleExpectedExecptionsProperly(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		if (ValidatorBase.getJunitVersion(projectDir) < 4.0) {
			return ret;
		}
		final CompilationUnit cu = tc.getCompilationUnit();
		tc.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(TryStatement node) {
				// not target if body does not have any JUnit assertions
				if (!ValidatorUtils.hasAssertMethodInvocation(node.getBody())) {
					return super.visit(node);
				}
				// get catch clauses containing 'expect' comments
				List<CatchClause> expects = getExpectedExceptions(node, cu);
				if (expects.size() == node.catchClauses().size()) {
					ret.add(node);
				}
				return super.visit(node);
			}
		});
		return ret;
	}

	private List<CatchClause> getExpectedExceptions(TryStatement node, CompilationUnit cu) {
		final List<CatchClause> expects = new ArrayList<>();
		for (Object object : node.catchClauses()) {
			final CatchClause cc = (CatchClause) object;
			if (!ValidatorUtils.hasAssertMethodInvocation(cc.getBody())) {
				expects.add(cc);
			}
		}
		return expects;
	}

	private Annotation createTestAnnotation(Annotation annot, List<ITypeBinding> expects, ASTRewrite rewrite) {
		StringBuilder classes = new StringBuilder();
		String delim = "";
		for (ITypeBinding type : expects) {
			classes.append(delim).append(type.getName()).append(".class");
			delim = ", ";
		}
		String code = annot.toString();
		if (annot.getProperty("expected") == null) {
			code += "(expected = " + classes.toString() + ")";
		} else {
			code.replace("expected = ", "expected = " + classes.toString());
		}
		return (Annotation) rewrite.createStringPlaceholder(code, ASTNode.MARKER_ANNOTATION);
	}

	private Block getParentBlock(TryStatement node) {
		ASTNode parent = node.getParent();
		while (parent != null) {
			if (parent instanceof Block) {
				return (Block) parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	@Override
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		final List<ASTNode> detects = detect(commit, tc, results);
		if (detects.isEmpty()) {
			return origin;
		}
		final CompilationUnit cu = tc.getCompilationUnit();
		final AST ast = cu.getAST();
		final ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		final MethodDeclaration method = tc.getMethodDeclaration();
		final List<ITypeBinding> atTestAnnot = new ArrayList<>();
		final List<ITypeBinding> atThrownProp = new ArrayList<>();
		for (ASTNode detect : detects) {
			TryStatement node = (TryStatement) detect;
			List<CatchClause> expects = getExpectedExceptions(node, cu);
			for (CatchClause cc : expects) {
				ITypeBinding eType = cc.getException().getType().resolveBinding();
				if (!atTestAnnot.contains(eType)) {
					atTestAnnot.add(eType);
				}
				for (Object object : method.thrownExceptionTypes()) {
					SimpleType thrown = (SimpleType) object;
					ITypeBinding tType = thrown.resolveBinding();
					if (!eType.equals(tType)) {
						if (!atThrownProp.contains(eType)) {
							atThrownProp.add(eType);
						}
					}
				}
			}
			// copy try-body statement
			Block parent = getParentBlock(node);
			ListRewrite lr = rewrite.getListRewrite(parent, Block.STATEMENTS_PROPERTY);
			ASTNode previous = null;
			for (Object object : node.getBody().statements()) {
				ASTNode statement = (ASTNode) object;
				ASTNode copy = ASTNode.copySubtree(ast, statement);
				if (previous == null) {
					rewrite.replace(node, copy, null);
				} else {
					lr.insertAfter(copy, previous, null);
				}
				previous = copy;
			}
		}
		// modify Test annotation
		Annotation annot = ValidatorUtils.getTestAnnotation(tc);
		Annotation newAnnot = ast.newMarkerAnnotation();
		newAnnot.setTypeName(ast.newName("Test"));
		Annotation newAnnotWithExpectedExceptions = createTestAnnotation(newAnnot, atTestAnnot, rewrite);
		if (annot == null) {
			ListRewrite lr = rewrite.getListRewrite(method, MethodDeclaration.MODIFIERS2_PROPERTY);
			lr.insertFirst(newAnnotWithExpectedExceptions, null);
		} else {
			rewrite.replace(annot, newAnnotWithExpectedExceptions, null);
		}
		// modify throw clauses
		ListRewrite list = rewrite.getListRewrite(method, MethodDeclaration.THROWN_EXCEPTION_TYPES_PROPERTY);
		for (ITypeBinding type : atThrownProp) {
			list.insertLast(ast.newSimpleType(ast.newSimpleName(type.getName())), null);
		}
		// modify
		final Document document = new Document(origin);
		final TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}

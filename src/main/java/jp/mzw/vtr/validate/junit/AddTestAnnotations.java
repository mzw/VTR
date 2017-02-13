package jp.mzw.vtr.validate.junit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
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

public class AddTestAnnotations extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(AddTestAnnotations.class);

	public AddTestAnnotations(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		if (Version.parse("4").compareTo(ValidatorBase.getJunitVersion(projectDir)) < 0) {
			return ret;
		}
		if (!ValidatorUtils.hasTestAnnotation(tc)) {
			ret.add(tc.getMethodDeclaration());
		}
		return ret;
	}

	@Override
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		final CompilationUnit cu = tc.getCompilationUnit();
		final AST ast = cu.getAST();
		final ASTRewrite rewrite = ASTRewrite.create(ast);
		// add Test annotation
		{
			MethodDeclaration method = tc.getMethodDeclaration();
			ListRewrite lr = rewrite.getListRewrite(method, method.getModifiersProperty());
			lr.insertFirst(rewrite.createStringPlaceholder("@Test", ASTNode.ANNOTATION_TYPE_DECLARATION), null);
			lr.insertFirst(rewrite.createStringPlaceholder("@Test", ASTNode.ANNOTATION_TYPE_DECLARATION), null);
		}
		// import Test if nothing
		{
			boolean imported = false;
			List<ImportDeclaration> removes = new ArrayList<>();
			for (Object object : cu.imports()) {
				ImportDeclaration current = (ImportDeclaration) object;
				if (current.getName().toString().startsWith("junit.framework")) {
					removes.add(current);
				} else if (current.getName().toString().equals("import org.junit.Test")) {
					imported = true;
				} else if (current.getName().toString().equals("import org.junit.*")) {
					imported = true;
				}
			}
			if (!imported) {
				ListRewrite lr = rewrite.getListRewrite(tc.getCompilationUnit(), CompilationUnit.IMPORTS_PROPERTY);
				lr.insertLast(rewrite.createStringPlaceholder("import org.junit.Test;", ASTNode.IMPORT_DECLARATION), null);
			}
			ASTRewrite rewriter = ASTRewrite.create(tc.getCompilationUnit().getAST());
			for (ImportDeclaration remove : removes) {
				rewriter.remove(remove, null);
			}
		}
		// remove types
		cu.accept(new ASTVisitor() {
			@Override
			public boolean visit(TypeDeclaration node) {
				Type type = node.getSuperclassType();
				if (type != null) {
					if ("TestCase".equals(type.toString())) {
						rewrite.remove(node, null);
					}
				}
				return super.visit(node);
			}
		});
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

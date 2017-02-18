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
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
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
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
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
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		final CompilationUnit cu = tc.getCompilationUnit();
		final AST ast = cu.getAST();
		final ASTRewrite rewrite = ASTRewrite.create(ast);
		// add Test annotation
		{
			MethodDeclaration method = tc.getMethodDeclaration();
			ListRewrite modifiers = rewrite.getListRewrite(method, method.getModifiersProperty());
			modifiers.insertFirst(rewrite.createStringPlaceholder("@Test", ASTNode.ANNOTATION_TYPE_DECLARATION), null);
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
				ListRewrite imports = rewrite.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY);
				imports.insertLast(rewrite.createStringPlaceholder("import org.junit.Test;", ASTNode.IMPORT_DECLARATION), null);
			}
			for (ImportDeclaration remove : removes) {
				rewrite.remove(remove, null);
			}
		}
		// remove types
		{
			final List<MethodDeclaration> setups = new ArrayList<>();
			final List<MethodDeclaration> teardowns = new ArrayList<>();
			cu.accept(new ASTVisitor() {
				@Override
				public boolean visit(TypeDeclaration node) {
					Type type = node.getSuperclassType();
					if (type != null) {
						if ("TestCase".equals(type.toString())) {
							rewrite.remove(type, null);
						}
					}
					return super.visit(node);
				}

				@Override
				public boolean visit(SuperMethodInvocation node) {
					String name = node.getName().toString();
					MethodDeclaration method = getParentMethodDeclaration(node);
					if (method == null) {
						return super.visit(node);
					}
					if ("setUp".equals(name)) {
						if (!setups.contains(method)) {
							setups.add(method);
						}
						rewrite.remove(node.getParent(), null);
					} else if ("tearDown".equals(name)) {
						if (!teardowns.contains(method)) {
							teardowns.add(method);
						}
						rewrite.remove(node.getParent(), null);
					}
					return super.visit(node);
				}
			});
			if (!setups.isEmpty()) {
				ListRewrite imports = rewrite.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY);
				imports.insertLast(rewrite.createStringPlaceholder("import org.junit.Before;", ASTNode.IMPORT_DECLARATION), null);
				for (MethodDeclaration method : setups) {
					ListRewrite modifiers = rewrite.getListRewrite(method, method.getModifiersProperty());
					modifiers.insertFirst(rewrite.createStringPlaceholder("@Before", ASTNode.ANNOTATION_TYPE_DECLARATION), null);
				}
			}
			if (!teardowns.isEmpty()) {
				ListRewrite imports = rewrite.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY);
				imports.insertLast(rewrite.createStringPlaceholder("import org.junit.After;", ASTNode.IMPORT_DECLARATION), null);
				for (MethodDeclaration method : teardowns) {
					ListRewrite modifiers = rewrite.getListRewrite(method, method.getModifiersProperty());
					modifiers.insertFirst(rewrite.createStringPlaceholder("@After", ASTNode.ANNOTATION_TYPE_DECLARATION), null);
				}
			}
		}

		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

	public static MethodDeclaration getParentMethodDeclaration(ASTNode node) {
		if (node == null) {
			return null;
		}
		if (node instanceof MethodDeclaration) {
			return (MethodDeclaration) node;
		}
		ASTNode parent = node.getParent();
		while (parent != null) {
			if (parent instanceof MethodDeclaration) {
				return (MethodDeclaration) parent;
			}
			parent = parent.getParent();
		}
		return null;
	}
}

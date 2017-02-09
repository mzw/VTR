package jp.mzw.vtr.validate.suppress_warnings.add_override_annotation;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by TK on 2017/02/02.
 */
abstract public class AddOverrideAnnotationBase extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(AddOverrideAnnotationBase.class);

	public AddOverrideAnnotationBase(Project project) {
		super(project);
	}

	@Override
	public String getModified(String origin, final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode node : detect(commit, tc, results)) {
			MethodDeclaration target = (MethodDeclaration) node;
			MethodDeclaration replace = addOverrideAnnotation(ast, target);
			rewrite.replace(target, replace, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

	public static boolean overrideMethod(MethodDeclaration method) {
		IMethodBinding binding = method.resolveBinding();
		ITypeBinding typeBinding = binding.getDeclaringClass();
		while (typeBinding != null) {
			for (IMethodBinding methodBinding : typeBinding.getDeclaredMethods()) {
				if (binding.overrides(methodBinding)) {
					return true;
				}
			}
			typeBinding = typeBinding.getSuperclass();
		}
		return false;
	}

	public static boolean hasOverrideAnnotation(MethodDeclaration method) {
		for (Object modifier : method.modifiers()) {
			if (!(modifier instanceof MarkerAnnotation)) {
				continue;
			}
			MarkerAnnotation marker = (MarkerAnnotation) modifier;
			if ("Override".equals(marker.getTypeName().toString())) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static MethodDeclaration addOverrideAnnotation(AST ast, MethodDeclaration target) {
		MethodDeclaration replace = (MethodDeclaration) ASTNode.copySubtree(ast, target);
		// create new annotation
		MarkerAnnotation annotation = ast.newMarkerAnnotation();
		annotation.setTypeName(ast.newName("Override"));
		// insert annotation
		replace.modifiers().add(0, annotation);
		return replace;
	}
}

package jp.mzw.vtr.validate;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

public class AddTestAnnotations extends ValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(AddTestAnnotations.class);

	public AddTestAnnotations(Project project) {
		super(project);
	}

	@Override
	public void onCheckout(Commit commit) {
		try {
			boolean isJunit4 = ValidatorUtils.isJunit4(new File(this.projectDir, "pom.xml"));
			if (isJunit4) {
				for (TestSuite ts : MavenUtils.getTestSuites(this.projectDir)) {
					for (TestCase tc : ts.getTestCases()) {
						if (this.dupulicates.contains(tc.getFullName())) {
							continue;
						}
						boolean hasTestAnnotation = ValidatorUtils.hasTestAnnotation(tc);
						if (!hasTestAnnotation) {
							this.dupulicates.add(tc.getFullName());
							MethodDeclaration method = tc.getMethodDeclaration();
							ValidationResult vr = new ValidationResult(this.projectId, commit, tc, tc.getStartLineNumber(method), tc.getEndLineNumber(method),
									this);
							this.validationResultList.add(vr);
						}
					}
				}
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to checkout: {}", commit.getId());
		}
	}

	@Override
	public void generate(ValidationResult result) {
		try {
			TestCase tc = getTestCase(result);
			String origin = FileUtils.readFileToString(tc.getTestFile());
			String hasTestAnnot = insertTestAnnotation(origin, tc);
			String hasJunitImports = insertJuitImports(hasTestAnnot, tc);
			List<String> patch = genPatch(origin, hasJunitImports, tc.getTestFile(), tc.getTestFile());
			output(result, tc, patch);
		} catch (IOException | ParseException | GitAPIException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}

	/**
	 * Insert test annotation
	 * 
	 * @param origin
	 * @param tc
	 * @return
	 * @throws MalformedTreeException
	 * @throws BadLocationException
	 */
	private String insertTestAnnotation(String origin, TestCase tc) throws MalformedTreeException, BadLocationException {
		ASTRewrite rewriter = ASTRewrite.create(tc.getCompilationUnit().getAST());
		MethodDeclaration method = tc.getMethodDeclaration();
		ListRewrite lr = rewriter.getListRewrite(method, method.getModifiersProperty());
		lr.insertFirst(rewriter.createStringPlaceholder("@Test", ASTNode.ANNOTATION_TYPE_DECLARATION), null);
		org.eclipse.jface.text.Document document = new org.eclipse.jface.text.Document(origin);
		TextEdit edit = rewriter.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

	/**
	 * Insert "org.junit.Test" import and remove "junit.framework" imports, if
	 * any
	 * 
	 * @param origin
	 * @param tc
	 * @return
	 * @throws MalformedTreeException
	 * @throws BadLocationException
	 */
	private String insertJuitImports(String origin, TestCase tc) throws MalformedTreeException, BadLocationException {
		List<ImportDeclaration> removeImports = new ArrayList<>();
		List<Type> removeTypes = new ArrayList<>();
		boolean isJunitTestImport = false;
		ASTNode root = tc.getMethodDeclaration().getRoot();
		for (ASTNode node : MavenUtils.getChildren(root)) {
			if (node instanceof ImportDeclaration) {
				ImportDeclaration importDec = (ImportDeclaration) node;
				if (importDec.getName().toString().startsWith("junit.framework")) {
					removeImports.add(importDec);
				} else if (importDec.getName().toString().equals("import org.junit.Test")) {
					isJunitTestImport = true;
				} else if (importDec.getName().toString().equals("import org.junit.*")) {
					isJunitTestImport = true;
				}
			} else if (node instanceof TypeDeclaration) {
				TypeDeclaration typeDec = (TypeDeclaration) node;
				Type type = typeDec.getSuperclassType();
				if (type != null) {
					if ("TestCase".equals(type.toString())) {
						removeTypes.add(type);
					}
				}
			}
		}
		ASTRewrite rewriter = ASTRewrite.create(tc.getCompilationUnit().getAST());
		for (ImportDeclaration remove : removeImports) {
			rewriter.remove(remove, null);
		}
		if (!isJunitTestImport) {
			ListRewrite lr = rewriter.getListRewrite(tc.getCompilationUnit(), CompilationUnit.IMPORTS_PROPERTY);
			lr.insertLast(rewriter.createStringPlaceholder("import org.junit.Test;", ASTNode.IMPORT_DECLARATION), null);
		}
		for (Type remove : removeTypes) {
			rewriter.remove(remove, null);
		}
		org.eclipse.jface.text.Document document = new org.eclipse.jface.text.Document(origin);
		TextEdit edit = rewriter.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

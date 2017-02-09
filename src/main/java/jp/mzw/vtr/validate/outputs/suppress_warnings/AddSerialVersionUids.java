package jp.mzw.vtr.validate.outputs.suppress_warnings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
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
import jp.mzw.vtr.validate.ValidatorUtils;

public class AddSerialVersionUids extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(AddSerialVersionUids.class);

	public AddSerialVersionUids(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(Commit commit, TestCase testcase, Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		final List<TypeDeclaration> classes = new ArrayList<>();
		testcase.getCompilationUnit().accept(new ASTVisitor() {
			@Override
			public boolean visit(TypeDeclaration node) {
				if (ValidatorUtils.isSerializable(node.resolveBinding())) {
					classes.add(node);
				}
				return super.visit(node);
			}
		});
		for (final TypeDeclaration clazz : classes) {
			boolean implemented = false;
			final List<FieldDeclaration> fields = new ArrayList<>();
			clazz.accept(new ASTVisitor() {
				@Override
				public boolean visit(FieldDeclaration node) {
					fields.add(node);
					return super.visit(node);
				}

				@Override
				public boolean visit(TypeDeclaration node) {
					if (node.equals(clazz)) {
						return true;
					}
					return false;
				}
			});
			for (FieldDeclaration field : fields) {
				for (Object object : field.fragments()) {
					if (object instanceof VariableDeclarationFragment) {
						VariableDeclarationFragment var = (VariableDeclarationFragment) object;
						if ("serialVersionUID".equals(var.getName().toString())) {
							implemented = true;
							break;
						}
					}
				}
				if (implemented) {
					break;
				}
			}
			if (!implemented) {
				ret.add(clazz);
			}
		}
		return ret;
	}

	@Override
	protected String getModified(String origin, Commit commit, TestCase testcase, Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = testcase.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode detect : detect(commit, testcase, results)) {
			TypeDeclaration node = (TypeDeclaration) detect;
			ListRewrite listRewrite = rewrite.getListRewrite(node, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
			FieldDeclaration field = (FieldDeclaration) rewrite.createStringPlaceholder("private static final long serialVersionUID = 1L;",
					ASTNode.FIELD_DECLARATION);
			listRewrite.insertFirst(field, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}

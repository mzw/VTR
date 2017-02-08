package jp.mzw.vtr.validate.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
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

public class UseDiamondOperators extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(UseDiamondOperators.class);

	public UseDiamondOperators(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(Commit commit, TestCase testcase, Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		// Java version check
		if (ValidatorBase.getJavaVersion(projectDir) < 1.7) {
			return ret;
		}
		// traverse
		testcase.getMethodDeclaration().accept(new ASTVisitor() {
			@Override
			public boolean visit(ClassInstanceCreation node) {
				Type type = node.getType();
				if (type instanceof ParameterizedType) {
					ParameterizedType paramed = (ParameterizedType) type;
					String diamond = type.toString().replace(paramed.getType().toString(), "").trim();
					if (!"<>".equals(diamond)) {
						ret.add(node);
					}
				}
				return super.visit(node);
			}
		});
		return ret;
	}

	@Override
	protected String getModified(String origin, Commit commit, TestCase testcase, Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> detects = detect(commit, testcase, results);
		if (detects.isEmpty()) {
			return origin;
		}
		CompilationUnit cu = testcase.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// rewrite
		for (ASTNode detect : detects) {
			ClassInstanceCreation node = (ClassInstanceCreation) detect;
			ParameterizedType type = (ParameterizedType) node.getType();
			String diamond = type.toString().replace(type.getType().toString(), "").trim();
			String name = type.toString().replace(diamond, "<>");
			ClassInstanceCreation copy = (ClassInstanceCreation) ASTNode.copySubtree(ast, node);
			ParameterizedType create = (ParameterizedType) rewrite.createStringPlaceholder(name,
					ASTNode.PARAMETERIZED_TYPE);
			copy.setType(create);
			rewrite.replace(node, copy, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}

package jp.mzw.vtr.validate.junit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
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
import jp.mzw.vtr.validate.ValidatorUtils;

public class ModifyAssertImports extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(ModifyAssertImports.class);

	public ModifyAssertImports(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(Commit commit, TestCase testcase, Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		// TODO Do not make sense but cannot parse to 'import static'...
		List<String> lines = FileUtils.readLines(testcase.getTestFile());
		boolean isStaticImport = false;
		for (String line : lines) {
			if (line.startsWith("import static org.junit.Assert")) {
				isStaticImport = true;
				break;
			}
		}
		if (isStaticImport) {
			ret.add(testcase.getMethodDeclaration());
		}
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
		final List<MethodInvocation> junitAssertMethods = new ArrayList<>();
		cu.accept(new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				boolean isJunitAssertMethod = false;
				for (String junit : ValidatorUtils.JUNIT_ASSERT_METHODS) {
					if (junit.equals(node.getName().toString())) {
						isJunitAssertMethod = true;
						break;
					}
				}
				if (isJunitAssertMethod) {
					junitAssertMethods.add(node);
				}
				return super.visit(node);
			}
		});
		for (MethodInvocation method : junitAssertMethods) {
			MethodInvocation create = (MethodInvocation) rewrite.createStringPlaceholder("Assert." + method.toString(), ASTNode.METHOD_INVOCATION);
			rewrite.replace(method, create, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		// specific modify
		StringBuilder ret = new StringBuilder();
		boolean add = false;
		for (String line : toList(document.get())) {
			if (line.startsWith("import static org.junit.Assert")) {
				if (!add) {
					ret.append("import org.junit.Assert;").append("\n");
					add = true;
				}
			} else {
				ret.append(line).append("\n");
			}
		}
		return ret.toString();
	}
}

package jp.mzw.vtr.validate.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.AllElementsFindVisitor;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AddSuppressWarningsAnnotation extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(AddSuppressWarningsAnnotation.class);

	public AddSuppressWarningsAnnotation(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> targets = new ArrayList<>();

		// results are null in generating patches.
		if (results == null) {
			results = Results.parse(outputDir, projectId, commit);
		}
		// get deprecated nodes' position from deprecated messages
		List<String> deprecatedMessages = deprecatedMessages(results.getCompileOutputs());
		List<Integer> deprecatedNodePositions = deprecatedNodePositions(deprecatedMessages, tc);
		// find deprecated nodes using obtained positions
		AllElementsFindVisitor visitor = new AllElementsFindVisitor();
		tc.getMethodDeclaration().accept(visitor);
		List<ASTNode> nodes = visitor.getNodes();
		List<ASTNode> deprecatedNodes = new ArrayList<>();
		for (ASTNode node: nodes) {
			if (deprecatedNodePositions.contains(node.getStartPosition())) {
				deprecatedNodes.add(node);
			}
		}
		for (ASTNode node: deprecatedNodes) {
			while (node.getParent() != null) {
				if (node instanceof MethodDeclaration) {
					targets.add(node);
					break;
				}
				node = node.getParent();
			}
		}
		return targets;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getModified(String origin, TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode node: detect(tc)) {
			MethodDeclaration target = (MethodDeclaration) node;
			MethodDeclaration replace = (MethodDeclaration) ASTNode.copySubtree(ast, target);
			// create new annotation
			SingleMemberAnnotation annotation = ast.newSingleMemberAnnotation();
			annotation.setTypeName(ast.newName("SuppressWarnings"));
			StringLiteral value = ast.newStringLiteral();
			value.setLiteralValue("deprecated");
			annotation.setValue(value);
			// insert annotation
			replace.modifiers().add(0, annotation);
			rewrite.replace(target, replace, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

	/**
	 * @param outputs: [WARNING] /home/ubuntu/workspace/VTR/subjects/commons-codec/src/test/java/org/apache/commons/codec/binary/Base64Test.java:[150,81] UTF_8 in org.apache.commons.codec.Charsets has been deprecated
	 * @return
	 */
	private List<String> deprecatedMessages(List<String> outputs) {
		List<String> ret = new ArrayList<>();
		for (String message: outputs) {
			String[] messages = message.split(" ");
			if (messages.length < 2) {
				continue;
			}
			if ( message.contains("WARNING") && (message.contains("非推奨") || message.contains("deprecated")) ) {
				ret.add(messages[1]);
			}
		}
		return ret;
	}
	
	private List<Integer> deprecatedNodePositions(List<String> deprecatedMessages, TestCase tc) {
		List<Integer> ret = new ArrayList<>();
		for (String message: deprecatedMessages) {
			if (isTarget(message, tc)) {
				int line  = getLinePos(message);
				int col  = getColPos(message);
				CompilationUnit cu = tc.getCompilationUnit();
				int pos = cu.getPosition(line, col);
				ret.add(pos);
			}
		}
		return ret;
	}
	
	/**
	 * 
	 * @param info: /Users/TK/workspace/VTR/subjects/commons-codec/src/test/java/org/apache/commons/codec/binary/Base64Test.java:[447,97]
	 * @return
	 */
	private int getLinePos(String info) {
		if (info.split(":").length < 2) {
			return -1;
		}
		String positionInfo = info.split(":")[1];
		int linePos = Integer.parseInt(positionInfo.split(",")[0].substring(1));
		return linePos;
	}
	
	/**
	 * 
	 * @param info: /Users/TK/workspace/VTR/subjects/commons-codec/src/test/java/org/apache/commons/codec/binary/Base64Test.java:[447,97]
	 * @return
	 */
	private int getColPos(String info) {
		if (info.split(":").length < 2) {
			return -1;
		}
		String positionInfo = info.split(":")[1];
		int colPos = Integer.parseInt(positionInfo.split(",")[1].substring(0, positionInfo.split(",")[1].length() - 1));
		return colPos;
	}
	
	/**
	 * 
	 * @param warning: /Users/TK/workspace/VTR/subjects/commons-codec/src/test/java/org/apache/commons/codec/binary/Base64Test.java:[447,97]
	 * @param tc
	 * @return
	 */
	private boolean isTarget(String warning, TestCase tc) {
		String testCaseName = warning.replace("/", ".");
		return testCaseName.contains(tc.getFullName().split("#")[0]);
	}
}

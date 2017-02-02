package jp.mzw.vtr.validate.javadoc;

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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UseCodeAnnotationsAtJavaDoc extends SimpleValidatorBase {
	protected Logger LOGGER = LoggerFactory.getLogger(UseCodeAnnotationsAtJavaDoc.class);

	public UseCodeAnnotationsAtJavaDoc(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		for (Object comment : tc.getCompilationUnit().getCommentList()) {
			if (comment instanceof Javadoc) {
				Javadoc javadoc = (Javadoc) comment;
				for (Object tag : javadoc.tags()) {
					if (tag instanceof TagElement) {
						TagElement element = (TagElement) tag;
						element.accept(new ASTVisitor() {
							@Override
							public boolean visit(TextElement node) {
								if (node.getText().contains("<tt>") && node.getText().contains("</tt>")) {
									ret.add(node);
								}
								return super.visit(node);
							}
						});
					} else {
						LOGGER.warn("Unknown tag-element class: {}", tag.getClass());
					}
				}
			}
		}
		return ret;
	}
	
	@Override
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> detects = detect(commit, tc, results);
		if (detects.isEmpty()) {
			return origin;
		}
		AST ast = detects.get(0).getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		for (ASTNode detect : detects) {
			TextElement node = (TextElement) detect;
			String text = node.getText().replaceAll("<tt>", "{@code ").replace("</tt>", "}");
			TextElement replacement = ast.newTextElement();
			replacement.setText(text);
			rewrite.replace(node, replacement, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}

}

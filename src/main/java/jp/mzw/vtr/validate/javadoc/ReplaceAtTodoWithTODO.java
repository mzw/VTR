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
import org.eclipse.jdt.core.dom.CompilationUnit;
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

public class ReplaceAtTodoWithTODO extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(ReplaceAtTodoWithTODO.class);
	
	public ReplaceAtTodoWithTODO(Project project) {
		super(project);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected List<ASTNode> detect(final Commit commit, final TestCase tc, final Results results) {
		List<ASTNode> ret = new ArrayList<>();
		final List<Object> targets = tc.getCompilationUnit().getCommentList();
		for (Object target: targets) {
			if (target instanceof Javadoc) {
				Javadoc comment = (Javadoc) target;
				List<Object> tags = comment.tags();
				for (Object tag: tags) {
					if (tag instanceof TagElement) {
						TagElement element = (TagElement) tag;
						String tagName = element.getTagName();
						if (tagName != null && tagName.contains("@todo")) {
							ret.add((TagElement) tag);
						}
					}
				}
			}
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results) throws IOException, MalformedTreeException, BadLocationException {
		// prepare
		CompilationUnit cu = tc.getCompilationUnit();
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// detect
		for (ASTNode node: detect(commit, tc, results)) {
			TagElement target = (TagElement) node;
			TextElement targetText = (TextElement) target.fragments().get(0);
			TagElement replace = ast.newTagElement();
			TextElement replaceText = ast.newTextElement();
			replaceText.setText("TODO" + targetText.getText());
			replace.fragments().add(replaceText);
			rewrite.replace(target, replace, null);
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

package jp.mzw.vtr.validate.javadoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.text.edits.MalformedTreeException;
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
		for (Object target : targets) {
			if (target instanceof Javadoc) {
				Javadoc comment = (Javadoc) target;
				List<Object> tags = comment.tags();
				for (Object tag : tags) {
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

	@Override
	protected String getModified(String origin, final Commit commit, final TestCase tc, final Results results)
			throws IOException, MalformedTreeException, BadLocationException {
		// detect
		char[] array = origin.toCharArray();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < array.length; i++) {

			boolean start = false;
			boolean in = false;
			for (ASTNode detect : detect(commit, tc, results)) {
				TagElement node = (TagElement) detect;
				if (i == node.getStartPosition()) {
					start = true;
					break;
				} else if (node.getStartPosition() < i && i < node.getStartPosition() + "@todo".length()) {
					in = true;
					break;
				}
			}
			if (start) {
				builder.append("TODO ");
			} else if (in) {
				// skip
			} else {
				builder.append(array[i]);
			}
		}
		return builder.toString();
	}
}

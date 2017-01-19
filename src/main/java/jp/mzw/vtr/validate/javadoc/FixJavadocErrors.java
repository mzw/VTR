package jp.mzw.vtr.validate.javadoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.validate.SimpleValidatorBase;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixJavadocErrors extends SimpleValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(FixJavadocErrors.class);

	public FixJavadocErrors(Project project) {
		super(project);
	}

	@Override
	protected List<ASTNode> detect(TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		final List<ASTNode> ret = new ArrayList<>();
		try {
			List<JavadocErrorMessage> messages = getJavadocErrorMessages(tc);
			if (!messages.isEmpty()) {
				ret.add(tc.getMethodDeclaration());
			}
		} catch (MavenInvocationException | InterruptedException e) {
			LOGGER.warn("Failed to get JavaDoc error/warning messages: {}", e.getMessage());
		}
		return ret;
	}

	public List<JavadocErrorMessage> getJavadocErrorMessages(TestCase tc) throws IOException, MavenInvocationException, InterruptedException {
		final List<JavadocErrorMessage> ret = new ArrayList<>();
		// Obtain class path of dependencies
		String classpath = null;
		List<String> outputs = MavenUtils.maven(projectDir, Arrays.asList("dependency:build-classpath"), mavenHome, true, false);
		for (String output : outputs) {
			if (!output.startsWith("[")) {
				classpath = output;
				break;
			}
		}
		if (classpath == null) {
			LOGGER.warn("Failed to get class-path of dependencies");
			return ret;
		}
		// Run JavaDoc
		List<String> cmd = Arrays.asList("javadoc", "-sourcepath", "src/main/java/:src/test/java/", "-classpath", classpath, getPackageName(tc));
		Pair<List<String>, List<String>> results = VtrUtils.exec(projectDir, cmd);
		if (results == null) {
			// No JavaDoc warnings/errors
			return ret;
		}
		List<String> lines = results.getRight();
		if (lines.size() % 3 != 0) {
			LOGGER.warn("Javadoc results might be unexpected: {}", results.getRight());
			return ret;
		}
		String target = VtrUtils.getFilePath(projectDir, tc.getTestFile());
		for (int i = 0; i < lines.size(); i += 3) {
			String[] split = lines.get(i).split(":");
			String filepath = split[0].trim();
			if (!filepath.equals(target)) {
				continue;
			}
			int lineno = Integer.parseInt(split[1].trim());
			String type = split[2].trim();
			String message = split[3].trim();
			int pos = lines.get(i + 2).length();
			ret.add(new JavadocErrorMessage(filepath, lineno, pos, type, message));
		}
		return ret;
	}

	public static class JavadocErrorMessage {

		private String filepath;
		private int lineno;
		private int pos;
		private String type;
		private String message;

		public JavadocErrorMessage(String filepath, int lineno, int pos, String type, String message) {
			this.filepath = filepath;
			this.lineno = lineno;
			this.pos = pos;
			this.type = type;
			this.message = message;
		}

		public String getFilePath() {
			return filepath;
		}

		public int getLineno() {
			return lineno;
		}

		public int getPos() {
			return pos;
		}

		public String getType() {
			return type;
		}

		public String getMessage() {
			return message;
		}

		public String toString() {
			return filepath + ": [" + type + "] " + message + " (" + lineno + ", " + pos + ")";
		}
	}

	@Override
	protected String getModified(String origin, TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> detects = detect(tc);
		if (detects.isEmpty()) {
			return origin;
		}
		AST ast = detects.get(0).getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		// TODO generate modified content
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

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
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
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
		
		private MethodDeclaration method;

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
		
		public void setMethod(MethodDeclaration method) {
			this.method = method;
		}
		
		public MethodDeclaration getMethod() {
			return method;
		}

		public String toString() {
			return filepath + ": [" + type + "] " + message + " (" + lineno + ", " + pos + ")";
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String getModified(String origin, TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		List<ASTNode> detects = detect(tc);
		if (detects.isEmpty()) {
			return origin;
		}
		AST ast = detects.get(0).getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		try {
			// get method declarations
			final List<MethodDeclaration> methods = new ArrayList<>();
			tc.getCompilationUnit().accept(new ASTVisitor() {
				@Override
				public boolean visit(MethodDeclaration node) {
					methods.add(node);
					return super.visit(node);
				}
			});
			// relate JavaDoc error messages to method declarations
			final List<JavadocErrorMessage> messages = getJavadocErrorMessages(tc);
			for (MethodDeclaration method : methods) {
				if (method.getJavadoc() == null) {
					continue;
				}
				int start = tc.getCompilationUnit().getLineNumber(method.getStartPosition());
				int end = tc.getCompilationUnit().getLineNumber(method.getStartPosition() + method.getLength());
				for (JavadocErrorMessage message : messages) {
					if (start <= message.getLineno() && message.getLineno() <= end) {
						message.setMethod(method);
					}
				}
			}
			// rewrite
			for (JavadocErrorMessage message : messages) {
				if (message.getMessage().startsWith("no @throws for ")) {
					String exception = message.getMessage().replace("no @throws for ", "");
					final List<ITypeBinding> exceptions = new ArrayList<>();
					for (Object object : message.getMethod().thrownExceptionTypes()) {
						if (object instanceof SimpleType) {
							SimpleType type = (SimpleType) object;
							exceptions.add(type.resolveBinding());
						} else {
							System.out.println("TODO: implement for thrown exception type, " + object.getClass());
						}
					}
					TagElement tag = null;
					for (ITypeBinding type : exceptions) {
						if (exception.equals(type.getQualifiedName())) {
							tag = ast.newTagElement();
							tag.setTagName(TagElement.TAG_THROWS);
							SimpleName name = ast.newSimpleName(type.getName());
							tag.fragments().add(name);
							TextElement text = ast.newTextElement();
							text.setText("May occur in some failure modes");
							tag.fragments().add(text);
							break;
						}
					}
					System.out.println(tag);
					if (tag != null) {
						Javadoc javadoc = message.getMethod().getJavadoc();
						Javadoc copy = (Javadoc) ASTNode.copySubtree(ast, javadoc);
						copy.tags().add(tag);
						rewrite.replace(javadoc, copy, null);
					}
				} else {
					System.out.println("TODO: " + message.getMessage());
				}
			}
		} catch (MavenInvocationException | InterruptedException e) {
			LOGGER.warn("Failed to get JavaDoc error/warning messages: {}", e.getMessage());
		}
		// modify
		Document document = new Document(origin);
		TextEdit edit = rewrite.rewriteAST(document, null);
		edit.apply(document);
		return document.get();
	}
}

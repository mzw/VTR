package jp.mzw.vtr.validate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class ValidatorUtils {

	/**
	 * Determine whether JUnit version is 4
	 * 
	 * @param pom
	 * @return
	 * @throws IOException
	 */
	public static boolean isJunit4(File pom) throws IOException {
		String content = FileUtils.readFileToString(pom);
		org.jsoup.nodes.Document document = Jsoup.parse(content, "", Parser.xmlParser());
		for (Element dependency : document.select("project dependencies dependency")) {
			if ("junit".equals(dependency.select("artifactid").text())) {
				Elements version = dependency.select("version");
				if (version != null) {
					if (version.text().startsWith("4")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Determine whether given test case has Test annotation
	 * 
	 * @param testCase
	 * @return
	 */
	public static boolean hasTestAnnotation(TestCase testCase) {
		for (ASTNode node : MavenUtils.getChildren(testCase.getMethodDeclaration())) {
			if (node instanceof Annotation) {
				Annotation annot = (Annotation) node;
				if ("Test".equals(annot.getTypeName().toString())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determine whether given test case has Test annotation
	 * 
	 * @param testCase
	 * @return
	 */
	public static Annotation getTestAnnotation(TestCase testCase) {
		for (ASTNode node : MavenUtils.getChildren(testCase.getMethodDeclaration())) {
			if (node instanceof Annotation) {
				Annotation annot = (Annotation) node;
				if ("Test".equals(annot.getTypeName().toString())) {
					return annot;
				}
			}
		}
		return null;
	}

	/**
	 * public void testFoo throws "exception, exception, ..., exception"
	 * 
	 * @param nodes
	 * @return
	 */
	public static List<SimpleType> getThrowedExceptions(List<ASTNode> nodes) {
		List<SimpleType> exceptions = new ArrayList<>();
		for (ASTNode node : nodes) {
			if (node instanceof SimpleType && node.getParent() instanceof MethodDeclaration) {
				exceptions.add((SimpleType) node);
			}
		}
		return exceptions;
	}

	/**
	 * Get comments in given node
	 * 
	 * @param tc
	 * @param node
	 * @return
	 */
	public static List<Comment> getComments(TestCase tc, ASTNode node) {
		int start = tc.getStartLineNumber(node);
		int end = tc.getEndLineNumber(node);
		List<Comment> comments = new ArrayList<>();
		for (Object object : tc.getCompilationUnit().getCommentList()) {
			Comment comment = (Comment) object;
			if (start <= tc.getStartLineNumber(comment) && tc.getEndLineNumber(comment) <= end) {
				comments.add(comment);
			}
		}
		return comments;
	}

	/**
	 * Get comment content
	 * 
	 * @param content
	 * @param comment
	 * @return
	 */
	public static String getCommentContent(String content, Comment comment) {
		char[] charArray = content.toCharArray();
		char[] ret = new char[comment.getLength()];
		for (int offset = 0; offset < comment.getLength(); offset++) {
			ret[offset] = charArray[comment.getStartPosition() + offset];
		}
		return String.valueOf(ret);
	}

	/**
	 * List of JUnit assert method names
	 */
	public static final String[] JUNIT_ASSERT_METHODS = { "assertArrayEquals", "assertEquals", "assertFalse", "assertNotNull", "assertNotSame", "assertNull",
			"assertSame", "assertThat", "assertTrue", "fail", };

	/**
	 * Determine whether given node has JUnit assert method invocation
	 * 
	 * @param node
	 * @return
	 */
	public static boolean hasAssertMethodInvocation(ASTNode node) {
		if (node instanceof MethodInvocation) {
			MethodInvocation method = (MethodInvocation) node;
			for (String name : JUNIT_ASSERT_METHODS) {
				if (name.equals(method.getName().toString())) {
					return true;
				}
			}
		}
		for (Object child : MavenUtils.getChildren(node)) {
			boolean has = hasAssertMethodInvocation((ASTNode) child);
			if (has) {
				return true;
			}
		}
		return false;
	}
}

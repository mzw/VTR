package jp.mzw.vtr.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCase {
	protected Logger LOGGER = LoggerFactory.getLogger(TestCase.class);

	MethodDeclaration method;
	CompilationUnit cu;

	String name;
	String className;
	double time;

	TestSuite testSuite;

	public TestCase(String name, String classname, MethodDeclaration method, CompilationUnit cu, TestSuite testSuite) {
		this.name = name;
		this.className = classname;

		this.method = method;
		this.cu = cu;

		this.testSuite = testSuite;
	}

	/**
	 * Get all nodes in this test case
	 * 
	 * @return
	 */
	protected List<ASTNode> getAllNodes() {
		AllElementsFindVisitor visitor = new AllElementsFindVisitor();
		cu.accept(visitor);
		return visitor.getNodes();
	}

	public List<ASTNode> getAllNodesIn(List<Integer> lineNumbers) {
		List<ASTNode> ret = new ArrayList<>();
		for (ASTNode node : this.getAllNodes()) {
			int start = this.cu.getLineNumber(node.getStartPosition());
			int end = this.cu.getLineNumber(node.getStartPosition() + node.getLength());
			boolean isIn = start <= end ? true : false; // end might be -1
			for (int lineno = start; lineno <= end; lineno++) {
				if (!lineNumbers.contains(lineno)) {
					isIn = false;
					break;
				}
			}
			if (isIn) {
				ret.add(node);
			}
		}
		return ret;
	}

	public List<ASTNode> getNodes() {
		List<Integer> lineNumbers = new ArrayList<>();
		for (int line = this.getStartLineNumber(); line <= this.getEndLineNumber(); line++) {
			lineNumbers.add(new Integer(line));
		}
		return this.getAllNodesIn(lineNumbers);
	}

	public String getName() {
		return this.name;
	}

	public String getClassName() {
		return this.className;
	}

	public MethodDeclaration getMethodDeclaration() {
		return this.method;
	}

	public CompilationUnit getCompilationUnit() {
		return this.cu;
	}

	public TestSuite getTestSuite() {
		return this.testSuite;
	}

	public File getTestFile() {
		return this.testSuite.getTestFile();
	}

	public int getStartLineNumber() {
		return this.cu.getLineNumber(this.method.getStartPosition());
	}

	public int getEndLineNumber() {
		return this.cu.getLineNumber(this.method.getStartPosition() + this.method.getLength());
	}

	public int getStartLineNumber(ASTNode node) {
		return this.cu.getLineNumber(node.getStartPosition());
	}

	public int getEndLineNumber(ASTNode node) {
		return this.cu.getLineNumber(node.getStartPosition() + node.getLength());
	}

	public List<Integer> getLineRange() {
		List<Integer> range = new ArrayList<>();
		for (int lineno = this.getStartLineNumber(); lineno <= this.getEndLineNumber(); lineno++) {
			range.add(lineno);
		}
		return range;
	}

	public String getFullName() {
		return this.className + "#" + this.name;
	}

	public static String getClassName(final String fullname) {
		return fullname.split("#")[0];
	}

	public static String getMethodName(final String fullname) {
		return fullname.split("#")[1];
	}

	public List<Comment> getComments() {
		List<Comment> ret = new ArrayList<>();
		for (Object object : cu.getCommentList()) {
			Comment comment = (Comment) object;
			if (method.getStartPosition() <= comment.getStartPosition() &&
					comment.getStartPosition() + comment.getLength() <= method.getStartPosition() + method.getLength()) {
				ret.add(comment);
			}
		}
		return ret;
	}
	
	public boolean changed(TestCase compare) {
		if (compare == null) {
			return true;
		}
		if (!method.toString().equals(compare.getMethodDeclaration().toString())) {
			return true;
		}
		List<Comment> comments = getComments();
		List<Comment> commentsUnderCompare = compare.getComments();
		if (comments.size() != commentsUnderCompare.size()) {
			return true;
		}
		for (int i = 0; i < getComments().size(); i++) {
			if (!comments.get(i).toString().equals(commentsUnderCompare.get(i).toString())) {
				return true;
			}
		}
		return false;
	}
}

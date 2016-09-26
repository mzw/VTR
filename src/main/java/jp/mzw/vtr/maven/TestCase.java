package jp.mzw.vtr.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.cluster.AllElementsFindVisitor;
import jp.mzw.vtr.cluster.difflib.ChunkTagRest;
import jp.mzw.vtr.git.Commit;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.jacoco.core.analysis.CoverageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import difflib.Chunk;
import difflib.Delta;

public class TestCase {
	protected Logger LOGGER = LoggerFactory.getLogger(TestCase.class);

	MethodDeclaration method;
	CompilationUnit cu;

	String name;
	String className;
	double time;

	TestSuite testSuite;

	CoverageBuilder covBuilder;

	Map<File, List<Integer>> coveredClassLinesMap;

	Delta<ChunkTagRest> delta;

	public TestCase(String name, String classname, MethodDeclaration method, CompilationUnit cu, TestSuite testSuite) {
		this.name = name;
		this.className = classname;

		this.method = method;
		this.cu = cu;

		this.testSuite = testSuite;
	}

	/**
	 * 
	 * @param delta
	 */
	public void setDelta(Delta<ChunkTagRest> delta) {
		this.delta = delta;
	}

	/**
	 * 
	 * @return
	 */
	public Delta<ChunkTagRest> getDelta() {
		return this.delta;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<ASTNode> getRevisedNodes() {
		List<Integer> range = getModifiedRange(this.delta.getRevised(), "+");
		return getModifiedNodes(range);
	}

	/**
	 * Get original nodes of modified test case
	 * @param delta Comes from modified test case
	 * @return Original nodes in this test case modified in modified test case
	 */
	public List<ASTNode> getOriginalNodes(Delta<ChunkTagRest> delta) {
		List<Integer> range = getModifiedRange(delta.getOriginal(), "-");
		return getModifiedNodes(range);
	}
	
	/**
	 * Get all nodes in this test case
	 * @return
	 */
	public List<ASTNode> getAllNodes() {
		AllElementsFindVisitor visitor = new AllElementsFindVisitor();
		cu.accept(visitor);
		return visitor.getNodes();
	}
	
	/**
	 * Get line range described in given chunk
	 * @param chunk Representing modification
	 * @param tag "+" if revised, otherwise "-" if original
	 * @return Line range
	 */
	private List<Integer> getModifiedRange(Chunk<ChunkTagRest> chunk, String tag) {
		List<Integer> ret = new ArrayList<>();
		List<ChunkTagRest> lines = chunk.getLines();
		for(int offset = 0; offset < lines.size(); offset++) {
			int pos = chunk.getPosition() + offset + 1;
			ChunkTagRest revisedLine = lines.get(offset);
			if (tag.equals(revisedLine.getTag()) && !"".equals(revisedLine.getRest().trim())) {
				ret.add(new Integer(pos));
			}					
		}
		return ret;
	}
	
	/**
	 * Get nodes in this test case if located in given line range
	 * @param range Target line range
	 * @return Corresponding nodes
	 */
	private List<ASTNode> getModifiedNodes(List<Integer> range) {
		List<ASTNode> ret = new ArrayList<>();
		for (ASTNode node : this.getAllNodes()) {
			int startPos = cu.getLineNumber(node.getStartPosition());
			int endPos = cu.getLineNumber(node.getStartPosition() + node.getLength());
			boolean inRange = false;
			for (int pos = startPos; pos <= endPos; pos++) {
				if (range.contains(pos)) {
					inRange = true;
				} else {
					inRange = false;
					break;
				}
			}
			if (inRange) {
				ret.add(node);
			}
		}
		return ret;
	}
	

	/**
	 * 
	 * @param covered
	 */
	public void setCoveredClassLinesMap(Map<File, List<Integer>> covered) {
		this.coveredClassLinesMap = covered;
	}

	/**
	 * 
	 * @return
	 */
	public Map<File, List<Integer>> getCoveredClassLinesMap() {
		return this.coveredClassLinesMap;
	}

	public String getName() {
		return this.name;
	}

	public String getClassName() {
		return this.className;
	}

	public String getRelativeFilePath(String mvn_prefix) {
		return mvn_prefix + "/" + this.className.replaceAll("\\.", "/") + ".java";
	}

	public TestSuite getTestSuite() {
		return this.testSuite;
	}

	public File getTestFile() {
		return this.testSuite.getTestFile();
	}

	List<Commit> test_commits;

	public void setTestCommits(List<Commit> test_commits) {
		this.test_commits = test_commits;
	}

	public List<Commit> getTestCommits() {
		return this.test_commits;
	}

	public int getStartLineNumber() {
		return this.cu.getLineNumber(this.method.getStartPosition());
	}

	public int getEndLineNumber() {
		return this.cu.getLineNumber(this.method.getStartPosition() + this.method.getLength());
	}

	public String getFullName() {
		return this.className + "#" + this.name;
	}

	public void setCoverageBuilder(CoverageBuilder builder) {
		this.covBuilder = builder;
	}

	public CoverageBuilder getCoverageBuilder() {
		return this.covBuilder;
	}

}

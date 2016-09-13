package jp.mzw.vtr.maven;

import java.io.File;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.git.Commit;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.jacoco.core.analysis.CoverageBuilder;

public class TestCase {

	MethodDeclaration method;
	CompilationUnit cu;

	String name;
	String className;
	double time;

	TestSuite testSuite;

	CoverageBuilder covBuilder;
	
	Map<File, List<Integer>> coveredClassLinesMap;

	public TestCase(String name, String classname, MethodDeclaration method, CompilationUnit cu, TestSuite testSuite) {
		this.name = name;
		this.className = classname;

		this.method = method;
		this.cu = cu;

		this.testSuite = testSuite;
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

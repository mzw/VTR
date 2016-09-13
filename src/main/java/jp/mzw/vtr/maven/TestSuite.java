package jp.mzw.vtr.maven;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class TestSuite {

	protected File testBaseDir;
	protected File testFile;
	String testClassName;
	List<TestCase> testCases;

	public TestSuite(File testBaseDir, File testFile) {
		this.testBaseDir = testBaseDir;
		this.testFile = testFile;

		String pathToTestFile = testFile.getAbsolutePath().substring(testBaseDir.getAbsolutePath().length() + 1);
		this.testClassName = pathToTestFile.replace(".java", "").replaceAll("/", ".");

		this.testCases = new ArrayList<TestCase>();
	}

	public TestSuite parseJuitTestCaseList() throws IOException {
		ArrayList<Character> _source = new ArrayList<Character>();
		FileReader reader = new FileReader(testFile);
		int ch;
		while ((ch = reader.read()) != -1) {
			_source.add((char) ch);
		}
		reader.close();

		char[] source = new char[_source.size()];
		for (int i = 0; i < _source.size(); i++) {
			source[i] = _source.get(i);
		}

		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(source);
		CompilationUnit cu = (CompilationUnit) parser.createAST(new NullProgressMonitor());

		AllMethodFindVisitor visitor = new AllMethodFindVisitor();
		cu.accept(visitor);
		List<MethodDeclaration> methods = visitor.getFoundMethods();

		for (MethodDeclaration method : methods) {
			if (MavenUtils.isJUnitTest(method)) {
				TestCase testcase = new TestCase(method.getName().getIdentifier(), testClassName, method, cu, this);
				testCases.add(testcase);
			}
		}

		return this;
	}

	public File getTestFile() {
		return this.testFile;
	}

	public List<TestCase> getTestCases() {
		return this.testCases;
	}

}

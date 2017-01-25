package jp.mzw.vtr.maven;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.core.VtrUtils;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSuite {
	protected static Logger LOGGER = LoggerFactory.getLogger(TestSuite.class);

	protected File testBaseDir;
	protected File testFile;
	String testClassName;
	List<TestCase> testCases;

	protected CompilationUnit cu;
	
	public TestSuite(File testBaseDir, File testFile) {
		this.testBaseDir = testBaseDir;
		this.testFile = testFile;

		String pathToTestFile = testFile.getAbsolutePath().substring(testBaseDir.getAbsolutePath().length() + 1);
		this.testClassName = pathToTestFile.replace(".java", "").replaceAll("/", ".");

		this.testCases = new ArrayList<TestCase>();
	}

	/**
	 * Parse source codes at level 1
	 * 
	 * @return
	 * @throws IOException
	 */
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
		this.cu = (CompilationUnit) parser.createAST(new NullProgressMonitor());

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
	
	/**
	 * Parse source codes at level 2
	 * 
	 * @param subjectDir
	 * @return
	 * @throws IOException
	 */
	public TestSuite parseJuitTestCaseList(File subjectDir) throws IOException {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setEnvironment(null, null, null, true);
		final Map<String, CompilationUnit> units = new HashMap<>();
		FileASTRequestor requestor = new FileASTRequestor() {
			@Override
			public void acceptAST(String sourceFilePath, CompilationUnit ast) {
				units.put(sourceFilePath, ast);
			}
		};
		parser.createASTs(getSources(subjectDir), null, new String[] {}, requestor, new NullProgressMonitor());
		this.cu = units.get(testFile.getCanonicalPath());
		
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

	/**
	 * Parse source codes at level 2
	 * 
	 * @param subjectDir
	 * @return
	 * @throws IOException
	 */
	public static Map<String, CompilationUnit> getFileUnitMap(File subjectDir) throws IOException {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setEnvironment(null, null, null, true);
		final Map<String, CompilationUnit> units = new HashMap<>();
		FileASTRequestor requestor = new FileASTRequestor() {
			@Override
			public void acceptAST(String sourceFilePath, CompilationUnit ast) {
				units.put(sourceFilePath, ast);
			}
		};
		parser.createASTs(getSources(subjectDir), null, new String[] {}, requestor, new NullProgressMonitor());
		return units;
	}
	
	public TestSuite setParseResults(CompilationUnit unit) {
		this.cu = unit;
		
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
	
	public String getPackageName() {
		return cu.getPackage().getName().toString();
	}

	protected static String[] getSources(File subjectDir) throws IOException {
		List<File> files = new ArrayList<>();
		files.addAll(VtrUtils.getFiles(new File(subjectDir, "src/main/java")));
		files.addAll(VtrUtils.getFiles(new File(subjectDir, "src/test/java")));
		String[] sources = new String[files.size()];
		for (int i = 0; i < files.size(); i++) {
			sources[i] = files.get(i).getCanonicalPath();
		}
		return sources;
	}

	public CompilationUnit getCompilationUnit() {
		return this.cu;
	}

	public File getTestFile() {
		return this.testFile;
	}

	public List<TestCase> getTestCases() {
		return this.testCases;
	}

	public void setTestCases(List<TestCase> testCases) {
		this.testCases = testCases;
	}

	public TestCase getTestCaseBy(String clazz, String method) {
		for (TestCase tc : this.testCases) {
			if (tc.getClassName().equals(clazz) && tc.getName().equals(method)) {
				return tc;
			}
		}
		return null;
	}

	/**
	 * Get test case having the same (class + method) as that of given test case
	 * from given test suites
	 * 
	 * @param testSuites
	 *            Search space
	 * @param testCase
	 *            Provides specific (class + method)
	 * @return Found test case or null if not found
	 */
	public static TestCase getTestCaseWithClassMethodName(List<TestSuite> testSuites, TestCase testCase) {
		for (TestSuite ts : testSuites) {
			for (TestCase tc : ts.getTestCases()) {
				if (tc.getFullName().equals(testCase.getFullName())) {
					return tc;
				}
			}
		}
		return null;
	}

	/**
	 * Get test case having the same (class + method) as that of given test case
	 * from given test suites
	 * 
	 * @param testSuites
	 *            Search space
	 * @param className
	 * @param methodName
	 * @return Found test case or null if not found
	 */
	public static TestCase getTestCaseWithClassMethodName(List<TestSuite> testSuites, String className, String methodName) {
		for (TestSuite ts : testSuites) {
			for (TestCase tc : ts.getTestCases()) {
				if (tc.getClassName().equals(className) && tc.getName().equals(methodName)) {
					return tc;
				}
			}
		}
		return null;
	}

}

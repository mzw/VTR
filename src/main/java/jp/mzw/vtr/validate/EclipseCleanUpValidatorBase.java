package jp.mzw.vtr.validate;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.text.edits.MalformedTreeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class EclipseCleanUpValidatorBase extends ValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(EclipseCleanUpValidatorBase.class);

	public EclipseCleanUpValidatorBase(Project project) {
		super(project);
	}

	@Override
	public void onCheckout(Commit commit) {
		try {
			for (TestSuite ts : MavenUtils.getTestSuites(this.projectDir)) {
				CompilationUnit cu = getCompilationUnit(ts.getTestFile());
				for (TestCase tc : ts.getTestCases()) {
					if (this.dupulicates.contains(tc.getFullName())) {
						continue;
					}
					List<ASTNode> detects = detect(tc, cu);
					if (!detects.isEmpty()) {
						this.dupulicates.add(tc.getFullName());
						for (ASTNode detect : detects) {
							int start = cu.getLineNumber(detect.getStartPosition());
							int end = cu.getLineNumber(detect.getStartPosition() + detect.getLength());
							ValidationResult vr = new ValidationResult(this.projectId, commit, tc, start, end, this);
							this.validationResultList.add(vr);
						}
					}
				}
			}
		} catch (IOException | CoreException e) {
			LOGGER.warn("Failed to checkout: {}", commit.getId());
		}
	}

	protected String[] getSources() throws IOException {
		List<File> files = new ArrayList<>();
		files.addAll(VtrUtils.getFiles(new File(this.projectDir, "src/main/java")));
		files.addAll(VtrUtils.getFiles(new File(this.projectDir, "src/test/java")));
		String[] sources = new String[files.size()];
		for (int i = 0; i < files.size(); i++) {
			sources[i] = files.get(i).getCanonicalPath();
		}
		return sources;
	}
	
	protected CompilationUnit getCompilationUnit(File file) throws IOException {
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
		parser.createASTs(getSources(), null, new String[] {}, requestor, new NullProgressMonitor());
		CompilationUnit cu = units.get(file.getCanonicalPath());
		return cu;
	}

	abstract protected List<ASTNode> detect(TestCase tc, CompilationUnit cu) throws CoreException;

	@Override
	public void generate(ValidationResult result) {
		try {
			TestCase tc = getTestCase(result);
			String origin = FileUtils.readFileToString(tc.getTestFile());
			String modified = getModified(origin.toString(), tc);
			List<String> patch = genPatch(origin, modified, tc.getTestFile(), tc.getTestFile());
			output(result, tc, patch);
		} catch (IOException | ParseException | GitAPIException | CoreException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}

	abstract protected String getModified(String origin, TestCase tc) throws IOException, CoreException, MalformedTreeException, BadLocationException;
}

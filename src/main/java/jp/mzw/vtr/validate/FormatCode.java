package jp.mzw.vtr.validate;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.AllMethodFindVisitor;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

public class FormatCode extends ValidatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(FormatCode.class);

	public FormatCode(Project project) {
		super(project);
	}

	@Override
	public void onCheckout(Commit commit) {
		try {
			for (TestSuite ts : MavenUtils.getTestSuites(this.projectDir)) {
				for (TestCase tc : ts.getTestCases()) {
					if (this.dupulicates.contains(tc.getFullName())) {
						continue;
					}
					try {
						if (detect(tc)) {
							this.dupulicates.add(tc.getFullName());
							ValidationResult vr = new ValidationResult(this.projectId, commit, tc, tc.getStartLineNumber(), tc.getEndLineNumber(), this);
							this.validationResultList.add(vr);
						}
					} catch (IOException | MalformedTreeException | BadLocationException e) {
						LOGGER.warn("Failed to invoke Checkstyle: {}", e.getMessage());
					}

				}
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to checkout: {}", commit.getId());
		}
	}

	private boolean detect(TestCase tc) throws IOException, MalformedTreeException, BadLocationException {
		String origin = FileUtils.readFileToString(tc.getTestFile());
		String modified = getModified(origin.toString(), tc);
		List<String> patch = genPatch(getTestCaseSource(origin, tc.getName()), getTestCaseSource(modified, tc.getName()), tc.getTestFile(), tc.getTestFile(),
				(tc.getStartLineNumber() - 1) * -1);
		if (patch.size() == 0) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void generate(ValidationResult result) {
		try {
			TestCase tc = getTestCase(result);
			String origin = FileUtils.readFileToString(tc.getTestFile());
			String modified = getModified(origin.toString(), tc);
			List<String> patch = genPatch(getTestCaseSource(origin, tc.getName()), getTestCaseSource(modified, tc.getName()), tc.getTestFile(),
					tc.getTestFile(), (tc.getStartLineNumber() - 1) * -1);
			output(result, tc, patch);
		} catch (IOException | ParseException | GitAPIException | MalformedTreeException | BadLocationException e) {
			LOGGER.warn("Failed to generate patch: {}", e.getMessage());
		}
	}

	/**
	 * 
	 * @param origin
	 * @param tc
	 * @param version
	 * @return
	 * @throws MalformedTreeException
	 * @throws BadLocationException
	 * @throws IOException
	 */
	private String getModified(String origin, TestCase tc) throws MalformedTreeException, BadLocationException, IOException {
		// Make document from original source code
		IDocument document = new Document(origin);
		String delim = TextUtilities.getDefaultLineDelimiter(document);
		// Instantiate CodeFormatter
		Map<String, String> options = new HashMap<>();
		options.put(JavaCore.COMPILER_SOURCE, getJavaVersion());
		CodeFormatter formatter = ToolFactory.createCodeFormatter(options);
		// Format
		TextEdit edit = formatter.format(CodeFormatter.K_COMPILATION_UNIT, document.get(), 0, document.getLength(), 0, delim);
		if (edit != null) {
			edit.apply(document);
		} else {
			LOGGER.warn("Failed to code format");
		}
		return document.get();
	}

	/**
	 * Get Java version from pom.xml
	 * 
	 * @param pom
	 * @return
	 * @throws IOException
	 */
	private String getJavaVersion() throws IOException {
		File pom = getPomFile();
		String content = FileUtils.readFileToString(pom);
		org.jsoup.nodes.Document document = org.jsoup.Jsoup.parse(content, "", org.jsoup.parser.Parser.xmlParser());
		org.jsoup.select.Elements elements = document.select("properties");
		for (Iterator<org.jsoup.nodes.Element> it = elements.iterator(); it.hasNext();) {
			org.jsoup.nodes.Element element = it.next();
			for (org.jsoup.nodes.Element child : element.children()) {
				if ("maven.compile.source".equals(child.tagName())) {
					return child.text();
				}
			}
		}
		LOGGER.warn("Java version not found");
		return "1.8"; // TODO as default
	}
}

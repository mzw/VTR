package jp.mzw.vtr.validate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import difflib.DiffUtils;
import difflib.Patch;
import jp.mzw.vtr.CLI;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.AllMethodFindVisitor;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;

abstract public class ValidatorBase implements Validator.Listener {
	protected static Logger LOGGER = LoggerFactory.getLogger(ValidatorBase.class);

	public static final String VALIDATOR_DIRNAME = "validate";
	public static final String VALIDATOR_FILENAME = "results.csv";

	public static final String VALIDATORS_LIST = "validators_list.txt";

	protected String projectId;
	protected File projectDir;
	protected File outputDir;
	protected File mavenHome;

	protected List<ValidationResult> validationResultList;
	protected List<String> dupulicates;

	public ValidatorBase(Project project) {
		this.projectId = project.getProjectId();
		this.projectDir = project.getProjectDir();
		this.mavenHome = project.getMavenHome();
		this.validationResultList = new ArrayList<>();
		this.dupulicates = new ArrayList<>();
	}
	
	abstract public void generate(ValidationResult result);

	/**
	 * Get validator list from resources
	 * 
	 * @param project
	 *            Project
	 * @return Validators
	 * @throws IOException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	public static List<ValidatorBase> getValidators(Project project, String filename) throws IOException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		InputStream is = ValidatorBase.class.getClassLoader().getResourceAsStream(filename);
		List<String> lines = IOUtils.readLines(is);
		List<ValidatorBase> validators = new ArrayList<>();
		for (String line : lines) {
			try {
				Class<?> clazz = Class.forName(line);
				ValidatorBase validator = (ValidatorBase) clazz.getConstructor(Project.class).newInstance(project);
				validators.add(validator);
				LOGGER.info("Register validator: {}", line);
			} catch (ClassNotFoundException e) {
				LOGGER.warn("Invalid validator: {}", line);
			}
		}
		return validators;
	}

	/**
	 * 
	 * @return
	 */
	public List<ValidationResult> getValidationResultList() {
		return this.validationResultList;
	}

	/**
	 * Output validation results in CSV file
	 * 
	 * @param project
	 *            Project
	 * @param validators
	 *            Validator
	 * @throws IOException
	 *             When fail to write CSV file
	 */
	public static void output(Project project, List<ValidatorBase> validators) throws IOException {
		// Destination
		File projectDir = new File(project.getOutputDir(), project.getProjectId());
		File validateDir = new File(projectDir, VALIDATOR_DIRNAME);
		File file = new File(validateDir, VALIDATOR_FILENAME);
		// Container
		StringBuilder builder = new StringBuilder();
		builder.append(getCsvHeader());
		// Update
		if (file.exists()) {
			List<ValidationResult> prevVRList = parse(file);
			for (ValidatorBase validator : validators) {
				for (ValidationResult vr : validator.getValidationResultList()) {
					ValidationResult contains = null;
					for (ValidationResult prev : prevVRList) {
						if (vr.equals(prev)) {
							contains = prev;
							break;
						}
					}
					if (contains != null) {
						builder.append(contains.toCsv());
					} else {
						builder.append(vr.toCsv());
					}
				}
			}
		}
		// New
		else {
			for (ValidatorBase validator : validators) {
				for (ValidationResult vr : validator.getValidationResultList()) {
					builder.append(vr.toCsv());
				}
			}
		}
		// Write
		FileUtils.writeStringToFile(file, builder.toString());
	}

	/**
	 * Get CSV header
	 * 
	 * @return CSV header
	 */
	protected static String getCsvHeader() {
		StringBuilder builder = new StringBuilder();
		builder.append("ProjectId");
		builder.append(",");
		builder.append("CommitId");
		builder.append(",");
		builder.append("TestCaseClassName");
		builder.append(",");
		builder.append("TestCaseMethodName");
		builder.append(",");
		builder.append("StartLineNumber");
		builder.append(",");
		builder.append("EndLineNumber");
		builder.append(",");
		builder.append("ValidatorName");
		builder.append(",");
		builder.append("TruePositive");
		builder.append(",");
		builder.append("ActuallyModified");
		builder.append(",");
		builder.append("ProperlyModified");
		builder.append("\n");
		return builder.toString();
	}

	/**
	 * Parse existing validation reuslts
	 * 
	 * @param project
	 * @return
	 * @throws IOException
	 */
	public static List<ValidationResult> parse(Project project) throws IOException {
		List<ValidationResult> ret = new ArrayList<>();
		File projectDir = new File(project.getOutputDir(), project.getProjectId());
		File validateDir = new File(projectDir, VALIDATOR_DIRNAME);
		File file = new File(validateDir, VALIDATOR_FILENAME);
		if (file.exists()) {
			ret = parse(file);
		}
		return ret;
	}

	/**
	 * Parse existing validation results
	 * 
	 * @param file
	 *            Contains existing validation results
	 * @return Existing validation results
	 * @throws IOException
	 *             When given file is not found
	 */
	public static List<ValidationResult> parse(File file) throws IOException {
		// Read
		String content = FileUtils.readFileToString(file);
		CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
		List<CSVRecord> records = parser.getRecords();
		int size = records.size();
		// Parse
		List<ValidationResult> ret = new ArrayList<>();
		for (int i = 1; i < size; i++) { // ignore CSV header
			CSVRecord record = records.get(i);
			String projectId = record.get(0);
			String commitId = record.get(1);
			String testCaseClassName = record.get(2);
			String testCaseMethodName = record.get(3);
			int startLineNumber = Integer.parseInt(record.get(4));
			int endLineNumber = Integer.parseInt(record.get(5));
			String validatorName = record.get(6);
			Boolean truePositive = record.get(7).equals("") ? null : Boolean.parseBoolean(record.get(7));
			Boolean actuallyModified = record.get(8).equals("") ? null : Boolean.parseBoolean(record.get(8));
			Boolean properlyModified = record.get(9).equals("") ? null : Boolean.parseBoolean(record.get(9));
			ValidationResult vr = new ValidationResult(projectId, commitId, testCaseClassName, testCaseMethodName, startLineNumber, endLineNumber,
					validatorName, truePositive, actuallyModified, properlyModified);
			ret.add(vr);
		}
		// Return
		return ret;
	}

	protected TestCase getTestCase(ValidationResult result) throws IOException, ParseException, GitAPIException {
		this.projectId = result.getProjectId();
		Project project = new Project(projectId).setConfig(CLI.CONFIG_FILENAME);
		this.projectDir = project.getProjectDir();
		this.outputDir = project.getOutputDir();
		CheckoutConductor cc = new CheckoutConductor(project);
		// Commit
		String commitId = result.getCommitId();
		Commit commit = new Commit(commitId, null);
		cc.checkout(commit);
		// Detect test case
		String clazz = result.getTestCaseClassName();
		String method = result.getTestCaseMathodName();
		List<TestSuite> testSuites = MavenUtils.getTestSuitesAtLevel2(project.getProjectDir());
		for (TestSuite ts : testSuites) {
			TestCase tc = ts.getTestCaseBy(clazz, method);
			if (tc != null) {
				return tc;
			}
		}
		return null;
	}

	protected File getPomFile() {
		return new File(this.projectDir, "pom.xml");
	}

	protected void output(ValidationResult result, TestCase tc, List<String> patch) throws IOException {
		File projectDir = new File(this.outputDir, this.projectId);
		File validateDir = new File(projectDir, ValidatorBase.VALIDATOR_DIRNAME);
		File commitDir = new File(validateDir, result.getCommitId());
		File patternDir = new File(commitDir, result.getValidatorName());
		if (!patternDir.exists()) {
			patternDir.mkdirs();
		}
		File patchFile = new File(patternDir, tc.getFullName() + ".patch");
		FileUtils.writeLines(patchFile, patch);
		LOGGER.info("Succeeded to generate patch: {}", tc.getTestFile().getPath());
	}

	/**
	 * Generate patch
	 * 
	 * @param origin
	 * @param modified
	 * @param file
	 * @return
	 */
	public static List<String> genPatch(String origin, String modified, File org, File mod) {
		List<String> originList = Arrays.asList(origin.split("\n"));
		List<String> modifyList = Arrays.asList(modified.split("\n"));
		Patch<String> patch = DiffUtils.diff(originList, modifyList);
		return DiffUtils.generateUnifiedDiff(org.getAbsolutePath(), mod.getAbsolutePath(), originList, patch, 0);
	}

	/**
	 * Generate patch
	 * 
	 * @param origin
	 * @param modified
	 * @param file
	 * @param contextSize
	 * @return
	 */
	public static List<String> genPatch(String origin, String modified, File org, File mod, int contextSize) {
		List<String> originList = Arrays.asList(origin.split("\n"));
		List<String> modifyList = Arrays.asList(modified.split("\n"));
		Patch<String> patch = DiffUtils.diff(originList, modifyList);
		return DiffUtils.generateUnifiedDiff(org.getAbsolutePath(), mod.getAbsolutePath(), originList, patch, contextSize);
	}

	public static List<String> genPatch(String origin, String modified, TestCase tc) {
		return genPatch(getTestCaseSource(origin, tc.getName()), getTestCaseSource(modified, tc.getName()), tc.getTestFile(),
				tc.getTestFile(), (tc.getStartLineNumber() - 1) * -1);
	}

	public static String getTestCaseSource(String content, String methodName) {
		char[] array = content.toCharArray();
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(array);
		CompilationUnit cu = (CompilationUnit) parser.createAST(new NullProgressMonitor());
		AllMethodFindVisitor visitor = new AllMethodFindVisitor();
		cu.accept(visitor);
		List<MethodDeclaration> methods = visitor.getFoundMethods();
		for (MethodDeclaration method : methods) {
			if (method.getName().getIdentifier().equals(methodName)) {
				int start = cu.getLineNumber(method.getStartPosition());
				int end = cu.getLineNumber(method.getStartPosition() + method.getLength());
				List<String> lines = toList(content);
				StringBuilder builder = new StringBuilder();
				String delim = "";
				for (int line = start; line <= end; line++) {
					try {
						builder.append(delim).append(lines.get(line));
						delim = "\n";
					} catch (IndexOutOfBoundsException e) {
						if (line == lines.size()) {
							// No new line at the end of file content
						} else {
							e.printStackTrace();
						}
					}
				}
				return builder.toString();
			}
		}
		return "";
	}

	public static List<String> toList(String content) {
		List<String> ret = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (char c : content.toCharArray()) {
			if (c == '\n') {
				ret.add(line.toString());
				line = new StringBuilder();
			} else {
				line.append(c);
			}
		}
		return ret;
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

	public static String format(String source) throws MalformedTreeException, BadLocationException {
		@SuppressWarnings("rawtypes")
		Map options = DefaultCodeFormatterConstants.getEclipseDefaultSettings();
		final CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(options);
		final TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, source, 0, source.length(), 0, System.getProperty("line.separator"));
		IDocument document = new Document(source);
		edit.apply(document);
		return document.get();
	}

	public static String getPackageName(TestCase tc) {
		String className = tc.getClassName();
		int index = className.lastIndexOf('.');
		if (index < 0) {
			return className;
		}
		return className.substring(0, index);
	}
}

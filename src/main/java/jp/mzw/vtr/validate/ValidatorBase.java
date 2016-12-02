package jp.mzw.vtr.validate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import difflib.DiffUtils;
import difflib.Patch;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;

abstract public class ValidatorBase implements CheckoutConductor.Listener {
	protected static Logger LOGGER = LoggerFactory.getLogger(ValidatorBase.class);

	public static final String VALIDATOR_DIRNAME = "validate";
	public static final String VALIDATOR_FILENAME = "results.csv";

	public static final String VALIDATORS_LIST = "validators_list.txt";

	protected String projectId;
	protected File projectDir;

	protected List<ValidationResult> validationResultList;
	protected List<String> dupulicates;

	public ValidatorBase(Project project) {
		this.projectId = project.getProjectId();
		this.projectDir = project.getProjectDir();
		this.validationResultList = new ArrayList<>();
		this.dupulicates = new ArrayList<>();
	}

	@Override
	abstract public void onCheckout(Commit commit);

	abstract public void generate(ValidationResult result);

	/**
	 * TODO: Give validator list from resources
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
	 * Determine whether given node has JUnit assert method invocation
	 * 
	 * @param node
	 * @return
	 */
	protected boolean hasAssertMethodInvocation(ASTNode node) {
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

	/**
	 * List of JUnit assert method names
	 */
	public static final String[] JUNIT_ASSERT_METHODS = { "assertArrayEquals", "assertEquals", "assertFalse", "assertNotNull", "assertNotSame", "assertNull",
			"assertSame", "assertThat", "assertTrue", "fail", };

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
}

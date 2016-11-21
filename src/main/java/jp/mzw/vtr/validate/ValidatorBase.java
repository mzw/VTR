package jp.mzw.vtr.validate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;

abstract public class ValidatorBase implements CheckoutConductor.Listener {
	protected static Logger LOGGER = LoggerFactory.getLogger(ValidatorBase.class);

	public static final String VALIDATOR_DIRNAME = "validate";
	public static final String VALIDATOR_FILENAME = "results.csv";

	protected String projectId;
	protected File projectDir;

	protected List<ValidationResult> validationResultList;

	public ValidatorBase(Project project) {
		this.projectId = project.getProjectId();
		this.projectDir = project.getProjectDir();
		// this.outputDir = project.getOutputDir();
		this.validationResultList = new ArrayList<>();
	}

	@Override
	abstract public void onCheckout(Commit commit);

	/**
	 * TODO: Give validator list from resources
	 * 
	 * @param project
	 *            Project
	 * @return Validators
	 */
	public static List<ValidatorBase> getValidators(Project project) {
		List<ValidatorBase> ret = new ArrayList<>();
		ret.add(new DoNotSwallowTestErrorsSilently(project));
		return ret;
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
	 * Parse existing validation results
	 * 
	 * @param file
	 *            Contains existing validation results
	 * @return Existing validation results
	 * @throws IOException
	 *             When given file is not found
	 */
	protected static List<ValidationResult> parse(File file) throws IOException {
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
}

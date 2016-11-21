package jp.mzw.vtr.validate;

import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.TestCase;

public class ValidationResult {

	private String projectId;
	private String commitId;
	private String testCaseClassName;
	private String testCaseMethodName;
	private int startLineNumber;
	private int endLineNumber;
	private String validatorName;

	private Boolean truePositive;
	private Boolean actuallyModified;
	private Boolean properlyModified;

	public ValidationResult(String projectId, Commit commit, TestCase testCase, int startLineNumber, int endLineNumber, ValidatorBase validator) {
		this.projectId = projectId;
		this.commitId = commit.getId();
		this.testCaseClassName = testCase.getClassName();
		this.testCaseMethodName = testCase.getName();
		this.startLineNumber = startLineNumber;
		this.endLineNumber = endLineNumber;
		this.validatorName = validator.getClass().toString();
		this.truePositive = null;
		this.actuallyModified = null;
		this.properlyModified = null;
	}

	public ValidationResult(String projectId, String commitId, String testCaseClassName, String testCaseMethodName, int startLineNumber, int endLineNumber,
			String validatorName, Boolean truePositive, Boolean actuallyModified, Boolean properlyModified) {
		this.projectId = projectId;
		this.commitId = commitId;
		this.testCaseClassName = testCaseClassName;
		this.testCaseMethodName = testCaseMethodName;
		this.startLineNumber = startLineNumber;
		this.endLineNumber = endLineNumber;
		this.validatorName = validatorName;
		this.truePositive = truePositive;
		this.actuallyModified = actuallyModified;
		this.properlyModified = properlyModified;
	}
	
	public String getProjectId() {
		return this.projectId;
	}
	
	public String getCommitId() {
		return this.commitId;
	}
	
	public String getTestCaseClassName() {
		return this.testCaseClassName;
	}
	
	public String getTestCaseMathodName() {
		return this.testCaseMethodName;
	}
	
	public int getStartLineNumber() {
		return this.startLineNumber;
	}
	
	public int getEndLineNumber() {
		return this.endLineNumber;
	}
	
	public String getValidatorName() {
		return this.validatorName;
	}

	public Boolean isTruePositive() {
		return this.truePositive;
	}

	public ValidationResult setTruePositive(Boolean tp) {
		this.truePositive = tp;
		return this;
	}

	public Boolean isActuallyModified() {
		return this.actuallyModified;
	}

	public ValidationResult setActuallyModified(boolean am) {
		this.actuallyModified = am;
		return this;
	}

	public Boolean isProperlyModified() {
		return this.properlyModified;
	}

	public ValidationResult setProperlyModified(Boolean pm) {
		this.properlyModified = pm;
		return this;
	}

	public String toCsv() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.projectId);
		builder.append(",");
		builder.append(this.commitId);
		builder.append(",");
		builder.append(this.testCaseClassName);
		builder.append(",");
		builder.append(this.testCaseMethodName);
		builder.append(",");
		builder.append(this.startLineNumber);
		builder.append(",");
		builder.append(this.endLineNumber);
		builder.append(",");
		builder.append(this.validatorName);
		builder.append(",");
		builder.append(this.truePositive != null ? this.truePositive : "");
		builder.append(",");
		builder.append(this.actuallyModified != null ? this.actuallyModified : "");
		builder.append(",");
		builder.append(this.properlyModified != null ? this.properlyModified : "");
		builder.append("\n");
		return builder.toString();
	}

	public boolean equals(ValidationResult vr) {
		if (this.projectId.equals(vr.getProjectId()) &&
				this.commitId.equals(vr.getCommitId()) &&
				this.testCaseClassName.equals(vr.getTestCaseClassName()) &&
				this.testCaseMethodName.equals(vr.getTestCaseMathodName()) &&
				this.startLineNumber == vr.getStartLineNumber() &&
				this.endLineNumber == vr.getEndLineNumber() &&
				this.validatorName.equals(vr.getValidatorName())) {
			return true;
		}
		return false;
	}
}

package jp.mzw.vtr.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.JavadocUtils.JavadocErrorMessage;
import jp.mzw.vtr.validate.ValidatorBase;

public class Results {

	protected List<String> compileOutputs;
	protected List<String> compileErrors;

	protected List<String> javadocResults;
	protected Map<String, List<JavadocErrorMessage>> javadocErrorMessages;

	protected Results(List<String> outputs, List<String> errors) {
		this.compileOutputs = outputs;
		this.compileErrors = errors;
	}

	public static Results of(List<String> outputs, List<String> errors) {
		return new Results(outputs, errors);
	}

	public List<String> getCompileOutputs() {
		return compileOutputs;
	}

	public List<String> getCompileErrors() {
		return compileErrors;
	}

	public void setJavadocResults(List<String> results) {
		this.javadocResults = results;
		this.javadocErrorMessages = JavadocUtils.parseJavadocErrorMessages(results);
	}

	public List<JavadocErrorMessage> getJavadocErrorMessages(File projectDir, File file) {
		String filepath = VtrUtils.getFilePath(projectDir, file);
		List<JavadocErrorMessage> messages = javadocErrorMessages.get(filepath);
		if (messages == null) {
			messages = new ArrayList<JavadocErrorMessage>();
			javadocErrorMessages.put(filepath, messages);
		}
		return messages;
	}

	public static final String VALIDATE_RESULTS_DIRNAME = "results";
	public static final String COMPILE_OUTPUTS_FILENAME = "compile_outputs.txt";
	public static final String COMPILE_ERRORS_FILENAME = "compile_errors.txt";
	public static final String JAVADOC_RESULTS_FILENAME = "javadoc_error_messages.txt";
	public static final String PATCH_BEFORE_RUNTIME_OUTPUTS_FILENAME = "before_runtime_outputs.txt";
	public static final String PATCH_AFTER_RUNTIME_OUTPUTS_FILENAME = "after_runtime_outputs.txt";

	public static File getCommitDir(File outputDir, String projectId, Commit commit) {
		File projectDir = new File(outputDir, projectId);
		File validateDir = new File(projectDir, ValidatorBase.VALIDATOR_DIRNAME);
		File resultsDir = new File(validateDir, VALIDATE_RESULTS_DIRNAME);
		File commitDir = new File(resultsDir, commit.getId());
		return commitDir;
	}

	public void output(File outputDir, String projectId, Commit commit) throws IOException {
		File commitDir = getCommitDir(outputDir, projectId, commit);
		File compileOutputsFile = new File(commitDir, COMPILE_OUTPUTS_FILENAME);
		FileUtils.writeLines(compileOutputsFile, compileOutputs);

		File compileErrorsFile = new File(commitDir, COMPILE_ERRORS_FILENAME);
		FileUtils.writeLines(compileErrorsFile, compileErrors);

		File javadocResultsFile = new File(commitDir, JAVADOC_RESULTS_FILENAME);
		FileUtils.writeLines(javadocResultsFile, javadocResults);
	}

	public void output(File outputDir) throws IOException {
		File compileOutputsFile = new File(outputDir, COMPILE_OUTPUTS_FILENAME);
		FileUtils.writeLines(compileOutputsFile, compileOutputs);

		File compileErrorsFile = new File(outputDir, COMPILE_ERRORS_FILENAME);
		FileUtils.writeLines(compileErrorsFile, compileErrors);

		File javadocResultsFile = new File(outputDir, JAVADOC_RESULTS_FILENAME);
		FileUtils.writeLines(javadocResultsFile, javadocResults);
	}

	public void compileOutput(File outputDir) throws IOException {
		File compileOutputsFile = new File(outputDir, COMPILE_OUTPUTS_FILENAME);
		FileUtils.writeLines(compileOutputsFile, compileOutputs);

		File compileErrorsFile = new File(outputDir, COMPILE_ERRORS_FILENAME);
		FileUtils.writeLines(compileErrorsFile, compileErrors);
	}
	public void javadocOutput(File outputDir) throws IOException {
		File javadocResultsFile = new File(outputDir, JAVADOC_RESULTS_FILENAME);
		FileUtils.writeLines(javadocResultsFile, javadocResults);
	}
	public void patchBeforeRuntimeOutput(File outputDir) throws IOException {
		File runtimeOutputsFile = new File(outputDir, PATCH_BEFORE_RUNTIME_OUTPUTS_FILENAME);
		// compileOutputs has a runtime output. so, below statement isn't bug.
		// if you have a question, ask Tsukamoto!
		FileUtils.writeLines(runtimeOutputsFile, compileOutputs);
	}
	public void patchAfterRuntimeOutput(File outputDir) throws IOException {
		File runtimeOutputsFile = new File(outputDir, PATCH_AFTER_RUNTIME_OUTPUTS_FILENAME);
		// compileOutputs has a runtime output. so, below statement isn't bug.
		// if you have a question, ask Tsukamoto!
		FileUtils.writeLines(runtimeOutputsFile, compileOutputs);
	}
	public static boolean is(File outputDir, String projectId, Commit commit) {
		File commitDir = getCommitDir(outputDir, projectId, commit);
		return commitDir.exists();
	}

	public static File getDir(File outputDir, String projectId, Commit commit) {
		File commitDir = getCommitDir(outputDir, projectId, commit);
		return commitDir;
	}

	public static Results parse(File outputDir, String projectId, Commit commit) throws IOException {
		File commitDir = getCommitDir(outputDir, projectId, commit);
		// Parse compile results
		File compileOutputsFile = new File(commitDir, COMPILE_OUTPUTS_FILENAME);
		List<String> compileOutputs = FileUtils.readLines(compileOutputsFile);
		File compileErrorsFile = new File(commitDir, COMPILE_ERRORS_FILENAME);
		List<String> compileErrors = FileUtils.readLines(compileErrorsFile);
		Results results = Results.of(compileOutputs, compileErrors);
		// Parse JavaDoc results
		File javadocResultsFile = new File(commitDir, JAVADOC_RESULTS_FILENAME);
		List<String> javadocResults = FileUtils.readLines(javadocResultsFile);
		results.setJavadocResults(javadocResults);
		// Return
		return results;
	}

	public boolean isBuildSuccess() {
		return compileOutputs.contains("[INFO] BUILD SUCCESS");
	}
}

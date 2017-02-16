package jp.mzw.vtr.repair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.CompilerPlugin;
import jp.mzw.vtr.maven.JavadocUtils;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.validate.ValidatorBase;
import jp.mzw.vtr.validate.javadoc.FixJavadocErrors;
import jp.mzw.vtr.validate.javadoc.ReplaceAtTodoWithTODO;
import jp.mzw.vtr.validate.javadoc.UseCodeAnnotationsAtJavaDoc;
import jp.mzw.vtr.validate.outputs.RemovePrintStatements;
import jp.mzw.vtr.validate.outputs.suppress_warnings.DeleteUnnecessaryAssignmenedVariables;
import jp.mzw.vtr.validate.outputs.suppress_warnings.IntroduceAutoBoxing;
import jp.mzw.vtr.validate.outputs.suppress_warnings.RemoveUnnecessaryCasts;
import jp.mzw.vtr.validate.outputs.suppress_warnings.add_suppress_warnings_annotation.AddSuppressWarningsDeprecationAnnotation;
import jp.mzw.vtr.validate.outputs.suppress_warnings.add_suppress_warnings_annotation.AddSuppressWarningsRawtypesAnnotation;
import jp.mzw.vtr.validate.outputs.suppress_warnings.add_suppress_warnings_annotation.AddSuppressWarningsUncheckedAnnotation;

public class Output extends EvaluatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(Output.class);

	private Map<Repair, Result> beforeResults;
	private Map<Repair, Result> afterResults;

	public Output(Project project) {
		super(project);
		beforeResults = new HashMap<>();
		afterResults = new HashMap<>();
	}

	@Override
	public void evaluateBefore(Repair repair) {
		// NOP because before outputs should be stored when running
		// Validator/PatchGenerator
	}

	@Override
	public List<Class<? extends ValidatorBase>> includeValidators() {
		final List<Class<? extends ValidatorBase>> includes = new ArrayList<>();

		/*
		 * Sharp-shooting outputs
		 */
		// Suppress warnings
		includes.add(DeleteUnnecessaryAssignmenedVariables.class);
		includes.add(IntroduceAutoBoxing.class);
		includes.add(RemoveUnnecessaryCasts.class);
		includes.add(AddSuppressWarningsDeprecationAnnotation.class);
		includes.add(AddSuppressWarningsRawtypesAnnotation.class);
		includes.add(AddSuppressWarningsUncheckedAnnotation.class);
		// Prints for debugging
		includes.add(RemovePrintStatements.class);
		// JavaDoc warnings and errors
		includes.add(FixJavadocErrors.class);
		includes.add(ReplaceAtTodoWithTODO.class);
		includes.add(UseCodeAnnotationsAtJavaDoc.class);

		return includes;
	}

	@Override
	public void evaluateAfter(Repair repair) {
		try {
			MavenUtils.maven(projectDir, Arrays.asList("clean"), mavenHome);
			CompilerPlugin cp = new CompilerPlugin(projectDir);
			boolean modified = cp.instrument();
			jp.mzw.vtr.maven.Results results = MavenUtils.maven(projectDir, Arrays.asList("test-compile"), mavenHome);
			List<String> javadocResults = JavadocUtils.executeJavadoc(projectDir, mavenHome);
			results.setJavadocResults(javadocResults);
			results.output(getAfterDir(repair));
			if (modified) {
				cp.revert();
			}
		} catch (MavenInvocationException | IOException | InterruptedException | DocumentException e) {
			LOGGER.warn("Failed to get/store outputs: {} at {} with {}", repair.getTestCaseFullName(), repair.getCommit().getId(), this.getClass().getName());
		}
	}

	private File getAfterDir(Repair repair) {
		File rootDir = EvaluatorBase.getRepairDir(outputDir, projectId);
		File mutationDir = new File(rootDir, "output");
		File commitDir = new File(mutationDir, repair.getCommit().getId());
		File validatorDir = new File(commitDir, repair.getValidatorName());
		String patchName = repair.getPatchFile().getName().replace(".patch", "");
		File patchDir = new File(validatorDir, patchName);
		if (!patchDir.exists()) {
			patchDir.mkdirs();
		}
		return patchDir;
	}

	@Override
	public void compare(Repair repair) {
		File beforeDir = Results.getDir(outputDir, projectId, repair.getCommit());
		File beforeCompileOutputs = new File(beforeDir, Results.COMPILE_OUTPUTS_FILENAME);
		// File beforeCompileErrors = new File(beforeDir,
		// Results.COMPILE_ERRORS_FILENAME);
		File beforeJavadocErrors = new File(beforeDir, Results.JAVADOC_RESULTS_FILENAME);

		File afterDir = getAfterDir(repair);
		File afterCompileOutputs = new File(afterDir, Results.COMPILE_OUTPUTS_FILENAME);
		// File afterCompileErrors = new File(afterDir,
		// Results.COMPILE_ERRORS_FILENAME);
		File afterJavadocErrors = new File(afterDir, Results.JAVADOC_RESULTS_FILENAME);

		if (!beforeCompileOutputs.exists() || !beforeJavadocErrors.exists() || !afterDir.exists() || !afterJavadocErrors.exists()) {
			repair.setStatus(this, Repair.Status.Broken);
			return;
		}

		try {
			List<String> beforeCompileOutputsLines = FileUtils.readLines(beforeCompileOutputs);
			// List<String> beforeCompileErrorsLines =
			// FileUtils.readLines(beforeCompileErrors);
			List<String> beforeJavadocErrorsLines = FileUtils.readLines(beforeJavadocErrors);

			List<String> afterCompileOutputsLines = FileUtils.readLines(afterCompileOutputs);
			// List<String> afterCompileErrorsLines =
			// FileUtils.readLines(afterCompileErrors);
			List<String> afterJavadocErrorsLines = FileUtils.readLines(afterJavadocErrors);

			int beforeNumOfWarnings = getNumOfWarnings(beforeCompileOutputsLines);
			int afterNumOfWarnings = getNumOfWarnings(afterCompileOutputsLines);

			int beforeNumOfJavadocErrors = JavadocUtils.parseJavadocErrorMessages(beforeJavadocErrorsLines).size();
			int afterNumOfJavadocErrors = JavadocUtils.parseJavadocErrorMessages(afterJavadocErrorsLines).size();

			if (beforeNumOfWarnings > afterNumOfWarnings && beforeNumOfJavadocErrors > afterNumOfJavadocErrors) {
				repair.setStatus(this, Repair.Status.Improved);
			} else if (beforeNumOfWarnings >= afterNumOfWarnings && beforeNumOfJavadocErrors > afterNumOfJavadocErrors) {
				repair.setStatus(this, Repair.Status.Improved);
			} else if (beforeNumOfWarnings > afterNumOfWarnings && beforeNumOfJavadocErrors >= afterNumOfJavadocErrors) {
				repair.setStatus(this, Repair.Status.Improved);
			} else if (beforeNumOfWarnings == afterNumOfWarnings && beforeNumOfJavadocErrors == afterNumOfJavadocErrors) {
				repair.setStatus(this, Repair.Status.Stay);
			} else {
				repair.setStatus(this, Repair.Status.Degraded);
			}

			beforeResults.put(repair, new Result(beforeNumOfWarnings, -1, beforeNumOfJavadocErrors));
			afterResults.put(repair, new Result(afterNumOfWarnings, -1, afterNumOfJavadocErrors));
		} catch (IOException e) {
			repair.setStatus(this, Repair.Status.Broken);
			return;
		}
	}

	public static int getNumOfWarnings(List<String> lines) {
		int num = 0;
		for (String line : lines) {
			if (line.startsWith("[WARNING] ")) {
				num++;
			}
			if (line.startsWith("[INFO] ") && line.endsWith(" warnings")) {
				return Integer.parseInt(line.replace("[INFO] ", "").replace(" warnings", ""));
			}
		}
		return num;
	}

	@Override
	public void output(List<Repair> repairs) throws IOException {
		File rootDir = EvaluatorBase.getRepairDir(outputDir, projectId);
		File dir = new File(rootDir, "output");
		if (!repairs.isEmpty() && !dir.exists()) {
			dir.mkdirs();
		}
		// csv
		StringBuilder builder = new StringBuilder();
		// header
		// common
		builder.append(EvaluatorBase.getCommonCsvHeader());
		// specific
		builder.append(",");
		builder.append("Before num of output lines").append(",");
		builder.append("Before num of error lines").append(",");
		builder.append("Before num of javadoc lines").append(",");
		builder.append("After num of output lines").append(",");
		builder.append("After num of error lines").append(",");
		builder.append("After num of javadoc lines");
		// end
		builder.append("\n");
		// content
		for (Repair repair : repairs) {
			// common
			builder.append(repair.toCsv(this)).append(",");
			// specific
			Result before = beforeResults.get(repair);
			Result after = afterResults.get(repair);
			builder.append(before == null ? "" : before.getOutputLineNum()).append(",");
			builder.append(before == null ? "" : before.getErrorLineNum()).append(",");
			builder.append(before == null ? "" : before.getJavadocErrorLineNum()).append(",");
			builder.append(after == null ? "" : after.getOutputLineNum()).append(",");
			builder.append(after == null ? "" : after.getErrorLineNum()).append(",");
			builder.append(after == null ? "" : after.getJavadocErrorLineNum());
			// end
			builder.append("\n");
		}
		// write
		File csv = new File(dir, EvaluatorBase.REPAIR_FILENAME);
		FileUtils.write(csv, builder.toString());
	}

	private static class Result {
		protected int outputs;
		protected int errors;
		protected int javadocs;

		public Result(int outputs, int errors, int javadocs) {
			this.outputs = outputs;
			this.errors = errors;
			this.javadocs = javadocs;
		}

		public int getOutputLineNum() {
			return outputs;
		}

		public int getErrorLineNum() {
			return errors;
		}

		public int getJavadocErrorLineNum() {
			return javadocs;
		}
	}
}

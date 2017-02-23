package jp.mzw.vtr.repair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.validate.ValidatorBase;
import jp.mzw.vtr.validate.coding_style.AccessFilesProperly;
import jp.mzw.vtr.validate.coding_style.AccessStaticFieldsAtDefinedSuperClass;
import jp.mzw.vtr.validate.coding_style.AccessStaticMethodsAtDefinedSuperClass;
import jp.mzw.vtr.validate.coding_style.AddCastToNull;
import jp.mzw.vtr.validate.coding_style.AddExplicitBlocks;
import jp.mzw.vtr.validate.coding_style.ConvertForLoopsToEnhanced;
import jp.mzw.vtr.validate.coding_style.FormatCode;
import jp.mzw.vtr.validate.coding_style.UseArithmeticAssignmentOperators;
import jp.mzw.vtr.validate.coding_style.UseDiamondOperators;
import jp.mzw.vtr.validate.coding_style.UseModifierFinalWherePossible;
import jp.mzw.vtr.validate.coding_style.UseThisIfNecessary;
import jp.mzw.vtr.validate.exception_handling.HandleExpectedExecptionsProperly;
import jp.mzw.vtr.validate.exception_handling.RemoveUnusedExceptions;
import jp.mzw.vtr.validate.junit.AddTestAnnotations;
import jp.mzw.vtr.validate.junit.ModifyAssertImports;
import jp.mzw.vtr.validate.junit.SwapActualExpectedValues;
import jp.mzw.vtr.validate.junit.UseAssertArrayEqualsProperly;
import jp.mzw.vtr.validate.junit.UseAssertEqualsProperly;
import jp.mzw.vtr.validate.junit.UseAssertFalseProperly;
import jp.mzw.vtr.validate.junit.UseAssertNotSameProperly;
import jp.mzw.vtr.validate.junit.UseAssertNullProperly;
import jp.mzw.vtr.validate.junit.UseAssertTrueProperly;
import jp.mzw.vtr.validate.junit.UseFailInsteadOfAssertTrueFalse;
import jp.mzw.vtr.validate.junit.UseStringContains;
import jp.mzw.vtr.validate.outputs.suppress_warnings.AddSerialVersionUids;
import jp.mzw.vtr.validate.outputs.suppress_warnings.add_override_annotation.AddOverrideAnnotationToMethodsInConstructors;
import jp.mzw.vtr.validate.outputs.suppress_warnings.add_override_annotation.AddOverrideAnnotationToTestCase;
import raykernel.apps.readability.eval.Main;

public class Readability extends EvaluatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(Readability.class);

	public Readability(Project project) {
		super(project);
	}
	
	@Override
	public String getName() {
		return "readability";
	}

	@Override
	public List<Class<? extends ValidatorBase>> includeValidators() {
		final List<Class<? extends ValidatorBase>> includes = new ArrayList<>();

		/*
		 * Clean up
		 */
		// Code organizing
		includes.add(FormatCode.class);
		// Code style
		includes.add(ConvertForLoopsToEnhanced.class);
		includes.add(UseModifierFinalWherePossible.class);
		includes.add(AddExplicitBlocks.class);
		includes.add(AddTestAnnotations.class);
		includes.add(ModifyAssertImports.class); // +JUnit
		includes.add(UseAssertArrayEqualsProperly.class); // +JUnit
		includes.add(UseAssertEqualsProperly.class); // +JUnit
		includes.add(UseAssertFalseProperly.class); // +JUnit
		includes.add(UseAssertNotSameProperly.class); // +JUnit
		includes.add(UseAssertNullProperly.class); // +JUnit
		includes.add(UseAssertTrueProperly.class); // +JUnit
		includes.add(UseFailInsteadOfAssertTrueFalse.class); // +JUnit
		includes.add(UseStringContains.class); // +JUnit
		includes.add(SwapActualExpectedValues.class); // +JUnit
		includes.add(HandleExpectedExecptionsProperly.class); // +ExceptionHandling
		includes.add(RemoveUnusedExceptions.class); // +ExceptionHandling
		includes.add(UseArithmeticAssignmentOperators.class);
		includes.add(AccessFilesProperly.class);
		// Missing code
		includes.add(AddSerialVersionUids.class); // +SuppressWarnings
		includes.add(AddOverrideAnnotationToMethodsInConstructors.class); // +SuppressWarnings
		includes.add(AddOverrideAnnotationToTestCase.class); // +SuppressWarnings
		includes.add(AddCastToNull.class);
		// Member access
		includes.add(UseThisIfNecessary.class);
		includes.add(AccessStaticFieldsAtDefinedSuperClass.class);
		includes.add(AccessStaticMethodsAtDefinedSuperClass.class);
		// Unnecessary code
		includes.add(UseDiamondOperators.class);

		return includes;
	}

	@Override
	public void evaluateBefore(Repair repair) {
		if (getFile(repair, Phase.Before, Type.Score).exists() && getFile(repair, Phase.Before, Type.Score).exists()) {
			return;
		}
		// get
		String content = repair.getOriginalPart();
		// measure
		int num = 1;
		double score = 0;
		for (int n = 1; n < content.length(); n++) {
			double sumScore = 0;
			int size = content.length() / n;
			for (int m = 1; m <= n; m++) {
				String subContent = content.substring(size * (m - 1), size * m - 1);
				double subScore = Main.getReadability(subContent);
				sumScore += subScore;
			}
			double aveScore = sumScore / n;
			if (0 < aveScore) {
				num = n;
				score = aveScore;
				break;
			}
			System.gc();
		}
		// put
		try {
			FileUtils.writeStringToFile(getFile(repair, Phase.Before, Type.Score), Double.toString(score));
			FileUtils.writeStringToFile(getFile(repair, Phase.Before, Type.SplitNum), Integer.toString(num));
		} catch (IOException e) {
			LOGGER.warn("Failed to store score/split-num into file system: {}", e.getMessage());
		}
	}

	@Override
	public void evaluateAfter(Repair repair) {
		try {
			File dstPatchFile = measure(repair);
			if (dstPatchFile == null) {
				return;
			}
			// start
			// get
			String content = repair.getRevisedPart();
			// measure
			int num = 1;
			double score = 0;
			for (int n = 1; n < content.length(); n++) {
				double sumScore = 0;
				int size = content.length() / n;
				for (int m = 1; m <= n; m++) {
					String subContent = content.substring(size * (m - 1), size * m - 1);
					double subScore = Main.getReadability(subContent);
					sumScore += subScore;
				}
				double aveScore = sumScore / n;
				if (0 < aveScore) {
					num = n;
					score = aveScore;
					break;
				}
			}
			// put
			FileUtils.writeStringToFile(getFile(repair, Phase.After, Type.Score), Double.toString(score));
			FileUtils.writeStringToFile(getFile(repair, Phase.After, Type.SplitNum), Integer.toString(num));
			// end
			FileUtils.copyFile(repair.getPatchFile(), dstPatchFile);
		} catch (IOException e) {
			LOGGER.warn("Failed to evaluate after: {} at {} with {}", repair.getTestCaseFullName(), repair.getCommit().getId(), this.getClass().getName());
		}
	}

	@Override
	public void compare(Repair repair) {
		try {
			Result before = Result.parse(this, repair, Phase.Before);
			Result after = Result.parse(this, repair, Phase.After);
			if (before.num < after.num) {
				repair.setStatus(this, Repair.Status.Degraded);
			} else if (before.num > after.num) {
				repair.setStatus(this, Repair.Status.Improved);
			} else {
				if (before.score < after.score) {
					repair.setStatus(this, Repair.Status.Improved);
				} else if (before.score > after.score) {
					repair.setStatus(this, Repair.Status.Degraded);
				} else {
					repair.setStatus(this, Repair.Status.Stay);
				}
			}
		} catch (NumberFormatException | IOException e) {
			repair.setStatus(this, Repair.Status.Broken);
		}
	}

	@Override
	public void output(List<Repair> repairs) throws IOException {
		File rootDir = EvaluatorBase.getRepairDir(outputDir, projectId);
		File dir = new File(rootDir, "readability");
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
		builder.append("Before score").append(",");
		builder.append("Before split num").append(",");
		builder.append("After score").append(",");
		builder.append("After split num");
		// end
		builder.append("\n");
		// content
		for (Repair repair : repairs) {
			if (!RepairEvaluator.include(this, repair)) {
				continue;
			}
			// common
			builder.append(repair.toCsv(this)).append(",");
			// specific
			Result before = Result.parse(this, repair, Phase.Before);
			builder.append(before == null ? "" : before.getScore()).append(",");
			builder.append(before == null ? "" : before.getSplitNum()).append(",");
			if (repair.getStatus(this).equals(Repair.Status.Broken)) {
				builder.append(-1).append(",");
				builder.append(-1);
			} else {
				Result after = Result.parse(this, repair, Phase.After);
				builder.append(after == null ? "" : after.getScore()).append(",");
				builder.append(after == null ? "" : after.getSplitNum());
			}
			// end
			builder.append("\n");
		}
		// write
		File csv = new File(dir, EvaluatorBase.REPAIR_FILENAME);
		FileUtils.write(csv, builder.toString());
	}

	protected static class Result {
		protected double score;
		protected int num;

		public Result(double score, int num) {
			this.score = score;
			this.num = num;
		}

		public double getScore() {
			return score;
		}

		public int getSplitNum() {
			return num;
		}

		public static Result parse(Readability readability, Repair repair, Phase phase) throws NumberFormatException, IOException {
			double score = Double.parseDouble(FileUtils.readFileToString(readability.getFile(repair, phase, Type.Score)));
			int num = Integer.parseInt(FileUtils.readFileToString(readability.getFile(repair, phase, Type.SplitNum)));
			return new Result(score, num);
		}
	}
}

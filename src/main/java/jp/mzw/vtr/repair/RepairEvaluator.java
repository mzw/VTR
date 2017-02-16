package jp.mzw.vtr.repair;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import difflib.PatchFailedException;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.validate.ValidatorBase;

public class RepairEvaluator {
	protected static Logger LOGGER = LoggerFactory.getLogger(RepairEvaluator.class);

	protected String projectId;
	protected File projectDir;
	protected File outputDir;
	protected File mavenHome;
	protected boolean mavenOutput;

	protected List<Repair> repairs;

	public RepairEvaluator(Project project) {
		this.projectId = project.getProjectId();
		this.projectDir = project.getProjectDir();
		this.outputDir = project.getOutputDir();
		this.mavenHome = project.getMavenHome();
		this.mavenOutput = project.getMavenOutput();
		this.repairs = new ArrayList<>();
	}

	public RepairEvaluator parse() throws IOException {
		File projectDir = new File(this.outputDir, this.projectId);
		File validateDir = new File(projectDir, ValidatorBase.VALIDATOR_DIRNAME);
		if (!validateDir.exists()) {
			LOGGER.info("Run Validator/PatchGenerator in advance");
			return this;
		}
		for (File commitDir : validateDir.listFiles()) {
			if (commitDir.isFile()) {
				continue;
			}
			if ("results".equals(commitDir.getName())) {
				continue;
			}
			String commitId = commitDir.getName();
			for (File validatorDir : commitDir.listFiles()) {
				String validatorName = validatorDir.getName();
				for (File patch : validatorDir.listFiles()) {
					if (!patch.getName().endsWith(".patch")) {
						continue;
					}
					String[] name = patch.getName().replace(".patch", "").split("#");
					String clazz = name[0];
					String method = name[1];

					Repair repair = new Repair(new Commit(commitId, null), validatorName, patch);
					repair.setTestCaseNames(clazz, method);
					repairs.add(repair);
				}
			}
		}
		return this;
	}

	public void evaluate(List<EvaluatorBase> evaluators) throws GitAPIException, IOException, PatchFailedException, ParseException {
		if (repairs.isEmpty()) {
			parse();
		}
		CheckoutConductor cc = new CheckoutConductor(projectId, projectDir, outputDir);
		String curCommitId = null;
		for (Repair repair : repairs) {
			Commit commit = repair.getCommit();
			if (curCommitId == null) {
				cc.checkout(commit);
				curCommitId = commit.getId();
			} else {
				if (!curCommitId.equals(commit.getId())) {
					cc.checkout(commit);
					curCommitId = commit.getId();
				}
			}
			repair.parse(projectDir);
			for (EvaluatorBase each : evaluators) {
				if (!include(each, repair)) {
					continue;
				}
				each.evaluateBefore(repair);
			}
			repair.apply(projectDir);
			for (EvaluatorBase each : evaluators) {
				if (!include(each, repair)) {
					continue;
				}
				each.evaluateAfter(repair);
			}
			for (EvaluatorBase each : evaluators) {
				if (!include(each, repair)) {
					continue;
				}
				each.compare(repair);
			}
			repair.revert();
		}
		// output
		for (EvaluatorBase each : evaluators) {
			each.output(repairs);
		}
	}

	private static boolean include(EvaluatorBase evaluator, Repair repair) {
		for (Class<? extends ValidatorBase> validator : evaluator.includeValidators()) {
			String validatorName = validator.getName();
			if (validatorName.equals(repair.getValidatorName())) {
				return true;
			}
		}
		return false;
	}
}

package jp.mzw.vtr.repair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
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
	
	public File getProjectDir() {
		return projectDir;
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

	public List<Repair> getRepairs() {
		return this.repairs;
	}

}

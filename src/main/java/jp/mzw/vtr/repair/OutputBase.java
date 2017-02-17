package jp.mzw.vtr.repair;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;

abstract public class OutputBase extends EvaluatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(OutputBase.class);

	public OutputBase(Project project) {
		super(project);
	}

	protected File getAfterDir(Repair repair) {
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
}

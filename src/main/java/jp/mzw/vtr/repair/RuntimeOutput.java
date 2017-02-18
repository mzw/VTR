package jp.mzw.vtr.repair;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.*;
import jp.mzw.vtr.validate.ValidatorBase;
import jp.mzw.vtr.validate.outputs.RemovePrintStatements;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by TK on 2017/02/17.
 */
public class RuntimeOutput extends OutputBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(RuntimeOutput.class);
    private Map<Repair, Result> beforeResults;
    private Map<Repair, Result> afterResults;
    public RuntimeOutput(Project project) {
        super(project);
        beforeResults = new HashMap<>();
        afterResults  = new HashMap<>();
    }

    @Override
    public void evaluateBefore(Repair repair) {
        // mavenでテスト実行
        try {
            int compile = MavenUtils.maven(this.projectDir, Arrays.asList("compile", "test-compile"), mavenHome, mavenOutput);
            if (compile != 0) {
                LOGGER.warn("Failed to compile before: {} at {}", repair.getTestCaseFullName(), repair.getCommit().getId());
                return;
            }
            // change po.xml
            JacocoInstrumenter ji = new JacocoInstrumenter(this.projectDir);
            boolean modified = ji.instrument();
            // execute test casea
            Results results = run(repair.getTestCaseFullName());
            results.patchBeforeRuntimeOutput(getAfterDir(repair));

            if (modified) {
                ji.revert();
            }
        } catch (IOException | DocumentException | MavenInvocationException e) {
            LOGGER.warn("Failed to evaluate before: {} at {} with {}", repair.getTestCaseFullName(), repair.getCommit().getId(), this.getClass().getName());
        }
    }

    @Override
    public List<Class<? extends ValidatorBase>> includeValidators() {
        final List<Class<? extends ValidatorBase>> includes = new ArrayList<>();
		/*
		 * Sharp-shooting outputs
		 */
        // Prints for debugging
        includes.add(RemovePrintStatements.class);
        return includes;
    }
    @Override
    public void evaluateAfter(Repair repair) {
        try {
            int compile = MavenUtils.maven(this.projectDir, Arrays.asList("compile", "test-compile"), mavenHome, mavenOutput);
            if (compile != 0) {
                LOGGER.warn("Failed to compile before: {} at {}", repair.getTestCaseFullName(), repair.getCommit().getId());
                return;
            }
            JacocoInstrumenter ji = new JacocoInstrumenter(this.projectDir);
            boolean modified = ji.instrument();
            Results results = run(repair.getTestCaseFullName());
            results.patchAfterRuntimeOutput(getAfterDir(repair));
            if (modified) {
                ji.revert();
            }
        } catch (IOException | DocumentException | MavenInvocationException e) {
            LOGGER.warn("Failed to evaluate before: {} at {} with {}", repair.getTestCaseFullName(), repair.getCommit().getId(), this.getClass().getName());
        }
    }

    @Override
    public void compare(Repair repair) {
        File resultsDir = getAfterDir(repair);
        // before
        File beforeRuntimeOutputs = new File(resultsDir, Results.PATCH_BEFORE_RUNTIME_OUTPUTS_FILENAME);
        // after
        File afterRuntimeOutputs = new File(resultsDir, Results.PATCH_AFTER_RUNTIME_OUTPUTS_FILENAME);
        if (!beforeRuntimeOutputs.exists() || !afterRuntimeOutputs.exists()) {
            repair.setStatus(this, Repair.Status.Broken);
            return;
        }
        try {
            List<String> beforeRuntimeOutputsLines = FileUtils.readLines(beforeRuntimeOutputs);
            List<String> afterRuntimeOutputsLines = FileUtils.readLines(afterRuntimeOutputs);
            int beforeNumOfRuntimeOutputs = getNumOfRuntimeOutputs(beforeRuntimeOutputsLines, repair);
            int afterNumOfRuntimeOutputs  = getNumOfRuntimeOutputs(afterRuntimeOutputsLines, repair);
            if (beforeNumOfRuntimeOutputs > afterNumOfRuntimeOutputs) {
                repair.setStatus(this, Repair.Status.Improved);
            } else if (beforeNumOfRuntimeOutputs == afterNumOfRuntimeOutputs) {
                repair.setStatus(this, Repair.Status.Stay);
            } else {
                repair.setStatus(this, Repair.Status.Degraded);
            }
            beforeResults.put(repair, new Result(beforeNumOfRuntimeOutputs));
            afterResults.put(repair, new Result(afterNumOfRuntimeOutputs));
        } catch (IOException e) {
            repair.setStatus(this, Repair.Status.Broken);
            return;
        }
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
        builder.append("Before num of runtime output lines").append(",");
        builder.append("After num of runtime output lines");
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
            Result before = beforeResults.get(repair);
            Result after = afterResults.get(repair);
            builder.append(before == null ? "" : before.getOutputLineNum()).append(",");
            builder.append(after == null ? "" : after.getOutputLineNum());
            // end
            builder.append("\n");
        }
        // write
        File csv = new File(dir, "runtime.csv");
        FileUtils.write(csv, builder.toString());
    }

    protected Results run(String testCaseFullName) throws MavenInvocationException {
        LOGGER.info("Measure coverage: {}", testCaseFullName);
        String each = "-Dtest=" + testCaseFullName;
        List<String> args = Arrays.asList(each, "test");
        return MavenUtils.maven(this.projectDir, args, this.mavenHome);
    }

    public static int getNumOfRuntimeOutputs(List<String> lines, Repair repair) {
        int ret = 0;
        boolean runtimeOutputs = false;
        for (String line : lines) {
            if (line.startsWith("Tests run: ")) {
                return ret;
            }
            if (runtimeOutputs) {
                ret++;
            }
            if (line.startsWith("Running " + repair.getTestCaseClassName())) {
                runtimeOutputs = true;
            }
        }
        return -1;
    }

    private static class Result {
        protected int outputs;
        public Result(int outputs) {
            this.outputs = outputs;
        }
        public int getOutputLineNum() {
            return outputs;
        }
    }
}

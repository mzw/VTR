package jp.mzw.vtr.repair;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.CompilerPlugin;
import jp.mzw.vtr.maven.JavadocUtils;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.validate.ValidatorBase;
import jp.mzw.vtr.validate.junit.AssertNotNullToInstances;
import jp.mzw.vtr.validate.outputs.suppress_warnings.AddSerialVersionUids;
import jp.mzw.vtr.validate.outputs.suppress_warnings.DeleteUnnecessaryAssignmenedVariables;
import jp.mzw.vtr.validate.outputs.suppress_warnings.IntroduceAutoBoxing;
import jp.mzw.vtr.validate.outputs.suppress_warnings.RemoveUnnecessaryCasts;
import jp.mzw.vtr.validate.outputs.suppress_warnings.add_override_annotation.AddOverrideAnnotationToMethodsInConstructors;
import jp.mzw.vtr.validate.outputs.suppress_warnings.add_override_annotation.AddOverrideAnnotationToTestCase;
import jp.mzw.vtr.validate.outputs.suppress_warnings.add_suppress_warnings_annotation.AddSuppressWarningsDeprecationAnnotation;
import jp.mzw.vtr.validate.outputs.suppress_warnings.add_suppress_warnings_annotation.AddSuppressWarningsRawtypesAnnotation;
import jp.mzw.vtr.validate.outputs.suppress_warnings.add_suppress_warnings_annotation.AddSuppressWarningsUncheckedAnnotation;
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
public class CompileOutput extends OutputBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(CompileOutput.class);

    protected Map<Repair, Result> beforeResults;
    protected Map<Repair, Result> afterResults;

    public CompileOutput(Project project) {
        super(project);
        beforeResults = new HashMap<>();
        afterResults  = new HashMap<>();
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
		 * Mutation analysis
		 */
        // JUnit
        includes.add(AssertNotNullToInstances.class); // +SuppressWarnings
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
		/*
		 * Clean up
		 */
        // Missing code
        includes.add(AddSerialVersionUids.class); // +SuppressWarnings
        includes.add(AddOverrideAnnotationToMethodsInConstructors.class); // +SuppressWarnings
        includes.add(AddOverrideAnnotationToTestCase.class); // +SuppressWarnings
        return includes;
    }

    @Override
    public void evaluateAfter(Repair repair) {
        try {
            MavenUtils.maven(projectDir, Arrays.asList("clean"), mavenHome);
            CompilerPlugin cp = new CompilerPlugin(projectDir);
            boolean modified = cp.instrument();
            jp.mzw.vtr.maven.Results results = MavenUtils.maven(projectDir, Arrays.asList("test-compile"), mavenHome);
            results.compileOutput(getAfterDir(repair));
            if (modified) {
                cp.revert();
            }
        } catch (MavenInvocationException | IOException | DocumentException e) {
            LOGGER.warn("Failed to get/store outputs: {} at {} with {}", repair.getTestCaseFullName(), repair.getCommit().getId(), this.getClass().getName());
        }
    }

    @Override
    public void compare(Repair repair) {
        // before
        File beforeDir = Results.getDir(outputDir, projectId, repair.getCommit());
        File beforeCompileOutputs = new File(beforeDir, Results.COMPILE_OUTPUTS_FILENAME);
        // after
        File afterDir = getAfterDir(repair);
        File afterCompileOutputs = new File(afterDir, Results.COMPILE_OUTPUTS_FILENAME);
        if (!beforeCompileOutputs.exists() || !afterDir.exists()) {
            repair.setStatus(this, Repair.Status.Broken);
            return;
        }
        try {
            List<String> beforeCompileOutputsLines = FileUtils.readLines(beforeCompileOutputs);
            List<String> afterCompileOutputsLines = FileUtils.readLines(afterCompileOutputs);
            int beforeNumOfWarnings = getNumOfWarnings(beforeCompileOutputsLines);
            int afterNumOfWarnings = getNumOfWarnings(afterCompileOutputsLines);
            if (beforeNumOfWarnings > afterNumOfWarnings) {
                repair.setStatus(this, Repair.Status.Improved);
            } else if (beforeNumOfWarnings == afterNumOfWarnings) {
                repair.setStatus(this, Repair.Status.Stay);
            } else {
                repair.setStatus(this, Repair.Status.Degraded);
            }
            beforeResults.put(repair, new Result(beforeNumOfWarnings, -1));
            afterResults.put(repair, new Result(afterNumOfWarnings, -1));
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
        builder.append("Before num of compile output lines").append(",");
        builder.append("Before num of compile error lines").append(",");
        builder.append("After num of compile output lines").append(",");
        builder.append("After num of compile error lines");
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
            builder.append(after == null ? "" : after.getOutputLineNum()).append(",");
            builder.append(after == null ? "" : after.getErrorLineNum()).append(",");
            // end
            builder.append("\n");
        }
        // write
        File csv = new File(dir, "compile.csv");
        FileUtils.write(csv, builder.toString());
    }

    private static class Result{
        protected int outputs;
        protected int errors;
        public Result(int outputs, int errors) {
            this.outputs = outputs;
            this.errors = errors;
        }
        public int getOutputLineNum() {
            return outputs;
        }
        public int getErrorLineNum() {
            return errors;
        }
    }
}

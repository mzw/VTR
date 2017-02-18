package jp.mzw.vtr.repair;


import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.CompilerPlugin;
import jp.mzw.vtr.maven.JavadocUtils;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.Results;
import jp.mzw.vtr.validate.ValidatorBase;
import jp.mzw.vtr.validate.javadoc.FixJavadocErrors;
import jp.mzw.vtr.validate.javadoc.ReplaceAtTodoWithTODO;
import jp.mzw.vtr.validate.javadoc.UseCodeAnnotationsAtJavaDoc;
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
public class JavadocOutput extends OutputBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(JavadocOutput.class);
    private Map<Repair, Result> beforeResults;
    private Map<Repair, Result> afterResults;
    public JavadocOutput(Project project) {
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

    @Override
    public void compare(Repair repair) {
        // before
        File beforeDir = Results.getDir(outputDir, projectId, repair.getCommit());
        File beforeJavadocErrors = new File(beforeDir, Results.JAVADOC_RESULTS_FILENAME);
        // after
        File afterDir = getAfterDir(repair);
        File afterJavadocErrors = new File(afterDir, Results.JAVADOC_RESULTS_FILENAME);
        if (!beforeJavadocErrors.exists() || !afterDir.exists() || !afterJavadocErrors.exists()) {
            repair.setStatus(this, Repair.Status.Broken);
            return;
        }
        try {
            List<String> beforeJavadocErrorsLines = FileUtils.readLines(beforeJavadocErrors);
            List<String> afterJavadocErrorsLines = FileUtils.readLines(afterJavadocErrors);
            int beforeNumOfJavadocErrors = JavadocUtils.parseJavadocErrorMessages(beforeJavadocErrorsLines).size();
            int afterNumOfJavadocErrors = JavadocUtils.parseJavadocErrorMessages(afterJavadocErrorsLines).size();
            if (beforeNumOfJavadocErrors > afterNumOfJavadocErrors) {
                repair.setStatus(this, Repair.Status.Improved);
            } else if (beforeNumOfJavadocErrors == afterNumOfJavadocErrors) {
                repair.setStatus(this, Repair.Status.Stay);
            } else {
                repair.setStatus(this, Repair.Status.Degraded);
            }
            beforeResults.put(repair, new Result(beforeNumOfJavadocErrors));
            afterResults.put(repair, new Result(afterNumOfJavadocErrors));
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
        builder.append("Before num of javadoc lines").append(",");
        builder.append("After num of javadoc lines");
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
            builder.append(before == null ? "" : before.getJavadocErrorLineNum()).append(",");
            builder.append(after == null ? "" : after.getJavadocErrorLineNum());
            // end
            builder.append("\n");
        }
        // write
        File csv = new File(dir, "javadoc.csv");
        FileUtils.write(csv, builder.toString());
    }

    private static class Result {
        protected int javadocs;
        public Result(int javadocs) {
            this.javadocs = javadocs;
        }
        public int getJavadocErrorLineNum() {
            return javadocs;
        }
    }
}

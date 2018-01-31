package jp.mzw.vtr.cluster.gumtreediff;

import jp.mzw.vtr.CLI;
import jp.mzw.vtr.cluster.BeforeAfterComparer;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.DetectionResult;
import jp.mzw.vtr.detect.Detector;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public class GumTreeDiff extends BeforeAfterComparer {

    public static void main(String[] args) throws IOException, GitAPIException, ParseException {
        Project project = new Project(null).setConfig(CLI.CONFIG_FILENAME);
        List<DetectionResult> results = Detector.getDetectionResults(project.getSubjectsDir(), project.getOutputDir());
        GumTreeDiff differ = new GumTreeDiff(project.getSubjectsDir(), project.getOutputDir());
        differ.run(results);
    }


    public GumTreeDiff(final File projectDir, final File outputDir) {
        super(projectDir, outputDir);
    }

    @Override
    public void prepare(final Project project) {

    }

    @Override
    public void before(final Project project, final String commitId) {

    }

    @Override
    public void after(final Project project, final String commitId) {

    }

    @Override
    public void compare(final Project project, final String prvCommitId, final String curCommitId, final String className, final String methodName) {

    }

    public enum Type {
        Additive,
        Subtractive,
        Altering,
        None
    }
}

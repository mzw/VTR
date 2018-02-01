package jp.mzw.vtr.cluster.gumtreediff;

import com.github.gumtreediff.actions.model.Action;
import jp.mzw.vtr.CLI;
import jp.mzw.vtr.cluster.BeforeAfterComparator;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.detect.DetectionResult;
import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GumTreeDiff extends BeforeAfterComparator {
    private static Logger LOGGER = LoggerFactory.getLogger(GumTreeDiff.class);
    private static final String GUMTREE_DIR = "gumtree";

    private Map<String, TestSuite> prevTestSuites;
    private Map<String, TestSuite> curTestSuites;

    // StringBuilders to contain results
    private StringBuilder additiveSb;
    private StringBuilder subtractiveSb;
    private StringBuilder alteringSb;
    private StringBuilder noneSb;


    public static void main(String[] args) throws IOException, GitAPIException, ParseException {
        Project project = new Project(null).setConfig(CLI.CONFIG_FILENAME);
        List<DetectionResult> results = Detector.getDetectionResults(project.getSubjectsDir(), project.getOutputDir());
        GumTreeDiff differ = new GumTreeDiff(project.getSubjectsDir(), project.getOutputDir());
        differ.run(results);
    }

    public GumTreeDiff(final File projectDir, final File outputDir) {
        super(projectDir, outputDir);
    }

    private enum Type {
        Additive,
        Subtractive,
        Altering,
        None
    }

    @Override
    public void prepare(final Project project) {
        additiveSb    = new StringBuilder();
        subtractiveSb = new StringBuilder();
        alteringSb    = new StringBuilder();
        noneSb        = new StringBuilder();
    }

    @Override
    public void before(final Project project, final String commitId) {
        prevTestSuites = getTestSuites(project);
    }

    @Override
    public void after(final Project project, final String commitId) {
        curTestSuites = getTestSuites(project);
    }


    @Override
    public void compare(final Project project, final String prvCommit, final String curCommit, final String className, final String methodName) {
        Type type = _compare(project, prvCommit, curCommit, className, methodName);
        if (type.equals(Type.Additive)) {
            VtrUtils.addCsvRecords(additiveSb, project.getProjectId(), prvCommit, curCommit, className, methodName);
        } else if (type.equals(Type.Subtractive)) {
            VtrUtils.addCsvRecords(subtractiveSb, project.getProjectId(), prvCommit, curCommit, className, methodName);
        } else if (type.equals(Type.Altering)) {
            VtrUtils.addCsvRecords(alteringSb, project.getProjectId(), prvCommit, curCommit, className, methodName);
        } else if (type.equals(Type.None)) {
            VtrUtils.addCsvRecords(noneSb, project.getProjectId(), prvCommit, curCommit, className, methodName);
        }
    }

    @Override
    public void output() {
        outputAddictive(additiveSb.toString());
        outputSubtractive(subtractiveSb.toString());
        outputAltering(alteringSb.toString());
        outputNone(noneSb.toString());
    }

    private Type _compare(final Project project, final String prvCommitId, final String curCommitId, final String className, final String methodName) {
        TestSuite curTestSuite  = curTestSuites.get(className);
        TestSuite prevTestSuite = prevTestSuites.get(className);
        if (curTestSuite == null && prevTestSuite == null) {
            LOGGER.error("Why both test-suites are null?");
            return Type.None;
        } else if (curTestSuite != null && prevTestSuite == null) {
            LOGGER.info("{} is not null at {} and null at {}", className, curCommitId, prvCommitId);
            return Type.Additive;
        } else if (curTestSuite == null) { // (prevTestSuite != null) is always true.
            LOGGER.info("{} is null at {} and not null at {}", className, curCommitId, prvCommitId);
            return Type.Subtractive;
        }
        TestCase curTestCase = listToMapTestCases(curTestSuite.getTestCases()).get(methodName);
        TestCase prevTestCase = listToMapTestCases(prevTestSuite.getTestCases()).get(methodName);
        if (curTestCase == null && prevTestCase == null) {
            LOGGER.error("Why both test-cases are null?");
            return Type.None;
        } else if (curTestCase != null && prevTestCase == null) {
            LOGGER.info("{} is not null at {} and null at {}", className + ":" + methodName, curCommitId, prvCommitId);
            return Type.Additive;
        } else if (curTestCase == null) { // (prevTestCase != null) is always true.
            LOGGER.info("{} is null at {} and not null at {}", className + ":" + methodName, curCommitId, prvCommitId);
            return Type.Subtractive;
        }
        GumTreeEngine engine = new GumTreeEngine();
        List<Action> actions = engine.getEditActions(prevTestCase, curTestCase);
        outputEditActions(project, curCommitId, prvCommitId, className, methodName, actions);
        return _compare(actions);
    }

    private Type _compare(List<Action> actions) {
        // 何を比較しよう？
        return Type.None;
    }

    private Map<String, TestSuite> getTestSuites(Project project) {
        try {
            return listToMapTestSuites(MavenUtils.getTestSuitesAtLevel2(project.getProjectDir()));
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    private Map<String, TestSuite> listToMapTestSuites(List<TestSuite> testSuites) {
        Map<String, TestSuite> ret = testSuites.stream()
                .collect(Collectors.toMap(
                        s -> s.getTestClassName(),
                        s -> s
                ));
        return ret;
    }
    private Map<String, TestCase> listToMapTestCases(List<TestCase> testCases) {
        Map<String, TestCase> ret = testCases.stream()
                .collect(Collectors.toMap(
                        s -> s.getName(),
                        s -> s
                ));
        return ret;
    }



    /* To output edit actions */
    private void outputEditActions(Project project, String curCommitId, String prvCommitId, String className, String methodName, List<Action> actions) {
        StringBuilder sb = new StringBuilder();
        sb.append("previous commit : ").append(curCommitId).append("\n");
        sb.append("current commit : ").append(prvCommitId).append("\n");
        for (Action action : actions) {
            sb.append(action.toString()).append("\n");
        }
        outputEditActions(project, curCommitId, className, methodName, sb.toString());
    }

    private void outputEditActions(Project project, String curCommitId, String className, String methodName, String content) {
        VtrUtils.writeContent(getPathToOutputEditActions(project, curCommitId, className, methodName), content);
    }

    /* To output results */
    private void outputAddictive(String content) {
        VtrUtils.writeContent(getPathToOutputAdditive(), content);
    }
    private void outputSubtractive(String content) {
        VtrUtils.writeContent(getPathToOutputSubtractive(), content);
    }
    private void outputAltering(String content) {
        VtrUtils.writeContent(getPathToOutputAltering(), content);
    }
    private void outputNone(String content) {
        VtrUtils.writeContent(getPathToOutputNone(), content);
    }
    /* To get path to output */
    private Path getPathToOutputAdditive() {
        return getPathToOutputFile(Type.Additive);
    }
    private Path getPathToOutputSubtractive() {
        return getPathToOutputFile(Type.Subtractive);
    }
    private Path getPathToOutputAltering() {
        return getPathToOutputFile(Type.Altering);
    }
    private Path getPathToOutputNone() {
        return getPathToOutputFile(Type.None);
    }
    private Path getPathToOutputFile(Type pattern) {
        String filename = "";
        if (pattern.equals(Type.Additive)) {
            filename = "additive";
        } else if (pattern.equals(Type.Subtractive)){
            filename = "subtractive";
        } else if (pattern.equals(Type.Altering)) {
            filename = "altering";
        } else if (pattern.equals(Type.None)) {
            filename = "none";
        }
        String className = this.getClass().toString();
        className = className.substring(className.lastIndexOf(".") + 1);
        return VtrUtils.getPathToFile(outputDir.getPath(), className, filename + ".csv");
    }
    private Path getPathToOutputEditActions(Project project, String commitId, String className, String methodName) {
        return VtrUtils.getPathToFile(project.getOutputDir().toString(), project.getProjectId(), GUMTREE_DIR, commitId, className, methodName, "actions.txt");
    }
}

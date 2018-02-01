package jp.mzw.vtr.cluster.gumtreediff;

import com.github.gumtreediff.actions.model.Action;
import jp.mzw.vtr.CLI;
import jp.mzw.vtr.cluster.BeforeAfterComparator;
import jp.mzw.vtr.core.Project;
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
        // do nothing
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
    public void compare(final Project project, final String prvCommitId, final String curCommitId, final String className, final String methodName) {
        // StringBuilders to contain results
        StringBuilder additiveSb    = new StringBuilder();
        StringBuilder subtractiveSb = new StringBuilder();
        StringBuilder alteringSb    = new StringBuilder();
        StringBuilder noneSb        = new StringBuilder();
        Type type = _compare(project, prvCommitId, curCommitId, className, methodName);
        if (type.equals(Type.Additive)) {
            generateContent(additiveSb, project.getProjectId(), curCommitId, prvCommitId, className, methodName);
        } else if (type.equals(Type.Subtractive)) {
            generateContent(subtractiveSb, project.getProjectId(), curCommitId, prvCommitId, className, methodName);
        } else if (type.equals(Type.Altering)) {
            generateContent(alteringSb, project.getProjectId(), curCommitId, prvCommitId, className, methodName);
        } else if (type.equals(Type.None)) {
            generateContent(noneSb, project.getProjectId(), curCommitId, prvCommitId, className, methodName);
        }
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
        outputEditActions(project, actions);
        return _compare(actions);
    }

    private Type _compare(List<Action> actions) {
        // 何を比較しよう？
        return Type.None;
    }

    private Map<String, TestSuite> getTestSuites(Project project) {
        try {
            return listToMapTestSuites(MavenUtils.getTestSuitesAtLevel2(project.getSubjectsDir()));
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

    public enum Type {
        Additive,
        Subtractive,
        Altering,
        None
    }

    /* To output edit actions */
    private void outputEditActions(Project project, List<Action> actions) {
        StringBuilder sb = new StringBuilder();
        for (Action action : actions) {
            sb.append(action.toString()).append("\n");
        }
        outputEditActions(project, sb.toString());
    }

    private void outputEditActions(Project project, String content) {
        if (!Files.exists(getPathToOutputEditActions(project))) {
            try {
                Files.createDirectories(getPathToOutputEditActionsDir(project));
                Files.createFile(getPathToOutputEditActions(project));
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(getPathToOutputEditActions(project))) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }
    /* To output results */
    private void outputAddictive(String content) {
        if (!Files.exists(getPathToOutputAdditive())) {
            try {
                Files.createDirectories(getPathToOutputDir());
                Files.createFile(getPathToOutputAdditive());
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(getPathToOutputAdditive())) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }
    private void outputSubtractive(String content) {
        if (!Files.exists(getPathToOutputSubtractive())) {
            try {
                Files.createDirectories(getPathToOutputDir());
                Files.createFile(getPathToOutputSubtractive());
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(getPathToOutputSubtractive())) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }
    private void outputAltering(String content) {
        if (!Files.exists(getPathToOutputAltering())) {
            try {
                Files.createDirectories(getPathToOutputDir());
                Files.createFile(getPathToOutputAltering());
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(getPathToOutputAltering())) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }
    private void outputNone(String content) {
        if (!Files.exists(getPathToOutputNone())) {
            try {
                Files.createDirectories(getPathToOutputDir());
                Files.createFile(getPathToOutputNone());
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(getPathToOutputNone())) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }
    /* To generate content */
    private void generateContent(StringBuilder sb, String projectId, String commit, String prevCommit, String className, String methodName) {
        sb.append(projectId).append(",");
        sb.append(commit).append(",");
        sb.append(prevCommit).append(",");
        sb.append(className).append(",");
        sb.append(methodName).append("\n");
    }

    /* To get path to output */
    private Path getPathToOutputEditActions(Project project) {
        return Paths.get(String.join("/", getPathToOutputEditActionsDir(project).toString(), "actions.txt"));
    }
    private Path getPathToOutputEditActionsDir(Project project) {
        return Paths.get(String.join("/", project.getOutputDir().toString(), project.getProjectId(), GUMTREE_DIR));
    }
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
        return Paths.get(String.join("/", getPathToOutputDir().toString(), filename + ".csv"));
    }
    private Path getPathToOutputDir() {
        return Paths.get(String.join("/", outputDir.getPath(), GUMTREE_DIR));
    }

}

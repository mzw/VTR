package jp.mzw.vtr.cluster.gumtreediff;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import jp.mzw.vtr.CLI;
import jp.mzw.vtr.cluster.BeforeAfterComparator;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.detect.DetectionResult;
import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;
import org.apache.commons.io.Charsets;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class GumTreeDiff extends BeforeAfterComparator {
    private static Logger LOGGER = LoggerFactory.getLogger(GumTreeDiff.class);
    private static final String GUMTREE_DIR = "gumtree";

    private Map<String, TestSuite> prevTestSuites;
    private Map<String, TestSuite> curTestSuites;

    // StringBuilders to contain results
    Map<String, StringBuilder> stringBuilderMap;
    
    private List<TestSuite> prev;
    private List<TestSuite> curr;

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
        INS_MOV_DEL_UPD,
        INS_MOV_DEL,
        INS_MOV_UPD,
        INS_DEL_UPD,
        MOV_DEL_UPD,
        INS_MOV,
        INS_DEL,
        INS_UPD,
        MOV_DEL,
        MOV_UPD,
        DEL_UPD,
        INS,
        MOV,
        DEL,
        UPD,
        None
    }

    @Override
    public void prepare(final Project project) {
        stringBuilderMap = new HashMap<>();
        for (Type type : Type.values()) {
            stringBuilderMap.put(type.toString(), new StringBuilder());
        }
    }

    @Override
    public void before(final Project project, final String commitId) {
        prevTestSuites = getTestSuites(project);
        try {
            prev = MavenUtils.getTestSuites(project.getProjectDir());
        } catch(Exception e) {

        }
    }

    @Override
    public void after(final Project project, final String commitId) {
        curTestSuites = getTestSuites(project);
        try {
            curr = MavenUtils.getTestSuites(project.getProjectDir());
        } catch(Exception e) {

        }
    }


    @Override
    public void compare(final Project project, final String prvCommit, final String curCommit, final String className, final String methodName) {
        Type type = _compareFromExistingEditScripts(project, curCommit, className, methodName);
//        Type type = _compare(project, prvCommit, curCommit, className, methodName);

        StringBuilder sb = stringBuilderMap.get(type.toString());
        VtrUtils.addCsvRecords(sb, project.getProjectId(), prvCommit, curCommit, className, methodName);
    }

    @Override
    public void output() {
        for (Type type : Type.values()) {
            outputGumTreeDiff(type);
        }
    }

    private Type _compare(final Project project, final String prvCommitId, final String curCommitId, final String className, final String methodName) {
        TestSuite curTestSuite  = curTestSuites.get(className);
        TestSuite prevTestSuite = prevTestSuites.get(className);
        if (curTestSuite == null && prevTestSuite == null) {
            LOGGER.error("Why both test-suites are null?");
            return Type.None;
        } else if (curTestSuite != null && prevTestSuite == null) {
            LOGGER.info("{} is not null at {} and null at {}", className, curCommitId, prvCommitId);
            return Type.INS;
        } else if (curTestSuite == null) { // (prevTestSuite != null) is always true.
            LOGGER.info("{} is null at {} and not null at {}", className, curCommitId, prvCommitId);
            return Type.DEL;
        }
        TestCase curTestCase = TestSuite.getTestCaseWithClassMethodName(curr, className, methodName);
        TestCase prevTestCase = TestSuite.getTestCaseWithClassMethodName(prev, className, methodName);
        if (curTestCase == null && prevTestCase == null) {
            LOGGER.error("Why both test-cases are null?");
            return Type.None;
        } else if (curTestCase != null && prevTestCase == null) {
            LOGGER.info("{} is not null at {} and null at {}", className + ":" + methodName, curCommitId, prvCommitId);
            return Type.INS;
        } else if (curTestCase == null) { // (prevTestCase != null) is always true.
            LOGGER.info("{} is null at {} and not null at {}", className + ":" + methodName, curCommitId, prvCommitId);
            return Type.DEL;
        }
        GumTreeEngine engine = new GumTreeEngine();
        List<Action> actions = engine.getEditActions(prevTestCase, curTestCase);
        outputEditActions(project, curCommitId, prvCommitId, className, methodName, actions);
        return _compare(actions);
    }

    private Type _compareFromExistingEditScripts(final Project project, final String curCommitId, final String className, final String methodName) {
        List<String> content;
        try {
            content = Files.readAllLines(getPathToOutputEditActions(project, curCommitId, className, methodName), Charsets.UTF_8);
        } catch (IOException e) {
            return Type.None;
        }
        List<Action> actions = analyzeEditActions(content);
        return _compare(actions);
    }

    private Type _compare(List<Action> actions) {
        if (actions.isEmpty()) {
            return Type.None;
        }
        boolean ins = false;
        boolean mov = false;
        boolean del = false;
        boolean upd = false;
        for (Action action : actions) {
            if (action instanceof Insert) {
                ins = true;
            } else if (action instanceof Move) {
                System.out.println("Move is found!");
                mov = true;
            } else if (action instanceof Delete) {
                del = true;
            } else if (action instanceof Update) {
                upd = true;
            }
        }
        if (ins && mov && del &&  upd) {
            return Type.INS_MOV_DEL_UPD;
        } else if (ins && mov && del) {
            return Type.INS_MOV_DEL;
        } else if (ins && mov && upd) {
            return Type.INS_MOV_UPD;
        } else if (ins && del && upd) {
            return Type.INS_DEL_UPD;
        } else if (mov && del && upd) {
            return Type.MOV_DEL_UPD;
        } else if (ins && mov) {
            return Type.INS_MOV;
        } else if (ins && del) {
            return Type.INS_DEL;
        } else if (ins && upd) {
            return Type.INS_UPD;
        } else if (mov && del) {
            return Type.MOV_DEL;
        } else if (mov && upd) {
            return Type.MOV_UPD;
        } else if (del && upd) {
            return Type.DEL_UPD;
        } else if (ins) {
            return Type.INS;
        } else if (mov) {
            return Type.MOV;
        } else if (del) {
            return Type.DEL;
        } else if (upd) {
            return Type.UPD;
        } else {
            return Type.None;
        }
    }

    private List<Action> analyzeEditActions(List<String> contents) {
        List<Action> actions = new ArrayList<>();
        if (contents.size() < 3) {
            return actions;
        }
        for (int i = 2; i < contents.size(); i++) {
            String content = contents.get(i);
            if (content.startsWith("INS")) {
                actions.add(new Insert(null, null, -1));
            } else if (content.startsWith("DEL")) {
                actions.add(new Delete(null));
            } else if (content.startsWith("MOV")) {
                actions.add(new Move(null, null, -1));
            } else if (content.startsWith("UPD")) {
                actions.add(new Update(null, ""));
            }
        }
        return actions;
    }

    private Map<String, TestSuite> getTestSuites(Project project) {
        try {
            return listToMapTestSuites(MavenUtils.getTestSuites(project.getProjectDir()));
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
    private void outputGumTreeDiff(Type type) {
        StringBuilder sb = stringBuilderMap.get(type.toString());
        VtrUtils.writeContent(getPathToOutputFile(type), sb.toString());
    }
    
    /* To get path to output */
    private Path getPathToOutputFile(Type pattern) {
        String filename = pattern.toString();
        String className = this.getClass().toString();
        className = className.substring(className.lastIndexOf(".") + 1);
        return VtrUtils.getPathToFile(outputDir.getPath(), className, filename + ".csv");
    }
    private Path getPathToOutputEditActions(Project project, String commitId, String className, String methodName) {
        return VtrUtils.getPathToFile(project.getOutputDir().toString(), project.getProjectId(), GUMTREE_DIR, commitId, className, methodName, "actions.txt");
    }
}

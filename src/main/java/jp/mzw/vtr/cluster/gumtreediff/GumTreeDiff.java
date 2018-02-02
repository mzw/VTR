package jp.mzw.vtr.cluster.gumtreediff;

import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.Insert;
import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.actions.model.Update;
import jp.mzw.vtr.cluster.BeforeAfterComparator;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;
import org.apache.commons.io.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Classifier for Test-Case Modifications with GumTree
 *
 * @author Keita Tsukamoto
 */
public class GumTreeDiff extends BeforeAfterComparator {
    private static Logger LOGGER = LoggerFactory.getLogger(GumTreeDiff.class);

    /** A directory name to output classification results */
    private static final String GUMTREE_DIR = "gumtree";

    /** Contain a previous target test suites */
    private List<TestSuite> prvTestSuites;
    /** Contain a current target test suites */
    private List<TestSuite> curTestSuites;

    /** Contain classification results by each type */
    Map<Type, StringBuilder> results;

    /**
     * Constructor
     *
     * @param projectDir
     * @param outputDir
     */
    public GumTreeDiff(final File projectDir, final File outputDir) {
        super(projectDir, outputDir);
    }

    /**
     * Types of classification results
     */
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
    public void prepare() {
        results = new HashMap<>();
        for (Type type : Type.values()) {
            results.put(type, new StringBuilder());
        }
    }

    @Override
    public void prepareEach(final Project project) {
        // NOP
    }

    @Override
    public void before(final Project project, final String commitId) {
        try {
            prvTestSuites = MavenUtils.getTestSuites(project.getProjectDir());
        } catch(Exception e) {
            LOGGER.warn("Not found previous test suites: {}, commit: {}", project.getProjectId(), commitId);
        }
    }

    @Override
    public void after(final Project project, final String commitId) {
        try {
            curTestSuites = MavenUtils.getTestSuites(project.getProjectDir());
        } catch(Exception e) {
            LOGGER.warn("Not found current test suites: {}, commit: {}", project.getProjectId(), commitId);
        }
    }


    @Override
    public void compare(final Project project, final String prvCommit, final String curCommit, final String className, final String methodName) {
        Type type;
        if (isDone(project, curCommit, className, methodName)) {
            type = read(project, curCommit, className, methodName);
        } else {
            type = apply(project, prvCommit, curCommit, className, methodName);
        }
        VtrUtils.addCsvRecords(results.get(type), project.getProjectId(), prvCommit, curCommit, className, methodName);
    }

    @Override
    public void output() {
        for (Type type : Type.values()) {
            VtrUtils.writeContent(getPathToOutputFile(type), results.get(type).toString());
        }
    }

    /**
     * Determine whether GumTree has been applied already
     *
     * @param project is a project under analysis
     * @param commitId is a current target commit ID
     * @param className is a name of current target test class
     * @param methodName is a name of current target test method
     * @return true if already done, otherwise false
     */
    private boolean isDone(Project project, String commitId, String className, String methodName) {
        final Path path = getPathToOutputEditActions(project, commitId, className, methodName);
        return Files.exists(path);
    }

    /**
     * Apply GumTree to get edit actions of a modified test case
     *
     * @param project is a project containing a test case
     * @param prvCommitId is a previous target commit ID
     * @param curCommitId is a current target commit ID
     * @param className is a name of current target test class
     * @param methodName is a name of current target test method
     * @return a type of this modification
     */
    private Type apply(final Project project, final String prvCommitId, final String curCommitId, final String className, final String methodName) {
        TestCase curTestCase = TestSuite.getTestCaseWithClassMethodName(curTestSuites, className, methodName);
        TestCase prvTestCase = TestSuite.getTestCaseWithClassMethodName(prvTestSuites, className, methodName);
        if (curTestCase == null && prvTestCase == null) {
            LOGGER.error("Both previous and current test cases are null");
            return Type.None;
        } else if (curTestCase != null && prvTestCase == null) {
            LOGGER.info("{} is not null at {} and null at {}", className + ":" + methodName, curCommitId, prvCommitId);
            return Type.INS;
        } else if (curTestCase == null) { // (prevTestCase != null) is always true.
            LOGGER.info("{} is null at {} and not null at {}", className + ":" + methodName, curCommitId, prvCommitId);
            return Type.DEL;
        }
        GumTreeEngine engine = new GumTreeEngine();
        List<Action> actions = engine.getEditActions(prvTestCase, curTestCase);
        outputEditActions(project, curCommitId, prvCommitId, className, methodName, actions);
        return classify(actions);
    }

    /**
     * Read existing results of applying GumTree
     *
     * @param project
     * @param curCommitId
     * @param className
     * @param methodName
     * @return
     */
    private Type read(final Project project, final String curCommitId, final String className, final String methodName) {
        try {
            List<String> contents = Files.readAllLines(getPathToOutputEditActions(project, curCommitId, className, methodName), Charsets.UTF_8);
            if (contents.size() < 3) {
                return Type.None;
            }
            List<Action> actions = new ArrayList<>();
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
            return classify(actions);
        } catch (IOException e) {
            return Type.None;
        }
    }

    /**
     * Classify a test-case modifications based on its edit actions
     *
     * @param actions
     * @return
     */
    private Type classify(List<Action> actions) {
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
                mov = true;
            } else if (action instanceof Delete) {
                del = true;
            } else if (action instanceof Update) {
                upd = true;
            }
        }
        if (ins && mov && del && upd) {
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

    /**
     * Output edit actions by applying GumTree
     *
     * @param project
     * @param curCommitId
     * @param prvCommitId
     * @param className
     * @param methodName
     * @param actions
     */
    private void outputEditActions(Project project, String curCommitId, String prvCommitId, String className, String methodName, List<Action> actions) {
        StringBuilder sb = new StringBuilder();
        sb.append("previous commit : ").append(curCommitId).append("\n");
        sb.append("current commit : ").append(prvCommitId).append("\n");
        for (Action action : actions) {
            sb.append(action.toString()).append("\n");
        }
        VtrUtils.writeContent(getPathToOutputEditActions(project, curCommitId, className, methodName), sb.toString());
    }

    /**
     * Get a path to an output file
     *
     * @param type
     * @return
     */
    private Path getPathToOutputFile(Type type) {
        String filename = type.toString();
        String className = this.getClass().toString();
        className = className.substring(className.lastIndexOf(".") + 1);
        return VtrUtils.getPathToFile(outputDir.getPath(), className, filename + ".csv");
    }

    /**
     * Get a path to an output file containing edit actions
     *
     * @param project
     * @param commitId
     * @param className
     * @param methodName
     * @return
     */
    private Path getPathToOutputEditActions(Project project, String commitId, String className, String methodName) {
        return VtrUtils.getPathToFile(project.getOutputDir().toString(), project.getProjectId(), GUMTREE_DIR, commitId, className, methodName, "actions.txt");
    }
}

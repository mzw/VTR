package jp.mzw.vtr.cluster.wild_caught_mutants;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jp.mzw.vtr.cluster.BeforeAfterComparator;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.TestCase;
import jp.mzw.vtr.maven.TestSuite;
import jp.mzw.vtr.validate.ValidatorBase;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class PatchCollector extends BeforeAfterComparator {
    private static Logger LOGGER = LoggerFactory.getLogger(PatchCollector.class);

    public static final String PATCH_COLLECTOR_DIR = "PatchCollector";
    public static final String PATCH_COLLECTOR_FILE = "patches.diff";

    /** Contain patches as results */
    List<List<String>> patches;

    /** Contain previous test suites */
    private List<TestSuite> prvTestSuites;
    /** Contain source codes of previous test suites */
    private Map<String, List<String>> prvTestSuiteCodes;

    /** Contain current test suites */
    private List<TestSuite> curTestSuites;
    /** Contain source codes of current test suites */
    private Map<String, List<String>> curTestSuiteCodes;

    /** If true, excluding parts of other test cases; otherwise, extracting parts of a given test case */
    private final boolean exclude;

    public PatchCollector(final File projectDir, final File outputDir, final boolean exclude) {
        super(projectDir, outputDir);
        this.exclude = exclude;
    }

    @Override
    public void prepare() {
        patches = Lists.newArrayList();
    }

    @Override
    public void prepareEach(final Project project) {
        // NOP
    }

    @Override
    public void before(final Project project, final String commitId) {
        try {
            prvTestSuites = MavenUtils.getTestSuites(project.getProjectDir());
            prvTestSuiteCodes = Maps.newHashMap();
            for (final TestSuite suite : prvTestSuites) {
                final File file = suite.getTestFile();
                final List<String> lines = FileUtils.readLines(file);
                prvTestSuiteCodes.put(suite.getTestClassName(), lines);
            }
        } catch(Exception e) {
            LOGGER.warn("Not found previous test suites: {}, commit: {}", project.getProjectId(), commitId);
        }
    }

    @Override
    public void after(final Project project, final String commitId) {
        try {
            curTestSuites = MavenUtils.getTestSuites(project.getProjectDir());
            curTestSuiteCodes = Maps.newHashMap();
            for (final TestSuite suite : curTestSuites) {
                final File file = suite.getTestFile();
                final List<String> lines = FileUtils.readLines(file);
                curTestSuiteCodes.put(suite.getTestClassName(), lines);
            }
        } catch(Exception e) {
            LOGGER.warn("Not found current test suites: {}, commit: {}", project.getProjectId(), commitId);
        }
    }


    @Override
    public void compare(final Project project, final String prvCommit, final String curCommit, final String className, final String methodName) {
        final TestCase curTestCase = TestSuite.getTestCaseWithClassMethodName(curTestSuites, className, methodName);
        final TestCase prvTestCase = TestSuite.getTestCaseWithClassMethodName(prvTestSuites, className, methodName);

        final File curTestFile = curTestCase.getTestFile();
        final File prvTestFile = prvTestCase != null ? prvTestCase.getTestFile() : curTestFile;

        String curTestCaseCode = "";
        String prvTestCaseCode = "";

        // FIXME: Keep raw text (not compressed text)
        if (curTestCase != null) {
            final List<String> lines = curTestSuiteCodes.get(curTestCase.getTestSuite().getTestClassName());
            curTestCaseCode = getContent(curTestCase, lines);
        }
        if (prvTestCase != null) {
            final List<String> lines = prvTestSuiteCodes.get(prvTestCase.getTestSuite().getTestClassName());
            prvTestCaseCode = getContent(prvTestCase, lines);
        }

        List<String> patch = ValidatorBase.genPatch(prvTestCaseCode, curTestCaseCode, prvTestFile, curTestFile);
        patches.add(patch);
    }

    /**
     * Get source code lines related to a given test case
     *
     * @param testCase
     * @param lines
     * @return
     */
    private String getContent(final TestCase testCase, final List<String> lines) {
        StringBuilder content = new StringBuilder();
        String delim = "";
        if (exclude) {
            for (int i = 0; i < lines.size(); i++) {
                boolean isLineForOthers = false;
                for (TestCase other : testCase.getTestSuite().getTestCases()) {
                    if (other.getFullName().equals(testCase.getFullName())) {
                        continue;
                    }
                    int start = other.getStartLineNumber();
                    int end = other.getEndLineNumber();
                    if (start - 1 <= i && i < end) {
                        isLineForOthers = true;
                        break;
                    }
                }
                if (isLineForOthers) {
                    content.append(delim).append(""); // add empty line
                } else {
                    content.append(delim).append(lines.get(i));
                }
                delim = "\n";
            }
        } else {
            for (int i = testCase.getStartLineNumber() - 1; i < testCase.getEndLineNumber(); i++) {
                content.append(delim).append(lines.get(i));
                delim = "\n";
            }
        }
        return content.toString();
    }

    @Override
    public void output() {
        try {
            File dir = new File(outputDir, PATCH_COLLECTOR_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, PATCH_COLLECTOR_FILE);
            boolean append = false;
            for (List<String> patch : patches) {
                FileUtils.writeLines(file, patch, append);
                append = true;
            }
            FileUtils.copyFile(file, new File(dir, exclude ? "exclude_" : "extract_" + PATCH_COLLECTOR_FILE));
        } catch (IOException e) {
            LOGGER.error("Error: {}", e.getMessage());
        }
    }

}

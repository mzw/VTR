package jp.mzw.vtr.cluster.testedness;

import jp.mzw.vtr.CLI;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.detect.DetectionResult;
import jp.mzw.vtr.dict.Dictionary;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.maven.JacocoInstrumenter;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.PitInstrumenter;
import jp.mzw.vtr.maven.TestCase;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.dom4j.DocumentException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Testedness {
    private static Logger LOGGER = LoggerFactory.getLogger(Testedness.class);

    /** A directory name to output classification results */
    static final String TESTEDNESS_DIR = "testedness";
    private static final String JACOCO_DIR = "jacoco";
    private static final String PITEST_DIR = "pitest";

    private static final String BLACKLIST = "blacklists/testedness.csv";

    /** A directory containing projects under analysis  */
    protected final File projectDir;
    /** A directory to output analysis results */
    protected final File outputDir;

    private Map<Type, StringBuilder> results;
    private Project project;

    public Testedness(final File projectDir, final File outputDir) {
        this.projectDir = projectDir;
        this.outputDir = outputDir;
    }

    /**
     * Compare before and after versions of projects under analysis
     *
     * @param results
     * @throws IOException
     * @throws ParseException
     * @throws GitAPIException
     */
    public void run(final List<DetectionResult> results) throws IOException, ParseException, GitAPIException {
        prepare();

        for (final DetectionResult result : results) {
            final String projectId = result.getSubjectName();
            LOGGER.info("Project: " + projectId);

            final Project project = new Project(projectId).setConfig(CLI.CONFIG_FILENAME);
            final CheckoutConductor git = new CheckoutConductor(project);
            final Dictionary dict = new Dictionary(this.outputDir, projectId).parse().createPrevCommitByCommitIdMap();

            prepareEach(project);

            final Map<String, List<String>> commits = result.getResults();
            for (final String curCommit : commits.keySet()) {
                // Classifying additive, subtractive, or altering patches
                List<String> testcases = commits.get(curCommit);
                for (final String testcase : testcases) {
                    final String className = TestCase.getClassName(testcase);
                    try {
                        final String methodName = TestCase.getMethodName(testcase);
                        String prvCommit = dict.getPrevCommitBy(curCommit).getId();
                        // skip if (projectId, curCommit, className) is listed on blacklist.
                        if (isSkip(projectId, curCommit, className)) {
                            LOGGER.warn("SKIP: {} @ {} on {}", className, curCommit, projectId);
                            continue;
                        }
                        // skip if (projectId, prvCommit, className) is listed on blacklist.
                        if (isSkip(projectId, prvCommit, className)) {
                            LOGGER.warn("SKIP: {} @ {} on {}", className, curCommit, projectId);
                            continue;
                        }
                        // After version of a project under analysis
                        if (!isDone(curCommit, className)) {
                            LOGGER.info("Checkout (after modified): " + curCommit);
                            git.checkoutAt(curCommit);
                            after(project, curCommit, className);
                        }
                        // Before version of a project under analysis
                        if (!isDone(prvCommit, className)) {
                            LOGGER.info("Checkout (before modified): " + prvCommit + " previous to " + curCommit);
                            git.checkoutAt(prvCommit);
                            before(project, prvCommit, className);
                        }
                        compare(project, prvCommit, curCommit, className, methodName);
                    } catch (NullPointerException e) {
                        LOGGER.warn("NullPointerException: {} @ {}", className, curCommit);
                    }
                }
                git.checkoutAt(git.getLatestCommit().getId());
            }
        }
        output();
    }

    private boolean isSkip(String projectId, String commit, String className) {
        List<CSVRecord> blacklists =VtrUtils.getCsvRecords(
                Paths.get(Testedness.class.getClassLoader().getResource(BLACKLIST).getPath())
        );
        for (CSVRecord blacklist : blacklists) {
            if (projectId.equals(blacklist.get(0))
                    && commit.equals(blacklist.get(1))
                    && className.equals(blacklist.get(2))) {
                return true;
            }
        }
        return false;
    }


    public void prepare() {
        results = new HashMap<>();
        for (Testedness.Type type : Testedness.Type.values()) {
            results.put(type, new StringBuilder());
        }
    }

    enum Type {
        JACOCO_PITEST,
        JACOCO,
        PITEST,
        NONE,
    }


    public void prepareEach(final Project project) {
        this.project = project;
    }

    public void before(final Project project, final String commitId, String className) {
        try {
            runJacocoAndPitest(project, commitId, className);
        } catch(Exception e) {
            LOGGER.warn("Not found previous test suites: {}, commit: {}", project.getProjectId(), commitId);
        }
    }

    public void after(final Project project, final String commitId, String className) {
        try {
            runJacocoAndPitest(project, commitId, className);
        } catch(Exception e) {
            LOGGER.warn("Not found current test suites: {}, commit: {}", project.getProjectId(), commitId);
        }
    }

    public void compare(final Project project, final String prvCommit, final String curCommit, final String className, final String methodName) {
        Testedness.Type type = Type.NONE;
        // compare coverage score
        boolean coverageIncrease = false;
        int prvCoverageScore = getCoverageScore(prvCommit, className);
        int curCoverageScore = getCoverageScore(curCommit, className);
        if (prvCoverageScore < curCoverageScore) {
            coverageIncrease = true;
        }
        // compare num of killed mutants
        boolean mutationIncrease = false;
        int prvNumOfKilledMutants = getNumOfKilledMutants(prvCommit, className);
        int curNumOfKilledMutants = getNumOfKilledMutants(curCommit, className);
        if (prvNumOfKilledMutants < curNumOfKilledMutants) {
            mutationIncrease = true;
        }

        if (coverageIncrease && mutationIncrease) {
            type = Type.JACOCO_PITEST;
        } else if (coverageIncrease) {
            type = Type.JACOCO;
        } else if (mutationIncrease) {
            type = Type.PITEST;
        }

        VtrUtils.addCsvRecords(results.get(type), project.getProjectId(), prvCommit, curCommit, className, methodName);
    }

    public void output() {
        for (Testedness.Type type : Testedness.Type.values()) {
            VtrUtils.writeContent(getPathToOutputFile(type), results.get(type).toString());
        }
    }

    private boolean isDone(String commit, String className) {
        return Files.exists(getPathToJacocoOutput(commit, className))
                && Files.exists(getPathToPitestOutput(commit, className));
    }

    protected void runJacocoAndPitest(final Project project, final String commitId, String className) {
        try {
            JacocoInstrumenter ji = new JacocoInstrumenter(project.getProjectDir());
            boolean modified = ji.instrument();
            runJacoco(commitId, className);
            copyJacocoOutput(commitId, className);
            if (modified) {
                ji.revert();
            }
        } catch (MavenInvocationException e) {
            LOGGER.warn("Error when maven invoking jacoco.");
        } catch (DocumentException e) {
            LOGGER.warn("Error when instrumenting jacoco.");
        } catch (IOException e) {
            LOGGER.warn("Error when copying jacoco result.");
        }
        try {
            PitInstrumenter pi = new PitInstrumenter(project.getProjectDir(), PitInstrumenter.getTargetClasses(project.getProjectDir()), className);
            boolean modified = pi.instrument();
            runPitest(commitId, className);
            copyPitestOutput(commitId, className);
            if (modified) {
                pi.revert();
            }
        } catch (MavenInvocationException e) {
            LOGGER.warn("Error when maven invoking pitest.");
        } catch (IOException e) {
            LOGGER.warn("Error when copying pitest result.");
        }
    }


    protected void runJacoco(String commit, String testCaseFullName) throws MavenInvocationException {
        LOGGER.info("Measure coverage: {} @ {}", testCaseFullName, commit);
        String each = "-Dtest=" + testCaseFullName;
        List<String> args = Arrays.asList("clean", "compile", "test-compile",
                "org.jacoco:jacoco-maven-plugin:prepare-agent", "test", each, "-DfailIfNoTests=false", "org.jacoco:jacoco-maven-plugin:report");
        runMaven(args);
        return;
    }

    protected void runPitest(String commit, String testCaseFullName) throws MavenInvocationException {
        LOGGER.info("Mutation score: {} @ {}", testCaseFullName, commit);
        List<String> args = Arrays.asList("clean", "compile", "test-compile", "org.pitest:pitest-maven:mutationCoverage");
        runMaven(args);
        return;
    }

    protected void runMaven(List<String> args) throws MavenInvocationException {
        MavenUtils.maven(this.project.getProjectDir(), args, this.project.getMavenHome());
        return;
    }


    private int getCoverageScore(String commit, String className) {
        String content = getJacocoReport(commit, className);
        if (content.equals("")) {
            return -1;
        }
        Document document = Jsoup.parse(content);
        Element element = document.select("#coveragetable tfoot tr .ctr2").first();
        return Integer.parseInt(element.text().replace("%", ""));
    }

    private String getJacocoReport(String commit, String className) {
        String content = "";
        try {
            content = Files.lines(
                    VtrUtils.getPathToFile(getPathToJacocoOutput(commit, className).toString(), "jacoco/index.html"),
                    Charset.forName("UTF-8")
            ).collect(Collectors.joining(System.getProperty("line.separator")));
        } catch (IOException e) {
            LOGGER.warn("Not found jacoco report of {} @ {}", className, commit);
        }
        return content;
    }

    private int getNumOfKilledMutants(String commit, String className) {
        String content = getPitestReport(commit, className);
        if (content.equals("")) {
            return -1;
        }
        Document document = Jsoup.parse(content, "", Parser.xmlParser());
        Elements elements = document.select("body > table:nth-child(3) > tbody > tr > td:nth-child(3) > div > div.coverage_ledgend");
        String text = elements.get(0).text();
        String[] split = text.split("/");
        return Integer.parseInt(split[0]);
    }

    private String getPitestReport(String commit, String className) {
        String content = "";
        try {
            content = Files.lines(
                    VtrUtils.getPathToFile(getPathToPitestOutput(commit, className).toString(), "pit-reports/index.html"),
                    Charset.forName("UTF-8")
            ).collect(Collectors.joining(System.getProperty("line.separator")));
        } catch (IOException e) {
            LOGGER.warn("Not found pitest report of {} @ {}", className, commit);
        }
        return content;
    }


    private void copyJacocoOutput(String commit, String testCaseFullName) throws IOException {
        FileUtils.copyDirectoryToDirectory(VtrUtils.getPathToFile(project.getProjectDir().getAbsolutePath(), "target/site/jacoco").toFile(),
                getPathToJacocoOutput(commit, testCaseFullName).toFile());
    }

    private void copyPitestOutput(String commit, String testCaseFullName) throws IOException {
        FileUtils.copyDirectoryToDirectory(VtrUtils.getPathToFile(project.getProjectDir().getAbsolutePath(), "target/pit-reports").toFile(),
                getPathToPitestOutput(commit, testCaseFullName).toFile());
    }

    private Path getPathToJacocoOutput(String commit, String testCaseFullName) {
        return VtrUtils.getPathToFile(project.getOutputDir().getAbsolutePath(), project.getProjectId(),
                TESTEDNESS_DIR, commit, testCaseFullName, JACOCO_DIR);
    }
    private Path getPathToPitestOutput(String commit, String testCaseFullName) {
        return VtrUtils.getPathToFile(project.getOutputDir().getAbsolutePath(), project.getProjectId(),
                TESTEDNESS_DIR, commit, testCaseFullName, PITEST_DIR);
    }

    Path getPathToOutputFile(Type type) {
        String filename = type.toString();
        return VtrUtils.getPathToFile(outputDir.getPath(), TESTEDNESS_DIR, filename + ".csv");
    }

}

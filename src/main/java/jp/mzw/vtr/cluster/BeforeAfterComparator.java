package jp.mzw.vtr.cluster;

import jp.mzw.vtr.CLI;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.DetectionResult;
import jp.mzw.vtr.dict.Dictionary;
import jp.mzw.vtr.git.CheckoutConductor;
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

public abstract class BeforeAfterComparator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeforeAfterComparator.class);

    /** A directory containing projects under analysis  */
    protected final File projectDir;

    /** A directory to output analysis results */
    protected final File outputDir;

    public BeforeAfterComparator(final File projectDir, final File outputDir) {
        this.projectDir = projectDir;
        this.outputDir = outputDir;
    }

    public abstract void prepare(Project project);

    public abstract void before(Project project, String commitId);

    public abstract void after(Project project, String commitId);

    public abstract Type compare(Project project, String prvCommitId, String curCommitId, String className, String methodName);


    /**
     * Compare before and after versions of projects under analysis
     *
     * @param results
     * @throws IOException
     * @throws ParseException
     * @throws GitAPIException
     */
    public void run(final List<DetectionResult> results) throws IOException, ParseException, GitAPIException {
        // StringBuilders to contain results
        StringBuilder additiveSb    = new StringBuilder();
        StringBuilder subtractiveSb = new StringBuilder();
        StringBuilder alteringSb    = new StringBuilder();
        StringBuilder noneSb        = new StringBuilder();
        for (final DetectionResult result : results) {
            final String projectId = result.getSubjectName();
            LOGGER.info("Project: " + projectId);

            final Project project = new Project(projectId).setConfig(CLI.CONFIG_FILENAME);
            final CheckoutConductor git = new CheckoutConductor(project);
            final Dictionary dict = new Dictionary(this.outputDir, projectId).parse().createPrevCommitByCommitIdMap();

            prepare(project);

            final Map<String, List<String>> commits = result.getResults();
            for (final String curCommit : commits.keySet()) {

                // After version of a project under analysis
                LOGGER.info("Checkout (after modified): " + curCommit);
                git.checkout(CheckoutConductor.Type.At, curCommit);
                after(project, curCommit);

                // Before version of a project under analysis
                String prvCommit = dict.getPrevCommitBy(curCommit).getId();
                LOGGER.info("Checkout (before modified): " + prvCommit + " previous to " + curCommit);
                git.checkout(CheckoutConductor.Type.At, prvCommit);
                before(project, prvCommit);

                // Classifying additive, subtractive, or altering patches
                List<String> testcases = commits.get(curCommit);
                for (final String testcase : testcases) {
                    final String className = TestCase.getClassName(testcase);
                    final String methodName = TestCase.getMethodName(testcase);
                    Type type = compare(project, curCommit, prvCommit, className, methodName);
                    if (type.equals(Type.Additive)) {
                        generateContent(additiveSb, project.getProjectId(), curCommit, prvCommit, className, methodName);
                    } else if (type.equals(Type.Subtractive)) {
                        generateContent(subtractiveSb, project.getProjectId(), curCommit, prvCommit, className, methodName);
                    } else if (type.equals(Type.Altering)) {
                        generateContent(alteringSb, project.getProjectId(), curCommit, prvCommit, className, methodName);
                    } else if (type.equals(Type.None)) {
                        generateContent(noneSb, project.getProjectId(), curCommit, prvCommit, className, methodName);
                    }

                }
            }
        }
        outputAddictive(additiveSb.toString());
        outputSubtractive(subtractiveSb.toString());
        outputAltering(alteringSb.toString());
        outputNone(noneSb.toString());
    }

    public enum Type {
        Additive,
        Subtractive,
        Altering,
        None
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
        String className = this.getClass().toString();
        className = className.substring(className.lastIndexOf(".") + 1);
        return Paths.get(String.join("/", outputDir.getPath(), className));
    }

}

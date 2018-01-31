package jp.mzw.vtr.cluster;

import jp.mzw.vtr.CLI;
import jp.mzw.vtr.cluster.grouminer.GrouMiner;
import jp.mzw.vtr.cluster.grouminer.IntegratedGrouMinerEngine;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.DetectionResult;
import jp.mzw.vtr.dict.Dictionary;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.maven.TestCase;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public abstract class BeforeAfterComparer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BeforeAfterComparer.class);

    /** A directory containing projects under analysis  */
    protected final File projectDir;

    /** A directory to output analysis results */
    protected final File outputDir;

    public BeforeAfterComparer(final File projectDir, final File outputDir) {
        this.projectDir = projectDir;
        this.outputDir = outputDir;
    }

    public abstract void prepare(Project project);

    public abstract void before(Project project, String commitId);

    public abstract void after(Project project, String commitId);

    public abstract void compare(Project project, String prvCommitId, String curCommitId, String className, String methodName);


    /**
     * Compare before and after versions of projects under analysis
     *
     * @param results
     * @throws IOException
     * @throws ParseException
     * @throws GitAPIException
     */
    public void compare(final List<DetectionResult> results) throws IOException, ParseException, GitAPIException {
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

                    compare(project, curCommit, prvCommit, className, methodName);
                }
            }
        }
    }

}

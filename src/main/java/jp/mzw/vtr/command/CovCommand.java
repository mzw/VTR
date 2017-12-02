package jp.mzw.vtr.command;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.maven.TestRunner;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.text.ParseException;

import static jp.mzw.vtr.CLI.CONFIG_FILENAME;

/**
 * measure each test case's coverage at each commit
 */
public class CovCommand {
    public static void command(String... args) throws IOException, ParseException, GitAPIException {
        String projectId = args[0];
        Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
        if (args.length == 1) {
            cov(project);
        } else if (args.length == 3) { // specific commit(s)
            CheckoutConductor.Type type = CheckoutConductor.Type.valueOf(args[1]);
            String commitId = args[2];
            cov(project, type, commitId);
        } else {
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI cov <subject-id>");
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI cov <subject-id> At    <commit-id>");
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI cov <subject-id> After <commit-id>");
        }
    }

    private static void cov(Project project) throws IOException, ParseException, GitAPIException {
        CheckoutConductor cc = new CheckoutConductor(project);
        cc.addListener(new TestRunner(project));
        cc.checkout();
    }

    private static void cov(Project project, CheckoutConductor.Type type, String commitId) throws IOException, ParseException, GitAPIException {
        CheckoutConductor cc = new CheckoutConductor(project);
        cc.addListener(new TestRunner(project));
        cc.checkout(type, commitId);
    }
}

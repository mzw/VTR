package jp.mzw.vtr.command;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.git.CheckoutConductor;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.text.ParseException;

import static jp.mzw.vtr.CLI.CONFIG_FILENAME;

/**
 * detect test case modifications for previously released source programs
 */
public class DetectCommand {
    public static void command(String... args) throws IOException, ParseException, GitAPIException {
        String projectId = args[1];
        Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
        if (args.length == 2) { // all commits
            detect(project);
        } else if (args.length == 4) { // specific commit(s)
            CheckoutConductor.Type type = CheckoutConductor.Type.valueOf(args[2]);
            String commitId = args[3];
            detect(project, type, commitId);
        } else {
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI detect <subject-id>");
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI detect <subject-id> At    <commit-id>");
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI detect <subject-id> After <commit-id>");
        }
    }
    private static void detect(Project project) throws IOException, ParseException, GitAPIException {
        CheckoutConductor cc = new CheckoutConductor(project);
        cc.addListener(new Detector(project).loadGeneratedSourceFileList(Detector.GENERATED_SOURCE_FILE_LIST));
        cc.checkout();
    }
    private static void detect(Project project, CheckoutConductor.Type type, String commitId) throws IOException, ParseException, GitAPIException {
        CheckoutConductor cc = new CheckoutConductor(project);
        cc.addListener(new Detector(project).loadGeneratedSourceFileList(Detector.GENERATED_SOURCE_FILE_LIST));
        cc.checkout(type, commitId);
    }
}

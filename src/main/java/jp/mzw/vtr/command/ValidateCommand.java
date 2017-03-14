package jp.mzw.vtr.command;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.validate.Validator;
import jp.mzw.vtr.validate.ValidatorBase;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;

import static jp.mzw.vtr.CLI.CONFIG_FILENAME;

/**
 * validate existing test cases using test case modification patterns for previously released software programs
 */
public class ValidateCommand {
    public static void command(String... args) throws IOException, ParseException, GitAPIException, InstantiationException, IllegalArgumentException, IllegalAccessException,
    InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, InterruptedException {
        String projectId = args[1];
        Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
        if (args.length == 2) { // all commits
            validate(project);
        } else if (args.length == 4) { // specific commit(s)
            CheckoutConductor.Type type = CheckoutConductor.Type.valueOf(args[2]);
            String commitId = args[3];
            validate(project, type, commitId);
        } else {
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI validate <subject-id>");
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI validate <subject-id> At    <commit-id>");
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI validate <subject-id> After <commit-id>");
        }
    }
    private static void validate(Project project) throws IOException, ParseException, GitAPIException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, InterruptedException {
        CheckoutConductor cc = new CheckoutConductor(project);
        Validator validator = new Validator(project);
        cc.addListener(validator);
        cc.checkout();
        ValidatorBase.output(project.getOutputDir(), project.getProjectId(), validator.getValidationResults());
    }
    private static void validate(Project project, CheckoutConductor.Type type, String commitId)
            throws IOException, ParseException, GitAPIException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, InterruptedException {
        CheckoutConductor cc = new CheckoutConductor(project);
        Validator validator = new Validator(project);
        cc.addListener(validator);
        cc.checkout(type, commitId);
        ValidatorBase.output(project.getOutputDir(), project.getProjectId(), validator.getValidationResults());
    }
}

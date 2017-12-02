package jp.mzw.vtr.command;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.dict.DictionaryMaker;
import jp.mzw.vtr.git.GitUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static jp.mzw.vtr.CLI.CONFIG_FILENAME;

/**
 * create a dictionary of subjects' commits and tags
 */
public class DictCommand {
     public static void command(String... args) throws IOException, NoHeadException, GitAPIException{
         if (args.length != 1) { // Invalid usage
             System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI dict      <subject-id>");
         } else {
             String projectId = args[0];
             Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
             dict(project);
         }
     }

     private static void dict(Project project) throws IOException, NoHeadException, GitAPIException {
         Git git = GitUtils.getGit(project.getProjectDir());
         DictionaryMaker dm = new DictionaryMaker(git);
         String refToComareBranch = GitUtils.getRefToCompareBranch(git);
         Map<Ref, Collection<RevCommit>> tagCommitsMap = dm.getTagCommitsMap(refToComareBranch);
         File dir = new File(project.getOutputDir(), project.getProjectId());
         dm.writeCommitListInXML(dir);
         dm.writeDictInXML(tagCommitsMap, refToComareBranch, dir);
     }

}

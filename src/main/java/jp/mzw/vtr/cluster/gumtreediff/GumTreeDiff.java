package jp.mzw.vtr.cluster.gumtreediff;

import jp.mzw.vtr.cluster.BeforeAfterComparer;
import jp.mzw.vtr.core.Project;

import java.io.File;

public class GumTreeDiff extends BeforeAfterComparer {


    public GumTreeDiff(final File projectDir, final File outputDir) {
        super(projectDir, outputDir);
    }

    @Override
    pubilc void prepare(final Project project) {

    }

    @Override
    public void before(final Project project, final String commitId) {

    }

    @Override
    public void after(final Project project, final String commitId) {

    }

    @Override
    public void compare(final Project project, final String prvCommitId, final String curCommitId, final String className, final String methodName) {

    }
}

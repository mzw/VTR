package jp.mzw.vtr.cluster.gumtreediff;

import jp.mzw.vtr.VtrTestBase;
import jp.mzw.vtr.core.Project;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GumTreeDiffTest extends VtrTestBase {

    @Test
    public void testIsDone() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        GumTreeDiff differ = new GumTreeDiff(project.getSubjectsDir(), project.getOutputDir());
        Method method = GumTreeDiff.class.getDeclaredMethod("isDone", Project.class, String.class, String.class, String.class);
        method.setAccessible(true);
        boolean actual = (boolean) method.invoke(differ, project, "", "", "");
        Assert.assertTrue(actual);
    }

}

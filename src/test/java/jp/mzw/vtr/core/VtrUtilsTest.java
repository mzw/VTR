package jp.mzw.vtr.core;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class VtrUtilsTest {

    @Test
    public void testGetNameWithoutExtention() {
        final String expect = "pom";
        final File pom = new File("pom.xml");
        final String actual = VtrUtils.getNameWithoutExtension(pom);
        Assert.assertArrayEquals(expect.toCharArray(), actual.toCharArray());
    }
}

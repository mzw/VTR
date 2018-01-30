package jp.mzw.vtr.maven;

import org.junit.Assert;
import org.junit.Test;

public class TestCaseTest {

    @Test
    public void testGetClassName() {
        final String expect = "class";
        final String actual = TestCase.getClassName("class#method");
        Assert.assertArrayEquals(expect.toCharArray(), actual.toCharArray());
    }

    @Test
    public void testGetClassNameWithNull() {
        Assert.assertNull(TestCase.getClassName(null));
    }

    @Test
    public void testGetMethodName() {
        final String expect = "method";
        final String actual = TestCase.getMethodName("class#method");
        Assert.assertArrayEquals(expect.toCharArray(), actual.toCharArray());
    }

    @Test
    public void testGetMethodNameWithNull() {
        Assert.assertNull(TestCase.getMethodName(null));
    }

}

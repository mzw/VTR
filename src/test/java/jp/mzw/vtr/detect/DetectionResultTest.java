package jp.mzw.vtr.detect;

import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class DetectionResultTest {

    private DetectionResult result;

    private static Map<String, List<String>> validData;

    @BeforeClass
    public static void beforeClass() {
        validData = Maps.newHashMap();
    }

    @Before
    public void setup() {
        this.result = new DetectionResult("foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstantiateIllegally() throws IllegalArgumentException {
        new DetectionResult(null);
    }

    @Test
    public void testConstructor() {
        Assert.assertNotNull(this.result);
    }

    @Test
    public void testWithInvalidResults() {
        this.result.setResult(null);
        Assert.assertFalse(this.result.hasResult());
    }

    @Test
    public void testWithValidResults() {
        Assert.assertFalse(this.result.hasResult());
        this.result.setResult(validData);
        Assert.assertTrue(this.result.hasResult());
    }

    @Test
    public void testGetSubjectName() {
        Assert.assertArrayEquals("foo".toCharArray(), this.result.getSubjectName().toCharArray());
    }

    @Test
    public void testGetResults() {
        this.result.setResult(validData);
        Assert.assertEquals(validData, this.result.getResults());
    }
}

package jp.mzw.vtr.cluster.grouminer;

import org.junit.Assert;
import org.junit.Test;

public class GrouMinerTest {

    @Test
    public void testConstructor() {
        Assert.assertNotNull(new GrouMiner(null, null));
    }
}

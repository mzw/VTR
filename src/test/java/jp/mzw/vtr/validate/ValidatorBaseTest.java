package jp.mzw.vtr.validate;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.junit.Test;

import jp.mzw.vtr.VtrTestBase;

public class ValidatorBaseTest extends VtrTestBase {

	@Test
	public void testGetValidators() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, ClassNotFoundException, IOException {
		List<ValidatorBase> validators = ValidatorBase.getValidators(project, "test-validators_list.txt");
		assertFalse(validators.isEmpty());
		assertArrayEquals("jp.mzw.vtr.validate.DoNotSwallowTestErrorsSilently".toCharArray(), validators.get(0).getClass().getName().toCharArray());
	}
}

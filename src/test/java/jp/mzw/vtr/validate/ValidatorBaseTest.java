package jp.mzw.vtr.validate;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import jp.mzw.vtr.VtrTestBase;

public class ValidatorBaseTest extends VtrTestBase {

	@Test
	public void testGetValidators() {
		List<ValidatorBase> validators = ValidatorBase.getValidators(project);
		assertFalse(validators.isEmpty());
	}
}

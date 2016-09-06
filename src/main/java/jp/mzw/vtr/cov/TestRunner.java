package jp.mzw.vtr.cov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestRunner implements CheckoutConductor.Listener {
	static Logger LOGGER = LoggerFactory.getLogger(TestRunner.class);

	protected String pathToMvnPrj;
	
	public TestRunner(String pathToMvnPrj) {
		this.pathToMvnPrj = pathToMvnPrj;
	}

	@Override
	public void onCheckout() {
		
	}
	
}

package jp.mzw.vtr.validate;

import java.util.ArrayList;
import java.util.List;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;

public class CheckstyleListener implements AuditListener {

	protected List<AuditEvent> errors;

	public CheckstyleListener() {
		this.errors = new ArrayList<>();
	}

	@Override
	public void auditStarted(AuditEvent event) {
		// NOP
	}

	@Override
	public void auditFinished(AuditEvent event) {
		// NOP
	}

	@Override
	public void fileStarted(AuditEvent event) {
		// NOP
	}

	@Override
	public void fileFinished(AuditEvent event) {
		// NOP
	}

	@Override
	public void addError(AuditEvent event) {
		this.errors.add(event);
	}

	@Override
	public void addException(AuditEvent event, Throwable throwable) {
		// NOP
	}

	public List<AuditEvent> getErrors() {
		return this.errors;
	}
}
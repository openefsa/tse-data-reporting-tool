package tse_validator;

import java.util.Arrays;
import java.util.Collection;

import i18n_messages.TSEMessages;
import report_validator.ReportError;

public class IndexCaseInconsistentWithStatusHerdError implements ReportError {

	private String rowId;
	private String indexCase;
	private String statusHerd;
	
	public IndexCaseInconsistentWithStatusHerdError(String rowId, String indexCase, String statusHerd) {
		this.rowId = rowId;
		this.indexCase = indexCase;
		this.statusHerd = statusHerd;
	}
	
	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		return TSEMessages.get("inconsistent.index.case.status.herd.message", indexCase, statusHerd);
	}

	@Override
	public Collection<String> getInvolvedRowsIdsMessage() {
		return Arrays.asList(rowId);
	}

	@Override
	public String getSuggestions() {
		return null;
	}

	@Override
	public Collection<String> getErroneousValues() {
		return null;
	}
}

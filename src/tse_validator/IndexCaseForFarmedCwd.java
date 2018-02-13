package tse_validator;

import java.util.Arrays;
import java.util.Collection;

import i18n_messages.TSEMessages;
import report_validator.ReportError;

public class IndexCaseForFarmedCwd implements ReportError {

	private String rowId;
	
	public IndexCaseForFarmedCwd(String rowId) {
		this.rowId = rowId;
	}
	
	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		return TSEMessages.get("index.case.farmed.cwd.message");
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

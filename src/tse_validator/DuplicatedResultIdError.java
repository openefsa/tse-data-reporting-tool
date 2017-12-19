package tse_validator;

import java.util.Arrays;
import java.util.Collection;

import i18n_messages.TSEMessages;
import report_validator.ReportError;

public class DuplicatedResultIdError implements ReportError {

	private String rowId1;
	private String rowId2;
	public DuplicatedResultIdError(String rowId1, String rowId2) {
		this.rowId1 = rowId1;
		this.rowId2 = rowId2;
	}
	
	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		return TSEMessages.get("duplicated.res.id.message");
	}

	@Override
	public Collection<String> getInvolvedRowsIdsMessage() {
		return Arrays.asList(rowId1, rowId2);
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

package tse_validator;

import java.util.Arrays;
import java.util.Collection;

import report_validator.ReportError;

public class MissingMandatoryFieldError implements ReportError {

	private String field;
	private String rowId;
	
	public MissingMandatoryFieldError(String field, String rowId) {
		this.field = field;
		this.rowId = rowId;
	}
	
	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		return "Mandatory field missing: " + field;
	}

	@Override
	public Collection<String> getInvolvedRowsIdsMessage() {
		return Arrays.asList(rowId);
	}

	@Override
	public String getCorrectExample() {
		return null;
	}

	@Override
	public Collection<String> getErroneousValues() {
		return null;
	}

}

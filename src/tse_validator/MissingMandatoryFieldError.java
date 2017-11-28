package tse_validator;

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
	public String getInvolvedRowsIdsMessage() {
		return rowId;
	}

	@Override
	public String getCorrectExample() {
		return null;
	}

	@Override
	public String getErroneousValue() {
		return null;
	}

}

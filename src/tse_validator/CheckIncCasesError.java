package tse_validator;

import report_validator.ReportError;

public class CheckIncCasesError implements ReportError {

	private String rowId;
	public CheckIncCasesError(String rowId) {
		this.rowId = rowId;
	}
	
	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		return "Number of declared inconclusive cases not consistent with number of specified inconclusive cases";
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

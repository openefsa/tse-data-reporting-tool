package tse_validator;

import java.util.Arrays;
import java.util.Collection;

import report_validator.ReportError;

public class TooManyCasesError implements ReportError {

	private String rowId;
	public TooManyCasesError(String rowId) {
		this.rowId = rowId;
	}
	
	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		return "Number of specified cases is greater than number of declared cases";
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

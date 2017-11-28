package tse_validator;

import report_validator.ReportError;

public class AlleleNotReportableError implements ReportError {

	private String rowId;
	private String wrongValue;
	public AlleleNotReportableError(String rowId, String wrongValue) {
		this.rowId = rowId;
		this.wrongValue = wrongValue;
	}
	
	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		return "Alleles cannot be specified for non-genotyping tests";
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
		return wrongValue;
	}

}

package tse_validator;

import java.util.Arrays;
import java.util.Collection;

import i18n_messages.TSEMessages;
import report_validator.ReportError;

public class AlleleNotReportableError implements ReportError {

	private String rowId;
	private String wrongValue1;
	private String wrongValue2;
	public AlleleNotReportableError(String rowId, String wrongValue1, String wrongValue2) {
		this.rowId = rowId;
		this.wrongValue1 = wrongValue1;
		this.wrongValue2 = wrongValue2;
	}
	
	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		return TSEMessages.get("alleles.message");
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
		return Arrays.asList(wrongValue1, wrongValue2);
	}

}

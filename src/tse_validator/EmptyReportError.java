package tse_validator;

import java.util.Collection;

import i18n_messages.TSEMessages;
import report_validator.ReportError;

public class EmptyReportError implements ReportError {

	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		return TSEMessages.get("empty.report.message");
	}

	@Override
	public Collection<String> getInvolvedRowsIdsMessage() {
		return null;
	}

	@Override
	public String getSuggestions() {
		return TSEMessages.get("empty.report.tip");
	}

	@Override
	public Collection<String> getErroneousValues() {
		return null;
	}

}

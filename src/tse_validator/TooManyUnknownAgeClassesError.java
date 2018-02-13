package tse_validator;

import java.util.Collection;

import i18n_messages.TSEMessages;
import report_validator.ReportError;

public class TooManyUnknownAgeClassesError implements ReportError {

	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		return TSEMessages.get("too.many.na.age.classes.message");
	}

	@Override
	public Collection<String> getInvolvedRowsIdsMessage() {
		return null;
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

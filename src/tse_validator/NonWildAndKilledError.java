package tse_validator;

import java.util.Arrays;
import java.util.Collection;

import i18n_messages.TSEMessages;
import report_validator.ReportError;

public class NonWildAndKilledError implements ReportError {

	private String progId;
	private String targetGroup;
	private String prodMethod;
	
	public NonWildAndKilledError(String progId, String targetGroup, String prodMethod) {
		this.progId = progId;
		this.targetGroup = targetGroup;
		this.prodMethod = prodMethod;
	}
	
	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		return TSEMessages.get("non.wild.and.killed.message");
	}

	@Override
	public Collection<String> getInvolvedRowsIdsMessage() {
		return Arrays.asList(progId);
	}

	@Override
	public String getSuggestions() {
		return null;
	}

	@Override
	public Collection<String> getErroneousValues() {
		return Arrays.asList(targetGroup, prodMethod);
	}

}

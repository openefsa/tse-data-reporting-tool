package tse_validator;

import java.util.Arrays;
import java.util.Collection;

import i18n_messages.TSEMessages;
import report_validator.ReportError;

public class WrongAgeClassError implements ReportError {

	private String rowId;
	private String ageClass;
	private int monthsFound;
	
	/**
	 * 
	 * @param rowId
	 * @param ageClass
	 * @param monthsFound
	 */
	public WrongAgeClassError(String rowId, String ageClass, int monthsFound) {
		this.rowId = rowId;
		this.ageClass = ageClass;
		this.monthsFound = monthsFound;
	}
	
	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		return TSEMessages.get("wrong.animage.message", 
				String.valueOf(monthsFound));
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
		return Arrays.asList(ageClass);
	}

}

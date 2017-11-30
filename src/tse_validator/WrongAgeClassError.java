package tse_validator;

import java.util.Arrays;
import java.util.Collection;

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
		return "Wrong animage range selected for " + monthsFound + " month-old animal";
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
		return Arrays.asList(ageClass);
	}

}

package tse_validator;

import java.util.Arrays;
import java.util.Collection;

import report_validator.ReportError;

public class InconsistentCasesError implements ReportError {

	private String row1;
	private String row2;
	
	private String idField;
	private String idFieldValue;
	private String field;
	private String value1;
	private String value2;
	
	/**
	 * 
	 * @param row1
	 * @param row2
	 * @param idField
	 * @param field
	 * @param value1
	 * @param value2
	 */
	public InconsistentCasesError(String row1, String row2, String idField, String idFieldValue, 
			String field, String value1, String value2) {
		this.row1 = row1;
		this.row2 = row2;
		this.idField = idField;
		this.idFieldValue = idFieldValue;
		this.field = field;
		this.value1 = value1;
		this.value2 = value2;
	}
	
	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		return "Cases with same " + idField + " (" + idFieldValue + ") but with different " + field;
	}

	@Override
	public Collection<String> getInvolvedRowsIdsMessage() {
		return Arrays.asList(row1, row2);
	}

	@Override
	public String getCorrectExample() {
		
		String example = "Put in both records";
		
		if (!value1.isEmpty() && !value2.isEmpty())
			example = example + " " + value1 + " or " + value2;
		else if (value1.isEmpty())
			example = example + " " + value2;
		else
			example = example + " " + value1;
		
		return  example + " for " + field + ", or change " + idField + " for one of them";
	}

	@Override
	public Collection<String> getErroneousValues() {
		return Arrays.asList(value1, value2);
	}

}

package tse_validator;

import java.util.Arrays;
import java.util.Collection;

import i18n_messages.TSEMessages;
import report_validator.ReportError;

public class WrongAllelesPairError implements ReportError {

	private String rowId;
	private String allele1;
	private String allele2;
	
	public WrongAllelesPairError(String rowId, String allele1, String allele2) {
		this.rowId = rowId;
		this.allele1 = allele1;
		this.allele2 = allele2;
	}
	
	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.WARNING;
	}

	@Override
	public String getErrorMessage() {
		return TSEMessages.get("wrong.alleles.pair.message");
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
		return Arrays.asList(allele1, allele2);
	}

}

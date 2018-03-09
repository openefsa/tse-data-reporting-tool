package tse_validator;

import java.util.Arrays;
import java.util.Collection;

import date_comparator.TseDate;
import i18n_messages.TSEMessages;
import report_validator.ReportError;

public class WrongAnalysisYearError implements ReportError {

	private String result;
	private TseDate analysisDate;
	private TseDate reportDate;
	
	public WrongAnalysisYearError(String result, TseDate analysisDate, TseDate reportDate) {
		this.result = result;
		this.analysisDate = analysisDate;
		this.reportDate = reportDate;
	}
	
	@Override
	public ErrorType getTypeOfError() {
		return ErrorType.ERROR;
	}

	@Override
	public String getErrorMessage() {
		String analysisY = String.valueOf(analysisDate.getYear());
		String repY = String.valueOf(reportDate.getYear());
		
		return TSEMessages.get("wrong.an.year.message", analysisY, repY);
	}

	@Override
	public Collection<String> getInvolvedRowsIdsMessage() {
		return Arrays.asList(result);
	}

	@Override
	public String getSuggestions() {
		return null;
	}

	@Override
	public Collection<String> getErroneousValues() {
		
		String analysisY = String.valueOf(analysisDate.getYear());
		
		return Arrays.asList(analysisY);
	}
}

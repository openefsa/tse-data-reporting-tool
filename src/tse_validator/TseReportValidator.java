package tse_validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import report_validator.ReportError;
import report_validator.ReportValidator;
import table_skeleton.TableColumn;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_report.TseReport;
import tse_validator.CaseReportValidator.Check;
import tse_validator.ResultValidator.ErrorType;
import tse_validator.SummarizedInfoValidator.SampleCheck;
import xlsx_reader.TableSchemaList;

public class TseReportValidator extends ReportValidator {

	private TseReport report;
	
	public TseReportValidator(TseReport report) {
		this.report = report;
	}
	
	@Override
	public Collection<ReportError> validate() {
		
		Collection<ReportError> errors = new ArrayList<>();
		
		errors.addAll(standardChecks());
		
		return errors;
	}
	
	public enum RowType {
		SUMM,
		CASE,
		RESULT
	}
	
	/**
	 * Get the type of the row
	 * @param row
	 * @return
	 */
	private RowType getRowType(TableRow row) {
		
		RowType type = null;
		
		switch(row.getSchema().getSheetName()) {
		case CustomStrings.SUMMARIZED_INFO_SHEET:
			type = RowType.SUMM;
			break;
		case CustomStrings.CASE_INFO_SHEET:
			type = RowType.CASE;
			break;
		case CustomStrings.RESULT_SHEET:
			type = RowType.RESULT;
			break;
		}
		
		return type;
	}
	
	/**
	 * Get the row id field of a row (not of db, the one defined by the domain)
	 * @param row
	 * @return
	 */
	private String getRowId(TableRow row) {
		
		String id = null;
		
		RowType type = getRowType(row);
		
		switch(type) {
		case SUMM:
			id = "Aggregated context ID: " + row.getCode(CustomStrings.SUMMARIZED_INFO_PROG_ID);
			break;
		case CASE:
			id = "Case sample ID: " + row.getCode(CustomStrings.CASE_INFO_SAMPLE_ID);
			break;
		case RESULT:
			id = "Result res ID: " + row.getCode(CustomStrings.RES_ID_COLUMN);
			break;
		default:
			id = "";
			break;
		}
		
		return id;
	}
	
	/**
	 * Standard checks that are already done at row level
	 * @return
	 */
	private Collection<ReportError> standardChecks() {
		
		Collection<ReportError> errors = new ArrayList<>();
		
		for (TableRow row : this.report.getAllRecords()) {

			String rowId = getRowId(row);
			
			// check mandatory fields
			for(TableColumn field : row.getMandatoryFieldNotFilled()) {
				errors.add(new MissingMandatoryFieldError(field.getLabel(), rowId));
			}

			RowType type = getRowType(row);
			
			// check results 
			if (type == RowType.RESULT) {
				ResultValidator validator = new ResultValidator();
				ErrorType errorType = validator.getError(row);
				
				switch(errorType) {
				case ALLELE_ERROR:
					errors.add(new AlleleNotReportableError(rowId, 
							row.getLabel(CustomStrings.RESULT_ALLELE_1) 
							+ " " + row.getLabel(CustomStrings.RESULT_ALLELE_2)));
					break;
				default:
					break;
				}
			}
			else if (type == RowType.CASE) {
				CaseReportValidator validator = new CaseReportValidator();
				try {
					
					Check check = validator.isRecordCorrect(row);
					
					switch(check) {
					case NO_TEST_SPECIFIED:
						
						TableRow caseReport = row.getParent(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));
						String parentId = getRowId(caseReport);
						errors.add(new NoTestSpecifiedError(parentId));
						break;
					default:
						break;
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if (type == RowType.SUMM) {
				
				SummarizedInfoValidator validator = new SummarizedInfoValidator();
				SampleCheck check = validator.isSampleCorrect(row);
				
				switch(check) {
				case CHECK_INC_CASES:
					errors.add(new CheckIncCasesError(rowId));
					break;
				case MISSING_CASES:
					errors.add(new MissingCasesError(rowId));
					break;
				case TOOMANY_CASES:
					errors.add(new TooManyCasesError(rowId));
					break;
				default:
					break;
				}
			}
		}
		
		return errors;
	}
}

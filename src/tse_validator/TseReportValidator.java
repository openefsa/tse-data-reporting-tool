package tse_validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import app_config.AppPaths;
import i18n_messages.TSEMessages;
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

/**
 * Validate an entire report and returns the errors
 * @author avonva
 *
 */
public class TseReportValidator extends ReportValidator {

	private TseReport report;
	
	/**	
	 * Validate an entire tse report and returns the errors
	 * in a list. It is also possible to show the list of
	 * errors by using the {@link #show(Collection)}
	 * method.
	 */
	public TseReportValidator(TseReport report) {
		this.report = report;
	}
	
	@Override
	public Collection<ReportError> validate() {
		
		Collection<ReportError> errors = new ArrayList<>();
		
		Collection<TableRow> reportRecords = this.report.getAllRecords();
		
		// check errors on single row (no interdependency is evaluated)
		for (TableRow row : reportRecords) {

			errors.addAll(checkMandatoryFields(row));
			
			RowType type = getRowType(row);

			if (type == RowType.RESULT) {
				errors.addAll(checkResult(row));
			}
			else if (type == RowType.CASE) {
				errors.addAll(checkCaseInfo(row));
			}
			else if (type == RowType.SUMM) {
				errors.addAll(checkSummarizedInfo(row));
			}
		}
		
		// check errors across different rows
		errors.addAll(checkDuplicatedSampleId(reportRecords));
		errors.addAll(checkNationalCaseId(reportRecords));
		errors.addAll(checkAnimalId(reportRecords));
		
		return errors;
	}
	
	/**
	 * Check if there are case reports with the same sample id
	 * @param reportRecords
	 * @return
	 */
	private Collection<ReportError> checkDuplicatedSampleId(Collection<TableRow> reportRecords) {

		Collection<ReportError> errors = new ArrayList<>();
		
		HashMap<String, TableRow> cases = new HashMap<>();
		for (TableRow row : reportRecords) {
			
			if (getRowType(row) != RowType.CASE)
				continue;
			
			String currentSampId = row.getLabel(CustomStrings.CASE_INFO_SAMPLE_ID);
			
			// if there is already an element, error! duplicated value
			if (cases.containsKey(currentSampId)) {
				TableRow conflict = cases.get(currentSampId);
				String rowId1 = getStackTrace(row);
				String rowId2 = getStackTrace(conflict);
				errors.add(new DuplicatedSampleIdError(rowId1, rowId2));
			}
			else {
				// otherwise standard insert
				cases.put(currentSampId, row);
			}
		}
		
		return errors;
	}
	
	private enum RowType {
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
			id = TSEMessages.get("si.id.label", 
					row.getCode(CustomStrings.SUMMARIZED_INFO_PROG_ID));
			break;
		case CASE:
			id = TSEMessages.get("case.id.label", 
					row.getCode(CustomStrings.CASE_INFO_SAMPLE_ID));
			break;
		case RESULT:
			id = TSEMessages.get("result.id.label", 
					row.getCode(CustomStrings.RES_ID_COLUMN));
			break;
		default:
			id = "";
			break;
		}
		
		return id;
	}
	
	private String getStackTrace(TableRow row) {
		
		RowType type = getRowType(row);
		
		String trace = null;
		
		final String arrowCharacter = TSEMessages.get("table.html.arrow"); // html arrow
		
		switch(type) {
		case SUMM:
			trace = getRowId(row);
			break;
		case CASE:
			TableRow summInfo = row.getParent(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));
			trace = getRowId(summInfo);
			trace = trace + arrowCharacter + getRowId(row);
			break;
		case RESULT:
			TableRow caseReport = row.getParent(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));
			TableRow summ = caseReport.getParent(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));
			trace = getRowId(summ);
			trace = trace + arrowCharacter + getRowId(caseReport);
			trace = trace + arrowCharacter + getRowId(row);
			break;
		default:
			trace = "";
			break;
		}
		
		return trace;
	}
	
	/**
	 * Check if the mandatory fields were filled
	 * @param row
	 * @return
	 */
	private Collection<ReportError> checkMandatoryFields(TableRow row) {
		
		Collection<ReportError> errors = new ArrayList<>();
		
		String rowId = getStackTrace(row);
		
		// check mandatory fields
		for(TableColumn field : row.getMandatoryFieldNotFilled()) {
			errors.add(new MissingMandatoryFieldError(field.getLabel(), rowId));
		}
		
		return errors;
	}
	
	/**
	 * Check errors in a single summarized information
	 * @param row
	 * @return
	 */
	private Collection<ReportError> checkSummarizedInfo(TableRow row) {
		
		Collection<ReportError> errors = new ArrayList<>();
		
		SummarizedInfoValidator validator = new SummarizedInfoValidator();
		SampleCheck check = validator.isSampleCorrect(row);
		String rowId = getStackTrace(row);
		
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
		case MISSING_RGT_CASE:
			errors.add(new MissingRGTCaseError(rowId));
			break;
		default:
			break;
		}
		
		return errors;
	}
	
	/**
	 * Check errors in a single case report
	 * @param row
	 * @return
	 */
	private Collection<ReportError> checkCaseInfo(TableRow row) {
		
		Collection<ReportError> errors = new ArrayList<>();

		CaseReportValidator validator = new CaseReportValidator();

		Check check = null;
		try {
			check = validator.isRecordCorrect(row);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (check != null) {
			switch(check) {
			case NO_TEST_SPECIFIED:
				errors.add(new NoTestSpecifiedError(getStackTrace(row)));
				break;
			default:
				break;
			}
		}
		
		errors.addAll(checkAgeClass(row));
		
		return errors;
	}
	

	private Collection<ReportError> checkNationalCaseId(Collection<TableRow> reportRecords) {

		String[] fieldsToCheck = {
				CustomStrings.CASE_INFO_ANIMAL_ID
			};
		
		return checkIdField(reportRecords, CustomStrings.CASE_INFO_CASE_ID, 
				TSEMessages.get("inconsistent.national.case.id"), fieldsToCheck);
	}
	
	private Collection<ReportError> checkAnimalId(Collection<TableRow> reportRecords) {

		String[] fieldsToCheck = {
				CustomStrings.CASE_INFO_CASE_ID,
				CustomStrings.CASE_INFO_HERD_ID,
				CustomStrings.CASE_INFO_STATUS,
				CustomStrings.CASE_INFO_HOLDING_ID,
				CustomStrings.RESULT_SAMP_DAY,
				CustomStrings.RESULT_SAMP_AREA,
				CustomStrings.CASE_INDEX_CASE,
				CustomStrings.CASE_BIRTH_COUNTRY,
				CustomStrings.CASE_INFO_BIRTH_YEAR,
				CustomStrings.CASE_INDEX_BIRTH_MONTH,
				CustomStrings.CASE_INFO_BORN_FLOCK,
				CustomStrings.CASE_INFO_BREED,
				CustomStrings.CASE_INFO_COMMENT
			};
		
		return checkIdField(reportRecords, CustomStrings.CASE_INFO_ANIMAL_ID, 
				TSEMessages.get("inconsistent.animal.id"), fieldsToCheck);
	}
	
	/**
	 * Check that two records with the same id have the same values in the other fields
	 * specified in the {@code fieldsToCheck} parameter.
	 * @param reportRecords
	 * @param idField
	 * @param idFieldLabel
	 * @param fieldsToCheck
	 * @return
	 */
	private Collection<ReportError> checkIdField(Collection<TableRow> reportRecords, 
			String idField, String idFieldLabel, String[] fieldsToCheck) {
		
		Collection<ReportError> errors = new ArrayList<>();

		HashMap<String, TableRow> cases = new HashMap<>();
		for (TableRow row : reportRecords) {

			if (getRowType(row) != RowType.CASE)
				continue;
			
			String id = row.getLabel(idField);
			
			if (id.isEmpty())
				continue;
			
			TableRow current = cases.get(id);
			
			// if one is already present, check if they are equal or not
			if (current != null) {
				
				// add all the mismatches
				for (String field : fieldsToCheck) {
					
					String currentValue = current.getCode(field);
					String currentValueLab = current.getLabel(field);
					String value = row.getCode(field);
					String label = row.getLabel(field);
					
					if (!value.equals(currentValue)) {
						errors.add(new InconsistentCasesError(getStackTrace(current), 
								getStackTrace(row), idFieldLabel, id, field, currentValueLab, label));
					}
				}
			}
			
			// save the new one
			cases.put(id, row);
		}

		return errors;
	}
	
	private Collection<ReportError> checkAgeClass(TableRow row) {
		
		Collection<ReportError> errors = new ArrayList<>();
		
		TableRow report = row.getParent(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		TableRow summInfo = row.getParent(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));
		String reportYear = report.getCode(AppPaths.REPORT_YEAR);
		String reportMonth = report.getCode(AppPaths.REPORT_MONTH);
		String reportMonthLabel = report.getLabel(AppPaths.REPORT_MONTH);
		String ageClass = summInfo.getCode(CustomStrings.SUMMARIZED_INFO_AGE);
		String ageClassLabel = summInfo.getLabel(CustomStrings.SUMMARIZED_INFO_AGE);
		String birthYear = row.getCode(CustomStrings.CASE_INFO_BIRTH_YEAR);
		String birthMonth = row.getCode(CustomStrings.CASE_INDEX_BIRTH_MONTH);
		String birthMonthLabel = row.getLabel(CustomStrings.CASE_INDEX_BIRTH_MONTH);
		
		if (birthYear.isEmpty() || birthMonth.isEmpty() || ageClass.isEmpty())
			return errors;
		
		AgeClassValidator ageValidator = new AgeClassValidator(
				ageClass, reportYear, reportMonth, birthYear, birthMonth
				);

		try {
			tse_validator.AgeClassValidator.Check check2 = ageValidator.validate();
			switch(check2) {
			case AGE_CLASS_NOT_RESPECTED:
				errors.add(new WrongAgeClassError(getStackTrace(row), ageClassLabel, 
						ageValidator.getMonthsDifference()));
				break;
			case REPORT_DATE_EXCEEDED:
				errors.add(new ReportDateExceededError(getStackTrace(row), reportYear, 
						reportMonthLabel, birthYear, birthMonthLabel));
				break;
			default:
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return errors;
	}
	
	/**
	 * Check errors in a single analytical result
	 * @param row
	 * @return
	 */
	private Collection<ReportError> checkResult(TableRow row) {
		
		Collection<ReportError> errors = new ArrayList<>();
		
		ResultValidator validator = new ResultValidator();
		ErrorType errorType = validator.getError(row);
		String rowId = getStackTrace(row);
		
		switch(errorType) {
		case ALLELE_ERROR:
			errors.add(new AlleleNotReportableError(rowId, 
					row.getLabel(CustomStrings.RESULT_ALLELE_1), 
					row.getLabel(CustomStrings.RESULT_ALLELE_2)));
			break;
		default:
			break;
		}
		
		return errors;
	}
}

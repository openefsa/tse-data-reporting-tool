package tse_validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import app_config.AppPaths;
import formula.FormulaException;
import i18n_messages.TSEMessages;
import providers.ITableDaoService;
import providers.TseReportService;
import report_validator.ReportError;
import report_validator.ReportValidator;
import table_relations.Relation;
import table_skeleton.TableCell;
import table_skeleton.TableColumn;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_report.TseReport;
import tse_validator.CaseReportValidator.Check;
import tse_validator.ResultValidator.ErrorType;
import tse_validator.SummarizedInfoValidator.SampleCheck;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

/**
 * Validate an entire report and returns the errors
 * @author avonva
 *
 */
public class TseReportValidator extends ReportValidator {

	private static final Logger LOGGER = LogManager.getLogger(TseReportValidator.class);
	
	private TseReport report;
	
	private TseReportService reportService;
	private ITableDaoService daoService;
	
	/**	
	 * Validate an entire tse report and returns the errors
	 * in a list. It is also possible to show the list of
	 * errors by using the {@link #show(Collection)}
	 * method.
	 */
	public TseReportValidator(TseReport report, TseReportService reportService, ITableDaoService daoService) {
		this.report = report;
		this.daoService = daoService;
		this.reportService = reportService;
	}
	
	@Override
	public Collection<ReportError> validate() {
		
		Collection<ReportError> errors = new ArrayList<>();
		
		Collection<TableRow> reportRecords = this.reportService.getAllRecords(report);

		if (reportRecords.isEmpty()) {
			errors.add(new EmptyReportError());
		}
		
		// check errors on single row (no interdependency is evaluated)
		for (TableRow row : reportRecords) {
			
			try {
				errors.addAll(checkMandatoryFields(row));
			} catch (FormulaException e) {
				e.printStackTrace();
			}
			
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
		errors.addAll(checkDuplicatedContext(reportRecords));
		errors.addAll(checkDuplicatedSampleId(reportRecords));
		errors.addAll(checkDuplicatedResId(reportRecords));
		errors.addAll(checkNationalCaseId(reportRecords));
		errors.addAll(checkAnimalId(reportRecords));
		errors.addAll(checkUnknownAgeClass(reportRecords));
		
		return errors;
	}
	
	
	/**
	 * Check if there are case reports with the same sample id
	 * @param reportRecords
	 * @return
	 */
	public Collection<ReportError> checkDuplicatedSampleId(Collection<TableRow> reportRecords) {

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
	
	/**
	 * Check if there are analytical results with the same result id
	 * @param reportRecords
	 * @return
	 */
	public Collection<ReportError> checkDuplicatedResId(Collection<TableRow> reportRecords) {

		Collection<ReportError> errors = new ArrayList<>();
		
		HashMap<String, TableRow> results = new HashMap<>();
		for (TableRow row : reportRecords) {
			
			if (getRowType(row) != RowType.RESULT)
				continue;
			
			String currentId = row.getLabel(CustomStrings.RES_ID_COLUMN);
			
			// if there is already an element, error! duplicated value
			if (results.containsKey(currentId)) {
				TableRow conflict = results.get(currentId);
				String rowId1 = getStackTrace(row);
				String rowId2 = getStackTrace(conflict);
				errors.add(new DuplicatedResultIdError(rowId1, rowId2));
			}
			else {
				// otherwise standard insert
				results.put(currentId, row);
			}
		}
		
		return errors;
	}
	
	public Collection<ReportError> checkDuplicatedContext(Collection<TableRow> reportRecords) {
		
		Collection<ReportError> errors = new ArrayList<>();
		
		HashMap<String, TableRow> summInfos = new HashMap<>();
		for (TableRow row: reportRecords) {
			
			if (getRowType(row) != RowType.SUMM)
				continue;
			String id = row.getLabel(CustomStrings.CONTEXT_ID_COL);
			if (summInfos.containsKey(id)) {
				TableRow conflict = summInfos.get(id);
				String rowId1 = getStackTrace(row);
				String rowId2 = getStackTrace(conflict);
				errors.add(new DuplicatedContextError(rowId1, rowId2));
			}
			else {
				// otherwise standard insert
				summInfos.put(id, row);
			}
		}
		
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
	public RowType getRowType(TableRow row) {
		
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
	public String getRowId(TableRow row) {
		
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
	
	public String getStackTrace(TableRow row) {
		
		RowType type = getRowType(row);
		
		String trace = null;
		
		final String arrowCharacter = TSEMessages.get("table.html.arrow"); // html arrow
		
		switch(type) {
		case SUMM:
			trace = getRowId(row);
			break;
		case CASE:
			
			int parentId = Integer.valueOf(row.getCode(Relation.foreignKeyFromParent(CustomStrings.SUMMARIZED_INFO_SHEET)));
			
			TableRow summInfo = daoService.getById(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET), parentId);

			trace = getRowId(summInfo);
			trace = trace + arrowCharacter + getRowId(row);
			break;
		case RESULT:
			
			
			int summParentId = Integer.valueOf(row.getCode(Relation.foreignKeyFromParent(CustomStrings.SUMMARIZED_INFO_SHEET)));
			int caseParentId = Integer.valueOf(row.getCode(Relation.foreignKeyFromParent(CustomStrings.CASE_INFO_SHEET)));
			
			TableRow summ = daoService.getById(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET), summParentId);
			TableRow caseReport = daoService.getById(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET), caseParentId);

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
	 * @throws FormulaException 
	 */
	public Collection<ReportError> checkMandatoryFields(TableRow row) throws FormulaException {
		
		Collection<ReportError> errors = new ArrayList<>();
		
		String rowId = getStackTrace(row);
		
		// check mandatory fields
		for(TableColumn field : reportService.getMandatoryFieldNotFilled(row)) {

			String label = field.getLabel();
			
			String fieldName = label == null || label.isEmpty() ? field.getId() : label;

			errors.add(new MissingMandatoryFieldError(fieldName, rowId));
		}
		
		return errors;
	}
	
	/**
	 * Check errors in a single summarized information
	 * @param row
	 * @return
	 */
	public Collection<ReportError> checkSummarizedInfo(TableRow row) {
		
		Collection<ReportError> errors = new ArrayList<>();
		
		SummarizedInfoValidator validator = new SummarizedInfoValidator(daoService);
		Collection<SampleCheck> checks = validator.isSampleCorrect(row);
		String rowId = getStackTrace(row);
		
		for (SampleCheck check: checks) {
			switch(check) {
			case TOO_MANY_INCONCLUSIVES:
				errors.add(new TooManyIncCasesError(rowId));
				break;
			case MISSING_RGT_CASE:
				errors.add(new MissingRGTCaseError(rowId));
				break;
			case TOO_MANY_POSITIVES:
				errors.add(new TooManyPositivesError(rowId));
				break;
			case NON_WILD_FOR_KILLED:
				
				TableSchema schema = TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET);
				
				String targetLabel = schema.getById(CustomStrings.SUMMARIZED_INFO_TARGET_GROUP).getLabel();
				String prodLabel = schema.getById(CustomStrings.SUMMARIZED_INFO_PROD).getLabel();
				
				String id = getStackTrace(row);
				TableCell targetGroup = row.get(CustomStrings.SUMMARIZED_INFO_TARGET_GROUP);
				TableCell prod = row.get(CustomStrings.SUMMARIZED_INFO_PROD);
				
				errors.add(new NonWildAndKilledError(id, targetLabel + ": " + targetGroup.getLabel(), 
						prodLabel + ": " + prod.getLabel()));
				
				break;
			default:
				break;
			}
		}
		
		// check declared cases
		if (!row.getCode(CustomStrings.SUMMARIZED_INFO_TYPE)
				.equals(CustomStrings.SUMMARIZED_INFO_RGT_TYPE)) {
			
			// if not RGT type, then check declared cases
			
			String total = row.getLabel(CustomStrings.SUMMARIZED_INFO_TOT_SAMPLES);
			String unsuitable = row.getLabel(CustomStrings.SUMMARIZED_INFO_UNS_SAMPLES);
			
			// if no declared case, then show error
			if (total.equals("0") && unsuitable.equals("0")) {
				errors.add(new NoCaseDeclaredError(getRowId(row)));
			}
		}
		
		return errors;
	}
	
	/**
	 * Check errors in a single case report
	 * @param row
	 * @return
	 */
	public Collection<ReportError> checkCaseInfo(TableRow row) {
		
		Collection<ReportError> errors = new ArrayList<>();

		CaseReportValidator validator = new CaseReportValidator(daoService);

		Collection<Check> checks = new ArrayList<>();
		try {
			checks = validator.isRecordCorrect(row);
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Cannot check if case is correct", e);
		}

		for (Check check: checks) {
			switch(check) {
			case NO_TEST_SPECIFIED:
				errors.add(new NoTestSpecifiedError(getStackTrace(row)));
				break;
			case DUPLICATED_TEST:
				errors.add(new DuplicatedTestError(getStackTrace(row)));
				break;
			case CASE_ID_FOR_NEGATIVE:
				errors.add(new CaseIdForNegativeError(getStackTrace(row)));
				break;
			case INDEX_CASE_FOR_NEGATIVE:
				errors.add(new IndexCaseForNegativeError(getStackTrace(row)));
				break;
			case INDEX_CASE_FOR_FARMED_CWD:
				errors.add(new IndexCaseForFarmedCwd(getStackTrace(row)));
				break;
			case EM_FOR_NOT_INFECTED:
				errors.add(new NotInfectedStatusForEradicationError(getStackTrace(row)));
				break;
			default:
				break;
			}
		}
		
		errors.addAll(checkAgeClass(row));
		
		return errors;
	}
	

	public Collection<ReportError> checkNationalCaseId(Collection<TableRow> reportRecords) {

		String[] fieldsToCheck = {
				CustomStrings.CASE_INFO_ANIMAL_ID
			};
		
		return checkIdField(reportRecords, CustomStrings.CASE_INFO_CASE_ID, 
				TSEMessages.get("inconsistent.national.case.id"), fieldsToCheck);
	}
	
	public Collection<ReportError> checkAnimalId(Collection<TableRow> reportRecords) {

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
				CustomStrings.CASE_INFO_BIRTH_MONTH,
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
	public Collection<ReportError> checkIdField(Collection<TableRow> reportRecords, 
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
	
	public Collection<ReportError> checkAgeClass(TableRow row) {
		
		Collection<ReportError> errors = new ArrayList<>();
		
		int reportId = row.getNumCode(Relation.foreignKeyFromParent(CustomStrings.REPORT_SHEET));
		TableRow report = daoService.getById(TableSchemaList.getByName(CustomStrings.REPORT_SHEET), reportId);

		int summId = row.getNumCode(Relation.foreignKeyFromParent(CustomStrings.SUMMARIZED_INFO_SHEET));
		TableRow summInfo = daoService.getById(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET), summId);

		String reportYear = report.getCode(AppPaths.REPORT_YEAR);
		String reportMonth = report.getCode(AppPaths.REPORT_MONTH);
		String reportMonthLabel = report.getLabel(AppPaths.REPORT_MONTH);
		String ageClass = summInfo.getCode(CustomStrings.SUMMARIZED_INFO_AGE);
		String ageClassLabel = summInfo.getLabel(CustomStrings.SUMMARIZED_INFO_AGE);
		String birthYear = row.getCode(CustomStrings.CASE_INFO_BIRTH_YEAR);
		String birthMonth = row.getCode(CustomStrings.CASE_INFO_BIRTH_MONTH);
		String birthMonthLabel = row.getLabel(CustomStrings.CASE_INFO_BIRTH_MONTH);
		
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
			LOGGER.error("Cannot check age class", e);
		}
		
		return errors;
	}
	
	/**
	 * Check errors in a single analytical result
	 * @param row
	 * @return
	 */
	public Collection<ReportError> checkResult(TableRow row) {
		
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
		case WRONG_ALLELE_PAIR:
			errors.add(new WrongAllelesPairError(rowId, 
					row.getLabel(CustomStrings.RESULT_ALLELE_1), 
					row.getLabel(CustomStrings.RESULT_ALLELE_2)));
			break;
		default:
			break;
		}
		
		return errors;
	}
	
	public Collection<ReportError> checkUnknownAgeClass(Collection<TableRow> rows) {
		
		int total = 0;
		int unk = 0;
		
		Collection<ReportError> errors = new ArrayList<>();
		for (TableRow row: rows) {
			
			// only for BSE summ info
			if (getRowType(row) == RowType.SUMM && row.getCode(CustomStrings.SUMMARIZED_INFO_TYPE)
					.equals(CustomStrings.SUMMARIZED_INFO_BSE_TYPE)) {
				
				int totalSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_TOT_SAMPLES);
				total += totalSamples;
				
				// if unknown age class
				if (row.getCode(CustomStrings.SUMMARIZED_INFO_AGE)
						.equals(CustomStrings.UNKNOWN_AGE_CLASS_CODE)) {
					unk += totalSamples;
				}
			}
		}
		
		// if unknown is bigger than 5%
		if (unk * 100.000 / total > 5) {
			errors.add(new TooManyUnknownAgeClassesError());
		}
		
		return errors;
	}
}

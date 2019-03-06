package tse_validator;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app_config.AppPaths;
import date_comparator.TseDate;
import formula.FormulaException;
import i18n_messages.TSEMessages;
import providers.ITableDaoService;
import providers.TseReportService;
import providers.TseReportService.RowType;
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
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class TseReportValidator extends ReportValidator {

	private static final Logger LOGGER = LogManager.getLogger(TseReportValidator.class);

	private TseReport report;

	private TseReportService reportService;
	private ITableDaoService daoService;

	/**
	 * Validate an entire tse report and returns the errors in a list. It is also
	 * possible to show the list of errors by using the {@link #show(Collection)}
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

		ArrayList<TableRow> reportRecords = this.reportService.getAllRecords(report);

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

			RowType type = TseReportService.getRowType(row);

			if (type == RowType.RESULT) {
				errors.addAll(checkResult(row));
			} else if (type == RowType.CASE) {
				errors.addAll(checkCaseInfo(row));
			} else if (type == RowType.SUMM) {
				errors.addAll(checkSummarizedInfo(row));
			}
		}

		// check errors across different rows
		errors.addAll(checkDuplicatedSummId(reportRecords));
		errors.addAll(checkDuplicatedSampleId(reportRecords));
		errors.addAll(checkDuplicatedResId(reportRecords));
		errors.addAll(checkNationalCaseId(reportRecords));
		errors.addAll(checkAnimalId(reportRecords));
		errors.addAll(checkUnknownAgeClass(reportRecords));

		return errors;
	}

	/**
	 * Check if there are case reports with the same sample id
	 * 
	 * @param reportRecords
	 * @return
	 */
	public Collection<ReportError> checkDuplicatedSampleId(Collection<TableRow> reportRecords) {

		Collection<ReportError> errors = new ArrayList<>();

		HashMap<String, TableRow> cases = new HashMap<>();
		for (TableRow row : reportRecords) {

			if (TseReportService.getRowType(row) != RowType.CASE)
				continue;

			String currentSampId = row.getLabel(CustomStrings.SAMPLE_ID_COL);

			// if there is already an element, error! duplicated value
			if (cases.containsKey(currentSampId)) {
				TableRow conflict = cases.get(currentSampId);
				String rowId1 = getStackTrace(row);
				String rowId2 = getStackTrace(conflict);
				errors.add(new DuplicatedSampleIdError(rowId1, rowId2));
			} else {
				// otherwise standard insert
				cases.put(currentSampId, row);
			}
		}

		return errors;
	}

	/**
	 * Check if there are analytical results with the same result id
	 * 
	 * @param reportRecords
	 * @return
	 */
	public Collection<ReportError> checkDuplicatedResId(Collection<TableRow> reportRecords) {

		Collection<ReportError> errors = new ArrayList<>();

		HashMap<String, TableRow> results = new HashMap<>();
		for (TableRow row : reportRecords) {

			if (TseReportService.getRowType(row) != RowType.RESULT)
				continue;

			String currentId = row.getLabel(CustomStrings.RES_ID_COL);

			// if there is already an element, error! duplicated value
			if (results.containsKey(currentId)) {
				TableRow conflict = results.get(currentId);
				String rowId1 = getStackTrace(row);
				String rowId2 = getStackTrace(conflict);
				errors.add(new DuplicatedResultIdError(rowId1, rowId2));
			} else {
				// otherwise standard insert
				results.put(currentId, row);
			}
		}

		return errors;
	}
	
	/**
	 * Check if there are summarize results with the same values in natural key
	 * 
	 * @author shahaal
	 * @param reportRecords
	 * @return
	 */
	public Collection<ReportError> checkDuplicatedSummId(ArrayList<TableRow> reportRecords) {
		
		//collection used to report duplicate errors
		Collection<ReportError> errors = new ArrayList<>();
		
		//the set store the values already analyzed
		Set<Integer> indexComputed = new HashSet<>();
		
		for(int i=0; i<reportRecords.size(); i++) {
			
			TableRow current = reportRecords.get(i);
			
			//if the row is not summ or already computed skip
			if(TseReportService.getRowType(current) != RowType.SUMM ||
					indexComputed.contains(i))
				continue;
			
			for(int j=0; j<reportRecords.size();j++) {

				TableRow next = reportRecords.get(j);
				
				//if the row is not summ or already computed or same as the current skip 
				if(i==j||TseReportService.getRowType(next) != RowType.SUMM||
						indexComputed.contains(j)) 
					continue;
				
				// if the records has same values under natural key
				if(current.sameAs(next)) {
					
					// get their ids
					String rowId1 = getStackTrace(current);
					String rowId2 = getStackTrace(next);
					//add their indexes to the set
					indexComputed.add(j);
					indexComputed.add(i);
					//add the error to print
					errors.add(new DuplicatedContextError(rowId1, rowId2));
				}
			}
		}

		return errors;
	}

	/**
	 * Get the row id field of a row (not of db, the one defined by the domain)
	 * 
	 * @param row
	 * @return
	 */
	public static String getRowId(TableRow row) {

		String id = null;

		RowType type = TseReportService.getRowType(row);

		switch (type) {
		case SUMM:
			id = TSEMessages.get("si.id.label", row.getCode(CustomStrings.PROG_ID_COL));
			break;
		case CASE:
			id = TSEMessages.get("case.id.label", row.getCode(CustomStrings.SAMPLE_ID_COL));
			break;
		case RESULT:
			id = TSEMessages.get("result.id.label", row.getCode(CustomStrings.RES_ID_COL));
			break;
		default:
			id = "";
			break;
		}

		return id;
	}

	public String getStackTrace(TableRow row) {

		RowType type = TseReportService.getRowType(row);

		String trace = null;

		final String arrowCharacter = TSEMessages.get("table.html.arrow"); // html arrow

		switch (type) {
		case SUMM:
			trace = getRowId(row);
			break;
		case CASE:

			int parentId = Integer
					.valueOf(row.getCode(Relation.foreignKeyFromParent(CustomStrings.SUMMARIZED_INFO_SHEET)));

			TableRow summInfo = daoService.getById(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET),
					parentId);

			trace = getRowId(summInfo);
			trace = trace + arrowCharacter + getRowId(row);
			break;
		case RESULT:

			int summParentId = Integer
					.valueOf(row.getCode(Relation.foreignKeyFromParent(CustomStrings.SUMMARIZED_INFO_SHEET)));
			int caseParentId = Integer
					.valueOf(row.getCode(Relation.foreignKeyFromParent(CustomStrings.CASE_INFO_SHEET)));

			TableRow summ = daoService.getById(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET),
					summParentId);
			TableRow caseReport = daoService.getById(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET),
					caseParentId);

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
	 * 
	 * @param row
	 * @return
	 * @throws FormulaException
	 */
	public Collection<ReportError> checkMandatoryFields(TableRow row) throws FormulaException {

		Collection<ReportError> errors = new ArrayList<>();

		String rowId = getStackTrace(row);

		// check mandatory fields
		for (TableColumn field : reportService.getMandatoryFieldNotFilled(row)) {

			String label = field.getLabel();

			String fieldName = label == null || label.isEmpty() ? field.getId() : label;

			errors.add(new MissingMandatoryFieldError(fieldName, rowId));
		}

		return errors;
	}

	/**
	 * Check errors in a single summarized information
	 * 
	 * @param row
	 * @return
	 */
	public Collection<ReportError> checkSummarizedInfo(TableRow row) {

		Collection<ReportError> errors = new ArrayList<>();

		SummarizedInfoValidator validator = new SummarizedInfoValidator(daoService);
		Collection<SampleCheck> checks = validator.isSampleCorrect(row);
		String rowId = getStackTrace(row);

		for (SampleCheck check : checks) {
			switch (check) {
			case TOO_MANY_INCONCLUSIVES:
				errors.add(new CheckInconclusiveCasesError(rowId));
				break;
			case MISSING_RGT_CASE:
				errors.add(new MissingRGTCaseError(rowId));
				break;
			case TOO_MANY_POSITIVES:
				errors.add(new CheckPositiveCasesError(rowId));
				break;
			case NON_WILD_FOR_KILLED:

				TableSchema schema = TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET);

				String targetLabel = schema.getById(CustomStrings.TARGET_GROUP_COL).getLabel();
				String prodLabel = schema.getById(CustomStrings.PROD_COL).getLabel();

				String id = getStackTrace(row);
				TableCell targetGroup = row.get(CustomStrings.TARGET_GROUP_COL);
				TableCell prod = row.get(CustomStrings.PROD_COL);

				errors.add(new NonWildAndKilledError(id, targetLabel + ": " + targetGroup.getLabel(),
						prodLabel + ": " + prod.getLabel()));

				break;
			default:
				break;
			}
		}

		// check declared cases
		if (!row.getCode(CustomStrings.SUMMARIZED_INFO_TYPE).equals(CustomStrings.SUMMARIZED_INFO_RGT_TYPE)) {

			// if not RGT type, then check declared cases

			String total = row.getLabel(CustomStrings.TOT_SAMPLE_TESTED_COL);
			String unsuitable = row.getLabel(CustomStrings.TOT_SAMPLE_UNSUITABLE_COL);

			// if no declared case, then show error
			if (total.equals("0") && unsuitable.equals("0")) {
				errors.add(new NoCaseDeclaredError(getRowId(row)));
			}
		}

		return errors;
	}

	/**
	 * Check errors in a single case report
	 * 
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

		for (Check check : checks) {
			switch (check) {
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
			case INDEX_CASE_FOR_INFECTED:
			case NOT_INDEX_CASE_FOR_FREE:
				errors.add(new IndexCaseInconsistentWithStatusHerdError(getStackTrace(row),
						row.getLabel(CustomStrings.INDEX_CASE_COL), row.getLabel(CustomStrings.STATUS_HERD_COL)));
				break;
			case NOT_CONSTANT_ANALYSIS_YEAR:
				errors.add(new NotConstantAnalysisYearError(getStackTrace(row)));
				break;
			default:
				break;
			}
		}

		try {
			errors.addAll(checkAgeClass(row));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return errors;
	}

	public Collection<ReportError> checkNationalCaseId(Collection<TableRow> reportRecords) {

		String[] fieldsToCheck = { CustomStrings.ANIMAL_ID_COL };

		return checkIdField(reportRecords, CustomStrings.NATIONAL_CASE_ID_COL,
				TSEMessages.get("inconsistent.national.case.id"), fieldsToCheck);
	}

	public Collection<ReportError> checkAnimalId(Collection<TableRow> reportRecords) {

		String[] fieldsToCheck = { CustomStrings.NATIONAL_CASE_ID_COL, CustomStrings.HERD_ID_COL,
				CustomStrings.STATUS_HERD_COL, CustomStrings.SAMP_HOLDING_ID_COL, CustomStrings.SAMP_DAY_COL,
				CustomStrings.SAMP_AREA_COL, CustomStrings.INDEX_CASE_COL, CustomStrings.BIRTH_COUNTRY_COL,
				CustomStrings.BIRTH_YEAR_COL, CustomStrings.BIRTH_MONTH_COL, CustomStrings.BORN_FLOCK_HERD_COL,
				CustomStrings.BREED_COL, CustomStrings.EVAL_COMMENT_COL };

		return checkIdField(reportRecords, CustomStrings.ANIMAL_ID_COL, TSEMessages.get("inconsistent.animal.id"),
				fieldsToCheck);
	}

	/**
	 * Check that two records with the same id have the same values in the other
	 * fields specified in the {@code fieldsToCheck} parameter.
	 * 
	 * @param reportRecords
	 * @param idField
	 * @param idFieldLabel
	 * @param fieldsToCheck
	 * @return
	 */
	public Collection<ReportError> checkIdField(Collection<TableRow> reportRecords, String idField, String idFieldLabel,
			String[] fieldsToCheck) {

		Collection<ReportError> errors = new ArrayList<>();

		HashMap<String, TableRow> cases = new HashMap<>();
		for (TableRow row : reportRecords) {

			if (TseReportService.getRowType(row) != RowType.CASE)
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
						errors.add(new InconsistentCasesError(getStackTrace(current), getStackTrace(row), idFieldLabel,
								id, field, currentValueLab, label));
					}
				}
			}

			// save the new one
			cases.put(id, row);
		}

		return errors;
	}

	@SuppressWarnings("unused")
	public Collection<ReportError> checkAgeClass(TableRow row) throws ParseException {

		Collection<ReportError> errors = new ArrayList<>();

		int reportId = row.getNumCode(Relation.foreignKeyFromParent(CustomStrings.REPORT_SHEET));
		TableRow report1 = daoService.getById(TableSchemaList.getByName(CustomStrings.REPORT_SHEET), reportId);

		int summId = row.getNumCode(Relation.foreignKeyFromParent(CustomStrings.SUMMARIZED_INFO_SHEET));
		TableRow summInfo = daoService.getById(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET), summId);

		String reportYear = report1.getCode(AppPaths.REPORT_YEAR_COL);
		String reportMonth = report1.getCode(AppPaths.REPORT_MONTH_COL);
		String reportMonthLabel = report1.getLabel(AppPaths.REPORT_MONTH_COL);

		String ageClass;
		String ageClassLabel;

		// for RGT age class is defined at case level
		if (summInfo.getCode(CustomStrings.SUMMARIZED_INFO_TYPE).equals(CustomStrings.SUMMARIZED_INFO_RGT_TYPE)) {
			ageClass = row.getCode(CustomStrings.ANIMAGE_COL);
			ageClassLabel = row.getLabel(CustomStrings.ANIMAGE_COL);
		} else { // otherwise aggregated level
			ageClass = summInfo.getCode(CustomStrings.ANIMAGE_COL);
			ageClassLabel = summInfo.getLabel(CustomStrings.ANIMAGE_COL);
		}

		String birthYear = row.getCode(CustomStrings.BIRTH_YEAR_COL);
		String birthMonth = row.getCode(CustomStrings.BIRTH_MONTH_COL);
		String birthMonthLabel = row.getLabel(CustomStrings.BIRTH_MONTH_COL);

		if (birthYear.isEmpty() || birthMonth.isEmpty() || ageClass.isEmpty())
			return errors;

		AgeClassValidator ageValidator = new AgeClassValidator(ageClass, reportYear, reportMonth, birthYear,
				birthMonth);

		try {
			tse_validator.AgeClassValidator.Check check2 = ageValidator.validate();
			switch (check2) {
			case AGE_CLASS_NOT_RESPECTED:
				errors.add(
						new WrongAgeClassError(getStackTrace(row), ageClassLabel, ageValidator.getMonthsDifference()));
				break;
			case REPORT_DATE_EXCEEDED:
				errors.add(new ReportDateExceededError(getStackTrace(row), reportYear, reportMonthLabel, birthYear,
						birthMonthLabel));
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
	 * 
	 * @param row
	 * @return
	 */
	public Collection<ReportError> checkResult(TableRow row) {

		Collection<ReportError> errors = new ArrayList<>();
		
		ErrorType errorType = ResultValidator.getError(row);
		String rowId = getStackTrace(row);

		switch (errorType) {
		case ALLELE_ERROR:
			errors.add(new AlleleNotReportableError(rowId, row.getLabel(CustomStrings.ALLELE_1_COL),
					row.getLabel(CustomStrings.ALLELE_2_COL)));
			break;
		case WRONG_ALLELE_PAIR:
			errors.add(new WrongAllelesPairError(rowId, row.getLabel(CustomStrings.ALLELE_1_COL),
					row.getLabel(CustomStrings.ALLELE_2_COL)));
			break;
		default:
			break;
		}

		// check analysis year (must be >= than report year)
		try {
			TseDate analysisDate = new TseDate(row.getCode(CustomStrings.ANALYSIS_Y_COL), "0");
			TseDate reportDate = new TseDate(report.getYear(), "0");

			if (analysisDate.compareTo(reportDate) < 0)
				errors.add(new WrongAnalysisYearError(rowId, analysisDate, reportDate));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		return errors;
	}

	public static Collection<ReportError> checkUnknownAgeClass(Collection<TableRow> rows) {

		int total = 0;
		int unk = 0;

		Collection<ReportError> errors = new ArrayList<>();
		for (TableRow row : rows) {

			// only for BSE summ info
			if (TseReportService.getRowType(row) == RowType.SUMM
					&& row.getCode(CustomStrings.SUMMARIZED_INFO_TYPE).equals(CustomStrings.SUMMARIZED_INFO_BSE_TYPE)) {

				int totalSamples = row.getNumLabel(CustomStrings.TOT_SAMPLE_TESTED_COL);
				total += totalSamples;

				// if unknown age class
				if (row.getCode(CustomStrings.ANIMAGE_COL).equals(CustomStrings.UNKNOWN_AGE_CLASS_CODE)) {
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

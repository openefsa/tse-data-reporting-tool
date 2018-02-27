package tse_validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import dataset.IDataset;
import formula.FormulaException;
import mocks.RowCreatorMock;
import mocks.TableDaoMock;
import providers.FormulaService;
import providers.IFormulaService;
import providers.ITableDaoService;
import providers.TableDaoService;
import providers.TseReportService;
import report_validator.ReportError;
import soap_test.GetAckMock;
import soap_test.GetDatasetMock;
import soap_test.GetDatasetsListMock;
import soap_test.SendMessageMock;
import table_skeleton.TableCell;
import table_skeleton.TableRow;
import tse_analytical_result.AnalyticalResult;
import tse_case_report.CaseReport;
import tse_config.CustomStrings;
import tse_report.TseReport;
import tse_summarized_information.SummarizedInfo;
import tse_validator.CaseReportValidator.Check;
import tse_validator.ResultValidator.ErrorType;
import tse_validator.SummarizedInfoValidator.SampleCheck;
import xlsx_reader.TableHeaders.XlsxHeader;

public class ReportValidatorTest {

	private TseReport report;
	private SummarizedInfo si;
	private CaseReport cr;
	private AnalyticalResult result;
	
	private TableRow pref;
	private TableRow opt;
	
	private TseReportService reportService;
	private GetAckMock getAck;
	private GetDatasetsListMock<IDataset> getDatasetsList;
	private SendMessageMock sendMessage;
	private GetDatasetMock getDataset;
	private ITableDaoService daoService;
	private IFormulaService formulaService;
	
	@Before
	public void init() {
		
		this.getAck = new GetAckMock();
		this.getDatasetsList = new GetDatasetsListMock<>();
		this.sendMessage = new SendMessageMock();
		this.getDataset = new GetDatasetMock();
		this.daoService = new TableDaoService(new TableDaoMock());
		
		this.formulaService = new FormulaService(daoService);
		
		this.reportService = new TseReportService(getAck, getDatasetsList, sendMessage, getDataset,
				daoService, formulaService);
		
		pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		opt = RowCreatorMock.genRandSettings();
		int optId = daoService.add(opt);
		
		report = RowCreatorMock.genRandReport(prefId);
		report.setYear("2005");
		report.setMonth("5");
		
		int reportId = daoService.add(report);
		
		si = RowCreatorMock.genRandSummInfo(reportId, optId, prefId);
		si.put(CustomStrings.SUMMARIZED_INFO_AGE, new TableCell("F31.A16NK", ""));  // < 24 months
		
		int siId = daoService.add(si);
		
		cr = RowCreatorMock.genRandCase(reportId, siId, optId, siId);
		cr.put(CustomStrings.CASE_INFO_BIRTH_MONTH, "6");
		cr.put(CustomStrings.CASE_INFO_BIRTH_YEAR, "2004");
		
		int caseId = daoService.add(cr);
		
		result = RowCreatorMock.genRandResult(reportId, siId, caseId, optId, prefId);
		daoService.add(result);
	}

	@Test
	public void checkMandatoryFieldsForSummarizedInformation() throws FormulaException {
		
		si.remove(CustomStrings.EM_TARGET_GROUP);

		assertFalse(reportService.getMandatoryFieldNotFilled(si).isEmpty());
	}
	
	@Test
	public void checkMandatoryFieldsForCaseInfo() throws FormulaException {
		
		cr.remove(CustomStrings.CASE_INFO_SAMPLE_ID);

		assertFalse(reportService.getMandatoryFieldNotFilled(cr).isEmpty());
	}
	
	@Test
	public void checkMandatoryFieldsForResult() throws FormulaException {
		
		result.remove(CustomStrings.RESULT_TEST_TYPE);
		
		assertFalse(reportService.getMandatoryFieldNotFilled(result).isEmpty());
	}
	
	@Test
	public void correctPositiveNumberWithSameCaseId() {
		
		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, 
				new TableCell(CustomStrings.SUMMARIZED_INFO_BSE_TYPE, ""));
		
		// one declared, two detailed
		si.put(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES, "1");
		
		cr.put(CustomStrings.CASE_INFO_ASSESS, 
				new TableCell(CustomStrings.DEFAULT_ASSESS_CBSE_CASE_CODE, ""));
		
		// create a second positive case
		CaseReport cr2 = new CaseReport();
		cr2.copyValues(cr);
		
		daoService.add(cr2);
		
		SummarizedInfoValidator validator = new SummarizedInfoValidator(daoService);
		Collection<SampleCheck> checks = validator.isSampleCorrect(si);
		
		assertTrue(checks.contains(SampleCheck.TOO_MANY_POSITIVES));
	}

	@Test
	public void declaredPositiveLessThanDetailedCheck() {
		
		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, 
				new TableCell(CustomStrings.SUMMARIZED_INFO_CWD_TYPE, ""));
		
		// one declared, two detailed
		si.put(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES, "1");
		
		cr.put(CustomStrings.CASE_INFO_ASSESS, 
				new TableCell(CustomStrings.DEFAULT_ASSESS_CBSE_CASE_CODE, ""));
		
		cr.put(CustomStrings.CASE_INFO_CASE_ID, "caseXdofndofj");
		
		// create a second positive case
		CaseReport cr2 = new CaseReport();
		cr2.copyValues(cr);
		cr2.put(CustomStrings.CASE_INFO_CASE_ID, "dsadsadijaidjsoiajdoisjadsa");
		
		daoService.add(cr2);
		
		SummarizedInfoValidator validator = new SummarizedInfoValidator(daoService);
		Collection<SampleCheck> checks = validator.isSampleCorrect(si);
		
		assertTrue(checks.contains(SampleCheck.TOO_MANY_POSITIVES));
	}
	
	@Test
	public void declaredInconclusiveLessThanDetailedCheck() {

		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, 
				new TableCell(CustomStrings.SUMMARIZED_INFO_BSE_TYPE, ""));
		
		si.put(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES, "1");
		cr.put(CustomStrings.CASE_INFO_ASSESS, 
				new TableCell(CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE, ""));
		
		// another inc report
		CaseReport cr2 = new CaseReport();
		cr2.copyValues(cr);
		cr2.put(CustomStrings.CASE_INFO_CASE_ID, "fkdsajkdjsakdjskajd");
		daoService.add(cr2);
		
		SummarizedInfoValidator validator = new SummarizedInfoValidator(daoService);
		Collection<SampleCheck> checks = validator.isSampleCorrect(si);
		
		assertTrue(checks.contains(SampleCheck.TOO_MANY_INCONCLUSIVES));
	}
	
	@Test
	public void correctInconclusiveNumberWithSameCaseId() {

		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, 
				new TableCell(CustomStrings.SUMMARIZED_INFO_BSE_TYPE, ""));
		
		si.put(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES, "1");
		cr.put(CustomStrings.CASE_INFO_ASSESS, 
				new TableCell(CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE, ""));
		
		// another inc report
		CaseReport cr2 = new CaseReport();
		cr2.copyValues(cr);
		daoService.add(cr2);
		
		SummarizedInfoValidator validator = new SummarizedInfoValidator(daoService);
		Collection<SampleCheck> checks = validator.isSampleCorrect(si);
		
		assertFalse(checks.contains(SampleCheck.TOO_MANY_INCONCLUSIVES));
	}
	
	@Test
	public void missingRGTCaseCheck() {

		// rgt
		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, 
				new TableCell(CustomStrings.SUMMARIZED_INFO_RGT_TYPE, ""));
		
		// delete the case
		daoService.delete(cr.getSchema(), cr.getDatabaseId());
		
		SummarizedInfoValidator validator = new SummarizedInfoValidator(daoService);
		Collection<SampleCheck> checks = validator.isSampleCorrect(si);
		
		assertTrue(checks.contains(SampleCheck.MISSING_RGT_CASE));
	}
	
	@Test
	public void wrongAgeClassComparedToReportDateCheck() {

		report.setYear("2005");
		report.setMonth("5");
		si.put(CustomStrings.SUMMARIZED_INFO_AGE, new TableCell("F31.A16NK", ""));  // < 24 months

		cr.put(CustomStrings.CASE_INFO_BIRTH_MONTH, "5");
		cr.put(CustomStrings.CASE_INFO_BIRTH_YEAR, "2002");

		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();
		
		boolean ageClassError = false;
		for(ReportError err: errors) {
			if (err instanceof WrongAgeClassError)
				ageClassError = true;
		}
		
		assertTrue(ageClassError);
	}
	
	@Test
	public void animalBornAfterReportDateCheck() {

		report.setYear("2005");
		report.setMonth("5");

		si.put(CustomStrings.SUMMARIZED_INFO_AGE, new TableCell("F31.A16NK", ""));  // < 24 months

		cr.put(CustomStrings.CASE_INFO_BIRTH_MONTH, "6");
		cr.put(CustomStrings.CASE_INFO_BIRTH_YEAR, "2005");

		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();
		
		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof ReportDateExceededError)
				myError = true;
		}
		
		assertTrue(myError);
	}
	
	@Test
	public void correctAgeClassComparedToReportDate() {
		
		report.setYear("2005");
		report.setMonth("5");
		si.put(CustomStrings.SUMMARIZED_INFO_AGE, new TableCell("F31.A16NK", ""));  // < 24 months
		cr.put(CustomStrings.CASE_INFO_BIRTH_MONTH, "6");
		cr.put(CustomStrings.CASE_INFO_BIRTH_YEAR, "2004");
		
		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();
		
		// no age class errors
		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof ReportDateExceededError || err instanceof WrongAgeClassError)
				myError = true;
		}
		
		assertFalse(myError);
	}
	
	@Test
	public void unknownAgeClassGreaterThan5Percent() {
		
		// unknown/all = 1/19 > 5%
		
		si.put(CustomStrings.SUMMARIZED_INFO_TOT_SAMPLES, "18");
		
		SummarizedInfo si2 = new SummarizedInfo();
		si2.copyValues(si);
		si2.put(CustomStrings.SUMMARIZED_INFO_AGE, new TableCell(CustomStrings.UNKNOWN_AGE_CLASS_CODE, ""));
		si2.put(CustomStrings.SUMMARIZED_INFO_TOT_SAMPLES, "1");
		
		daoService.add(si2);
		
		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();
		
		// no age class errors
		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof TooManyUnknownAgeClassesError)
				myError = true;
		}
		
		assertTrue(myError);
	}
	
	@Test
	public void unknownAgeClassLessThan5Percent() {
		
		// unknown/all = 1/20 <= 5%
		
		si.put(CustomStrings.SUMMARIZED_INFO_TOT_SAMPLES, "19");
		
		SummarizedInfo si2 = new SummarizedInfo();
		si2.copyValues(si);
		si2.put(CustomStrings.SUMMARIZED_INFO_AGE, new TableCell(CustomStrings.UNKNOWN_AGE_CLASS_CODE, ""));
		si2.put(CustomStrings.SUMMARIZED_INFO_TOT_SAMPLES, "1");
		
		daoService.add(si2);
		
		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();
		
		// no age class errors
		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof TooManyUnknownAgeClassesError)
				myError = true;
		}
		
		assertFalse(myError);
	}
	
	@Test
	public void nonWildCwdAndKilledShouldBeRejected() {
		
		si.put(CustomStrings.SUMMARIZED_INFO_PROD, new TableCell(CustomStrings.FARMED_PROD, ""));
		si.put(CustomStrings.SUMMARIZED_INFO_TARGET_GROUP, new TableCell(CustomStrings.KILLED_TARGET_GROUP, ""));
		
		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();
		
		// no age class errors
		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof NonWildAndKilledError)
				myError = true;
		}
		
		assertTrue(myError);
	}
	
	@Test
	public void wildCwdAndKilledShouldBeAccepted() {
		
		si.put(CustomStrings.SUMMARIZED_INFO_PROD, new TableCell(CustomStrings.WILD_PROD, ""));
		si.put(CustomStrings.SUMMARIZED_INFO_TARGET_GROUP, new TableCell(CustomStrings.KILLED_TARGET_GROUP, ""));
		
		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();
		
		// no age class errors
		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof NonWildAndKilledError)
				myError = true;
		}
		
		assertFalse(myError);
	}
	
	
	@Test
	public void allelesNotReportableWithScreening() {
		
		result.put(CustomStrings.RESULT_ALLELE_1, new TableCell(CustomStrings.ALLELE_AFRR, ""));
		result.put(CustomStrings.RESULT_TEST_TYPE, new TableCell(CustomStrings.SCREENING_TEST_CODE, ""));
		
		ResultValidator validator = new ResultValidator();
		ErrorType error = validator.getError(result);
		
		assertEquals(ErrorType.ALLELE_ERROR, error);
	}
	
	@Test
	public void allelesNotReportableWithConfirmatory() {
		
		result.put(CustomStrings.RESULT_ALLELE_1, new TableCell(CustomStrings.ALLELE_AFRR, ""));
		result.put(CustomStrings.RESULT_TEST_TYPE, new TableCell(CustomStrings.CONFIRMATORY_TEST_CODE, ""));
		
		ResultValidator validator = new ResultValidator();
		ErrorType error = validator.getError(result);
		
		assertEquals(ErrorType.ALLELE_ERROR, error);
	}
	
	@Test
	public void allelesNotReportableWithDiscriminatory() {
		
		result.put(CustomStrings.RESULT_ALLELE_1, new TableCell(CustomStrings.ALLELE_AFRR, ""));
		result.put(CustomStrings.RESULT_TEST_TYPE, new TableCell(CustomStrings.DISCRIMINATORY_TEST_CODE, ""));
		
		ResultValidator validator = new ResultValidator();
		ErrorType error = validator.getError(result);
		
		assertEquals(ErrorType.ALLELE_ERROR, error);
	}
	
	@Test
	public void allelesAreReportableWithGenotyping() {
		
		result.put(CustomStrings.RESULT_ALLELE_1, new TableCell(CustomStrings.ALLELE_AFRR, ""));
		result.put(CustomStrings.RESULT_TEST_TYPE, new TableCell(CustomStrings.MOLECULAR_TEST_CODE, ""));
		
		ResultValidator validator = new ResultValidator();
		ErrorType error = validator.getError(result);
		
		assertEquals(ErrorType.NONE, error);
	}
	
	@Test
	public void wrongAllelePairsAfrrAfrr() {
		
		result.put(CustomStrings.RESULT_ALLELE_1, new TableCell(CustomStrings.ALLELE_AFRR, ""));
		result.put(CustomStrings.RESULT_ALLELE_2, new TableCell(CustomStrings.ALLELE_AFRR, ""));
		result.put(CustomStrings.RESULT_TEST_TYPE, new TableCell(CustomStrings.MOLECULAR_TEST_CODE, ""));
		
		ResultValidator validator = new ResultValidator();
		ErrorType error = validator.getError(result);
		
		assertEquals(ErrorType.WRONG_ALLELE_PAIR, error);
	}
	
	@Test
	public void wrongAllelePairsAfrrAlrr() {
		
		result.put(CustomStrings.RESULT_ALLELE_1, new TableCell(CustomStrings.ALLELE_AFRR, ""));
		result.put(CustomStrings.RESULT_ALLELE_2, new TableCell(CustomStrings.ALLELE_ALRR, ""));
		result.put(CustomStrings.RESULT_TEST_TYPE, new TableCell(CustomStrings.MOLECULAR_TEST_CODE, ""));
		
		ResultValidator validator = new ResultValidator();
		ErrorType error = validator.getError(result);
		
		assertEquals(ErrorType.WRONG_ALLELE_PAIR, error);
	}
	
	@Test
	public void wrongAllelePairsAlrrAfrr() {
		
		result.put(CustomStrings.RESULT_ALLELE_1, new TableCell(CustomStrings.ALLELE_ALRR, ""));
		result.put(CustomStrings.RESULT_ALLELE_2, new TableCell(CustomStrings.ALLELE_AFRR, ""));
		result.put(CustomStrings.RESULT_TEST_TYPE, new TableCell(CustomStrings.MOLECULAR_TEST_CODE, ""));
		
		ResultValidator validator = new ResultValidator();
		ErrorType error = validator.getError(result);
		
		assertEquals(ErrorType.WRONG_ALLELE_PAIR, error);
	}
	
	@Test
	public void wrongAllelePairsAlrrAlrr() {
		
		result.put(CustomStrings.RESULT_ALLELE_1, new TableCell(CustomStrings.ALLELE_ALRR, ""));
		result.put(CustomStrings.RESULT_ALLELE_2, new TableCell(CustomStrings.ALLELE_ALRR, ""));
		result.put(CustomStrings.RESULT_TEST_TYPE, new TableCell(CustomStrings.MOLECULAR_TEST_CODE, ""));
		
		ResultValidator validator = new ResultValidator();
		ErrorType error = validator.getError(result);
		
		assertEquals(ErrorType.WRONG_ALLELE_PAIR, error);
	}
	
	@Test
	public void duplicatedScreeningTestCheck() throws IOException {

		result.put(CustomStrings.RESULT_TEST_TYPE, new TableCell(CustomStrings.SCREENING_TEST_CODE, ""));
		
		AnalyticalResult r2 = new AnalyticalResult();
		r2.copyValues(result);
		r2.put(CustomStrings.RES_ID_COLUMN, "9089038139281");
		
		daoService.add(r2);
		
		CaseReportValidator validator = new CaseReportValidator(daoService);
		Collection<Check> checks = validator.isRecordCorrect(cr);

		assertTrue(checks.contains(Check.DUPLICATED_TEST));
	}
	
	@Test
	public void duplicatedConfirmatoryTestCheck() throws IOException {

		result.put(CustomStrings.RESULT_TEST_TYPE, new TableCell(CustomStrings.CONFIRMATORY_TEST_CODE, ""));
		
		AnalyticalResult r2 = new AnalyticalResult();
		r2.copyValues(result);
		r2.put(CustomStrings.RES_ID_COLUMN, "9089038139281");
		
		daoService.add(r2);

		CaseReportValidator validator = new CaseReportValidator(daoService);
		Collection<Check> checks = validator.isRecordCorrect(cr);
		
		assertTrue(checks.contains(Check.DUPLICATED_TEST));
	}
	
	@Test
	public void duplicatedDiscriminatoryTestCheck() throws IOException {

		result.put(CustomStrings.RESULT_TEST_TYPE, new TableCell(CustomStrings.DISCRIMINATORY_TEST_CODE, ""));
		
		AnalyticalResult r2 = new AnalyticalResult();
		r2.copyValues(result);
		r2.put(CustomStrings.RES_ID_COLUMN, "9089038139281");
		
		daoService.add(r2);

		CaseReportValidator validator = new CaseReportValidator(daoService);
		Collection<Check> checks = validator.isRecordCorrect(cr);
		
		assertTrue(checks.contains(Check.DUPLICATED_TEST));
	}
	
	@Test
	public void duplicatedGenotypingTestCheck() throws IOException {

		result.put(CustomStrings.RESULT_TEST_TYPE, new TableCell(CustomStrings.MOLECULAR_TEST_CODE, ""));
		
		AnalyticalResult r2 = new AnalyticalResult();
		r2.copyValues(result);
		r2.put(CustomStrings.RES_ID_COLUMN, "9089038139281");
		
		daoService.add(r2);

		CaseReportValidator validator = new CaseReportValidator(daoService);
		Collection<Check> checks = validator.isRecordCorrect(cr);
		
		assertTrue(checks.contains(Check.DUPLICATED_TEST));
	}
	
	@Test
	public void noDetailedTestCheck() throws IOException {

		daoService.delete(result.getSchema(), result.getDatabaseId());

		CaseReportValidator validator = new CaseReportValidator(daoService);
		Collection<Check> checks = validator.isRecordCorrect(cr);
		
		assertTrue(checks.contains(Check.NO_TEST_SPECIFIED));
	}
	
	@Test
	public void indexCaseForNegativeSampleCheck() throws IOException {

		cr.put(CustomStrings.CASE_INFO_ASSESS, 
				new TableCell(CustomStrings.DEFAULT_ASSESS_NEG_CASE_CODE, ""));

		cr.put(CustomStrings.CASE_INDEX_CASE, 
				new TableCell(CustomStrings.INDEX_CASE_NO, ""));
		
		CaseReportValidator validator = new CaseReportValidator(daoService);
		Collection<Check> checks = validator.isRecordCorrect(cr);
		
		assertTrue(checks.contains(Check.INDEX_CASE_FOR_NEGATIVE));
	}
	
	@Test
	public void caseIdForNegativeSampleCheck() throws IOException {

		cr.put(CustomStrings.CASE_INFO_ASSESS, 
				new TableCell(CustomStrings.DEFAULT_ASSESS_NEG_CASE_CODE, ""));

		cr.put(CustomStrings.CASE_INFO_CASE_ID, new TableCell("caseid", "caseid"));
		
		CaseReportValidator validator = new CaseReportValidator(daoService);
		Collection<Check> checks = validator.isRecordCorrect(cr);
		
		assertTrue(checks.contains(Check.CASE_ID_FOR_NEGATIVE));
	}
	
	@Test
	public void indexCaseNOForFarmedCwdCheck() throws IOException {

		// prod
		si.put(CustomStrings.SUMMARIZED_INFO_PROD, 
				new TableCell(CustomStrings.FARMED_PROD, ""));
		
		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, 
				new TableCell(CustomStrings.SUMMARIZED_INFO_CWD_TYPE, ""));
		
		// put index case
		cr.put(CustomStrings.CASE_INDEX_CASE, 
				new TableCell(CustomStrings.INDEX_CASE_NO, ""));
		
		CaseReportValidator validator = new CaseReportValidator(daoService);
		Collection<Check> checks = validator.isRecordCorrect(cr);
		
		assertTrue(checks.contains(Check.INDEX_CASE_FOR_FARMED_CWD));
	}
	
	@Test
	public void duplicatedResIdCheck() {
		AnalyticalResult r2 = new AnalyticalResult();
		r2.copyValues(result);
		
		daoService.add(r2);

		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();

		// no age class errors
		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof DuplicatedResultIdError)
				myError = true;
		}
		
		assertTrue(myError);
	}
	
	@Test
	public void notInfectedStatusForEradicationTargetGroupCheck() throws IOException {
		
		// Eradication measure
		si.put(CustomStrings.SUMMARIZED_INFO_TARGET_GROUP, 
				new TableCell(CustomStrings.EM_TARGET_GROUP, ""));
		
		// not infected
		cr.put(CustomStrings.CASE_INFO_STATUS, 
				new TableCell(CustomStrings.CASE_INFO_STATUS_NOT_INFECTED, ""));
		
		CaseReportValidator validator = new CaseReportValidator(daoService);
		Collection<Check> checks = validator.isRecordCorrect(cr);
		
		assertTrue(checks.contains(Check.EM_FOR_NOT_INFECTED));
	}
	
	@Test
	public void infectedStatusForEradicationTargetGroupInSheepShouldRaiseNoWarning() throws IOException {

		// Eradication measure
		si.put(CustomStrings.SUMMARIZED_INFO_TARGET_GROUP, 
				new TableCell(CustomStrings.EM_TARGET_GROUP, ""));
		
		// infected
		cr.put(CustomStrings.CASE_INFO_STATUS, 
				new TableCell(CustomStrings.CASE_INFO_STATUS_INFECTED, ""));
		
		CaseReportValidator validator = new CaseReportValidator(daoService);
		Collection<Check> checks = validator.isRecordCorrect(cr);
		
		assertFalse(checks.contains(Check.EM_FOR_NOT_INFECTED));
	}
	
	@Test
	public void notInfectedStatusForNOTEradicationTargetGroupShouldRaiseNoWarning() throws IOException {
		
		// Eradication measure
		si.put(CustomStrings.SUMMARIZED_INFO_TARGET_GROUP, 
				new TableCell(CustomStrings.KILLED_TARGET_GROUP, ""));
		
		// infected
		cr.put(CustomStrings.CASE_INFO_STATUS, 
				new TableCell(CustomStrings.CASE_INFO_STATUS_NOT_INFECTED, ""));
		
		CaseReportValidator validator = new CaseReportValidator(daoService);
		Collection<Check> checks = validator.isRecordCorrect(cr);
		
		assertFalse(checks.contains(Check.EM_FOR_NOT_INFECTED));
	}
	
	
	@Test
	public void nonEmptyReportShouldCreateNoWarning() {
		
		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();

		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof EmptyReportError)
				myError = true;
		}
		
		assertFalse(myError);
	}
	
	@Test
	public void emptyReportShouldCreateWarning() {
		
		// delete the data
		daoService.delete(si.getSchema(), si.getDatabaseId());
		daoService.delete(cr.getSchema(), cr.getDatabaseId());
		daoService.delete(result.getSchema(), result.getDatabaseId());
		
		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();

		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof EmptyReportError)
				myError = true;
		}
		
		assertTrue(myError);
	}
	
	@Test
	public void duplicatedContextIdForSummarizedInformation() throws IOException, FormulaException {

		String contextId = formulaService.solve(si, 
				si.getSchema().getById(CustomStrings.CONTEXT_ID_COL), 
				XlsxHeader.LABEL_FORMULA);
		
		si.put(CustomStrings.CONTEXT_ID_COL, contextId);

		SummarizedInfo si2 = new SummarizedInfo();
		si2.copyValues(si);
		
		daoService.add(si2);

		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();

		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof DuplicatedContextError)
				myError = true;
		}
		
		assertTrue(myError);
	}
	
	@Test
	public void duplicatedResultIdForResults() throws IOException {

		result.put(CustomStrings.RES_ID_COLUMN, "resId");

		AnalyticalResult res2 = new AnalyticalResult();
		res2.copyValues(result);
		
		daoService.add(res2);

		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();

		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof DuplicatedResultIdError)
				myError = true;
		}
		
		assertTrue(myError);
	}
	
	@Test
	public void duplicatedSampleIdForCases() throws IOException {

		cr.put(CustomStrings.CASE_INFO_SAMPLE_ID, "sampleid");

		CaseReport cr2 = new CaseReport();
		cr2.copyValues(cr);
		
		daoService.add(cr2);

		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();

		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof DuplicatedSampleIdError)
				myError = true;
		}
		
		assertTrue(myError);
	}
	
	// two cases with the same animal id must have the same data
	@Test
	public void inconsistentCaseDataWithSameAnimalId() throws IOException {

		cr.put(CustomStrings.CASE_INFO_ANIMAL_ID, "animalId");
		cr.put(CustomStrings.CASE_INFO_HOLDING_ID, "hold1");

		// different holding id
		CaseReport cr2 = new CaseReport();
		cr2.copyValues(cr);
		cr2.put(CustomStrings.CASE_INFO_HOLDING_ID, "hold2");
		
		daoService.add(cr2);

		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();

		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof InconsistentCasesError)
				myError = true;
		}
		
		assertTrue(myError);
	}
	
	// two cases with the same animal id must have the same data
	@Test
	public void differentAnimalIdWithSameNationalCaseId() throws IOException {

		cr.put(CustomStrings.CASE_INFO_CASE_ID, "caseId");
		cr.put(CustomStrings.CASE_INFO_ANIMAL_ID, "animalId1");

		// different holding id
		CaseReport cr2 = new CaseReport();
		cr2.copyValues(cr);
		cr2.put(CustomStrings.CASE_INFO_ANIMAL_ID, "animalId2");
		
		daoService.add(cr2);

		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();

		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof InconsistentCasesError)
				myError = true;
		}
		
		assertTrue(myError);
	}
	
	@Test
	public void noCaseDeclaredInSummarizedInformationForNonRGT() {
		
		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, 
				new TableCell(CustomStrings.SUMMARIZED_INFO_BSE_TYPE, ""));
		
		si.put(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES, "0");
		si.put(CustomStrings.SUMMARIZED_INFO_NEG_SAMPLES, "0");
		si.put(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES, "0");
		si.put(CustomStrings.SUMMARIZED_INFO_UNS_SAMPLES, "0");
		si.put(CustomStrings.SUMMARIZED_INFO_TOT_SAMPLES, "0");
		
		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();

		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof NoCaseDeclaredError)
				myError = true;
		}
		
		assertTrue(myError);
	}
	
	@Test
	public void noCaseDeclaredInSummarizedInformationForRGT() {
		
		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, 
				new TableCell(CustomStrings.SUMMARIZED_INFO_RGT_TYPE, ""));
		
		si.put(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES, "0");
		si.put(CustomStrings.SUMMARIZED_INFO_NEG_SAMPLES, "0");
		si.put(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES, "0");
		si.put(CustomStrings.SUMMARIZED_INFO_UNS_SAMPLES, "0");
		si.put(CustomStrings.SUMMARIZED_INFO_TOT_SAMPLES, "0");
		
		TseReportValidator validator = new TseReportValidator(report, reportService, daoService);
		Collection<ReportError> errors = validator.validate();

		boolean myError = false;
		for(ReportError err: errors) {
			if (err instanceof NoCaseDeclaredError)
				myError = true;
		}
		
		assertFalse(myError);
	}
}

package predefined_results_reader;

import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import report.Report;
import table_relations.Relation;
import table_skeleton.TableRow;
import table_skeleton.TableRowList;
import tse_analytical_result.AnalyticalResult;
import tse_case_report.CaseReport;
import tse_config.CustomStrings;
import tse_summarized_information.SummarizedInfo;

public class PredefinedResult extends HashMap<PredefinedResultHeader, String> {
	
	private static final Logger LOGGER = LogManager.getLogger(PredefinedResult.class);
	private static final long serialVersionUID = 1L;
	
	/**
	 * Create the default results for a case
	 * @param report
	 * @param summInfo
	 * @param caseReport
	 * @throws IOException
	 */
	public static TableRowList createDefaultResults(Report report, 
			SummarizedInfo summInfo, CaseReport caseReport) throws IOException {

		TableRowList results = new TableRowList();
		
		AnalyticalResult r = createDefaultResult(report, summInfo, caseReport, 
				PredefinedResultHeader.SCREENING,
				CustomStrings.RESULT_SCREENING_TEST);
		if (r != null)
			results.add(r);
		
		r = createDefaultResult(report, summInfo, caseReport, 
				PredefinedResultHeader.CONFIRMATORY, 
				CustomStrings.SUMMARIZED_INFO_CONFIRMATORY_TEST);
		if (r != null)
			results.add(r);
		
		
		r = createDefaultResult(report, summInfo, caseReport, 
				PredefinedResultHeader.DISCRIMINATORY,
				CustomStrings.RESULT_DISCRIMINATORY_TEST);
		if (r != null)
			results.add(r);
		
		r = createDefaultResult(report, summInfo, caseReport, 
				PredefinedResultHeader.GENOTYPING_BASE_TERM,
				CustomStrings.SUMMARIZED_INFO_MOLECULAR_TEST);
		if (r != null)
			results.add(r);
		
		return results;
	}
	
	/**
	 * Check if the preference for the confirmatory test was set for
	 * the selected type of animal
	 * @param type
	 * @return
	 * @throws IOException
	 */
	public static boolean isConfirmatoryTested(String type) throws IOException {

		String confirmatory = "";
		
		switch(type) {
		case CustomStrings.SUMMARIZED_INFO_BSE_TYPE:
			confirmatory = Relation.getGlobalParent(CustomStrings.PREFERENCES_SHEET)
					.getCode(CustomStrings.PREFERENCES_CONFIRMATORY_BSE);
			break;
		case CustomStrings.SUMMARIZED_INFO_SCRAPIE_TYPE:
			confirmatory = Relation.getGlobalParent(CustomStrings.PREFERENCES_SHEET)
			.getCode(CustomStrings.PREFERENCES_CONFIRMATORY_SCRAPIE);
			break;
		case CustomStrings.SUMMARIZED_INFO_CWD_TYPE:
			confirmatory = Relation.getGlobalParent(CustomStrings.PREFERENCES_SHEET)
			.getCode(CustomStrings.PREFERENCES_CONFIRMATORY_CWD);
			break;
		}
		
		return !confirmatory.isEmpty();
	}
	
	public static String getPreferredTestType(String type, String testType) throws IOException {
		
		return Relation.getGlobalParent(CustomStrings.PREFERENCES_SHEET)
				.getCode(testType);
	}

	public static String getPreferredTestType(AnalyticalResult row, String recordType, String testType) 
			throws IOException {
		
		String preferredTestType = null;
		
		switch(testType) {
		case CustomStrings.RESULT_SCREENING_TEST:
			switch(recordType) {
			case CustomStrings.SUMMARIZED_INFO_BSE_TYPE:
				preferredTestType = getPreferredTestType(recordType, 
						CustomStrings.PREFERENCES_SCREENING_BSE);
				break;
			case CustomStrings.SUMMARIZED_INFO_SCRAPIE_TYPE:
				preferredTestType = getPreferredTestType(recordType, 
						CustomStrings.PREFERENCES_SCREENING_SCRAPIE);
				break;
			case CustomStrings.SUMMARIZED_INFO_CWD_TYPE:
				preferredTestType = getPreferredTestType(recordType, 
						CustomStrings.PREFERENCES_SCREENING_CWD);
				break;
			}
			break;
		case CustomStrings.SUMMARIZED_INFO_CONFIRMATORY_TEST:
			switch(recordType) {
			case CustomStrings.SUMMARIZED_INFO_BSE_TYPE:
				preferredTestType = getPreferredTestType(recordType, 
						CustomStrings.PREFERENCES_CONFIRMATORY_BSE);
				break;
			case CustomStrings.SUMMARIZED_INFO_SCRAPIE_TYPE:
				preferredTestType = getPreferredTestType(recordType, 
						CustomStrings.PREFERENCES_CONFIRMATORY_SCRAPIE);
				break;
			case CustomStrings.SUMMARIZED_INFO_CWD_TYPE:
				preferredTestType = getPreferredTestType(recordType, 
						CustomStrings.PREFERENCES_CONFIRMATORY_CWD);
				break;
			}
			break;
		case CustomStrings.RESULT_DISCRIMINATORY_TEST:
			switch(recordType) {
			case CustomStrings.SUMMARIZED_INFO_BSE_TYPE:
				preferredTestType = getPreferredTestType(recordType, 
						CustomStrings.PREFERENCES_DISCRIMINATORY_BSE);
				break;
			case CustomStrings.SUMMARIZED_INFO_SCRAPIE_TYPE:
				preferredTestType = getPreferredTestType(recordType, 
						CustomStrings.PREFERENCES_DISCRIMINATORY_SCRAPIE);
				break;
			case CustomStrings.SUMMARIZED_INFO_CWD_TYPE:
				preferredTestType = getPreferredTestType(recordType, 
						CustomStrings.PREFERENCES_DISCRIMINATORY_CWD);
				break;
			}
			break;
		case CustomStrings.SUMMARIZED_INFO_MOLECULAR_TEST:
			preferredTestType = CustomStrings.AN_METH_CODE_GENOTYPING;
			break;
		}
		
		return preferredTestType;
	}
	
	public static PredefinedResult getPredefinedResult(Report report, 
			SummarizedInfo summInfo, TableRow caseReport) throws IOException {
		
		// put the predefined value for the param code and the result
		PredefinedResultList predResList = PredefinedResultList.getAll();

		// get the info to know which result should be created
		String recordType = summInfo.getCode(CustomStrings.SUMMARIZED_INFO_TYPE);
		String source = summInfo.getCode(CustomStrings.SUMMARIZED_INFO_SOURCE);
		String sampAnAsses = caseReport.getCode(CustomStrings.CASE_INFO_ASSESS);
		boolean confirmatoryTested = isConfirmatoryTested(recordType);

		// get the default value
		PredefinedResult defaultResult = predResList.get(recordType, source, confirmatoryTested, sampAnAsses);
		
		return defaultResult;
	}
	
	/**
	 * Create a default result for the selected test
	 * @param report
	 * @param summInfo
	 * @param caseReport
	 * @param codeCol
	 * @param testTypeCode
	 * @throws IOException
	 */
	private static AnalyticalResult createDefaultResult(Report report, 
			SummarizedInfo summInfo, TableRow caseReport, 
			PredefinedResultHeader test, 
			String testTypeCode) throws IOException {
		
		AnalyticalResult resultRow = new AnalyticalResult();
		
		// inject the case parent to the result
		Relation.injectParent(report, resultRow);
		Relation.injectParent(summInfo, resultRow);
		Relation.injectParent(caseReport, resultRow);

		// get the default value
		PredefinedResult defaultResult = getPredefinedResult(report, summInfo, caseReport);
		
		// add the param base term and the related default result
		boolean added = addParamAndResult(resultRow, defaultResult, test);
		
		if (added) {
			// code crack to save the row id for the resId field
			// otherwise its default value will be corrupted
			resultRow.save();
			
			resultRow.initialize();
			
			resultRow.put(CustomStrings.RESULT_TEST_TYPE, testTypeCode);
			
			// get the info to know which result should be created
			String recordType = summInfo.getCode(CustomStrings.SUMMARIZED_INFO_TYPE);
			
			// add also the preferred test type
			String prefTest = getPreferredTestType(resultRow, recordType, testTypeCode);
			
			if (prefTest != null)
				resultRow.put(CustomStrings.AN_METH_CODE, prefTest);
			else
				LOGGER.warn("No preferred value of anMethCode was found for anMethType " + testTypeCode);
			
			addParamAndResult(resultRow, defaultResult, test);
			
			resultRow.updateFormulas();

			resultRow.update();
			
			return resultRow;
		}
		
		return null;
	}
	
	
	/**
	 * Add the param and the result to the selected row
	 * @param result
	 * @param defValues
	 * @param codeCol
	 * @return
	 */
	public static boolean addParamAndResult(TableRow result, 
			PredefinedResult defValues, 
			PredefinedResultHeader codeCol) {
		
		String code = defValues.get(codeCol);
		
		// put the test aim
		if (codeCol != PredefinedResultHeader.GENOTYPING_BASE_TERM && code != null && !code.equals("null"))  // excel fix
			result.put(CustomStrings.RESULT_TEST_AIM, code);
		
		// extract from it the param code and the result
		// and add them to the row
		return addParamAndResult(result, code);
	}
	
	/**
	 * Add param and result. These values are taken from the test code
	 * which is composed of paramBaseTerm$resultValue
	 * @param result
	 * @param testCode
	 * @return
	 */
	public static boolean addParamAndResult(TableRow result, String testCode) {
		
		if (testCode == null)
			return false;
		
		String[] split = testCode.split("\\$");
		String paramBaseTerm = split[0];
		
		String resultValue = null;
		if (split.length > 1)
			resultValue = split[1];
		
		boolean added = false;
		if (paramBaseTerm != null) {
			result.put(CustomStrings.PARAM_CODE_BASE_TERM_COL, paramBaseTerm);
			
			added = true;
			
			if (resultValue != null) {
				result.put(CustomStrings.RESULT_TEST_RESULT, resultValue);
			}
		}
		
		return added;
	}
}

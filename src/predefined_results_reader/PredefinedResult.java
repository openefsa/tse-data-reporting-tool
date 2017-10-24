package predefined_results_reader;

import java.io.IOException;
import java.util.HashMap;

import report.Report;
import table_relations.Relation;
import table_skeleton.TableRow;
import tse_analytical_result.AnalyticalResult;
import tse_config.CustomStrings;
import tse_summarized_information.SummarizedInfo;

public class PredefinedResult extends HashMap<PredefinedResultHeader, String> {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Create the default results for a case
	 * @param report
	 * @param summInfo
	 * @param caseReport
	 * @throws IOException
	 */
	public static void createDefaultResults(Report report, 
			SummarizedInfo summInfo, TableRow caseReport) throws IOException {

		createDefaultResult(report, summInfo, caseReport, 
				PredefinedResultHeader.SCREENING,
				CustomStrings.RESULT_SCREENING_TEST);
		
		createDefaultResult(report, summInfo, caseReport, 
				PredefinedResultHeader.CONFIRMATORY, 
				CustomStrings.SUMMARIZED_INFO_CONFIRMATORY_TEST);
		
		createDefaultResult(report, summInfo, caseReport, 
				PredefinedResultHeader.DISCRIMINATORY,
				CustomStrings.RESULT_DISCRIMINATORY_TEST);
		
		createDefaultResult(report, summInfo, caseReport, 
				PredefinedResultHeader.GENOTYPING_BASE_TERM,
				CustomStrings.SUMMARIZED_INFO_MOLECULAR_TEST);
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
	
	/**
	 * Create a default result for the selected test
	 * @param report
	 * @param summInfo
	 * @param caseReport
	 * @param codeCol
	 * @param testTypeCode
	 * @throws IOException
	 */
	private static void createDefaultResult(Report report, 
			SummarizedInfo summInfo, TableRow caseReport, 
			PredefinedResultHeader test, 
			String testTypeCode) throws IOException {
		
		AnalyticalResult resultRow = new AnalyticalResult();
		
		// inject the case parent to the result
		Relation.injectParent(report, resultRow);
		Relation.injectParent(summInfo, resultRow);
		Relation.injectParent(caseReport, resultRow);
		
		// put the predefined value for the param code and the result
		PredefinedResultList predResList = PredefinedResultList.getAll();

		// get the info to know which result should be created
		String recordType = summInfo.getCode(CustomStrings.SUMMARIZED_INFO_TYPE);
		String sampAnAsses = caseReport.getCode(CustomStrings.CASE_INFO_ASSESS);
		boolean confirmatoryTested = isConfirmatoryTested(recordType);

		// get the default value
		PredefinedResult defaultResult = predResList.get(recordType, confirmatoryTested, sampAnAsses);
		
		// add the param base term and the related default result
		boolean added = addParamAndResult(resultRow, defaultResult, test);
		
		if (added) {
			// code crack to save the row id for the resId field
			// otherwise its default value will be corrupted
			resultRow.save();
			
			resultRow.initialize();
			resultRow.updateFormulas();
			addParamAndResult(resultRow, defaultResult, test);
			resultRow.put(CustomStrings.RESULT_TEST_TYPE, testTypeCode);
			
			resultRow.update();
		}
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

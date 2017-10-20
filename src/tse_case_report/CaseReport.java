package tse_case_report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import predefined_results_reader.PredefinedResult;
import predefined_results_reader.PredefinedResultHeader;
import predefined_results_reader.PredefinedResultList;
import report.Report;
import table_database.TableDao;
import table_relations.Relation;
import table_skeleton.TableRow;
import tse_analytical_result.AnalyticalResult;
import tse_config.CustomStrings;
import tse_report.TseTableRow;
import tse_summarized_information.SummarizedInfo;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class CaseReport extends TableRow implements TseTableRow {

	public CaseReport(TableRow row) {
		super(row);
	}
	
	/**
	 * Get all the results
	 * @return
	 */
	public Collection<TseTableRow> getChildren() {
		
		Collection<TseTableRow> output = new ArrayList<>();
		
		TableSchema caseSchema = TableSchemaList.getByName(CustomStrings.RESULT_SHEET);
		
		TableDao dao = new TableDao(caseSchema);
		Collection<TableRow> children = dao.getByParentId(CustomStrings.CASE_INFO_SHEET, 
				this.getId(), "desc");
		
		// create it as result
		for (TableRow child : children) {
			output.add(new AnalyticalResult(child));
		}
		
		return output;
	}
	
	private static void createDefaultResult(Report report, 
			SummarizedInfo summInfo, TableRow caseReport, 
			PredefinedResultHeader codeCol, 
			PredefinedResultHeader resultCol,
			String testTypeCode) {
		
		AnalyticalResult resultRow = new AnalyticalResult();
		
		// inject the case parent to the result
		Relation.injectParent(report, resultRow);
		Relation.injectParent(summInfo, resultRow);
		Relation.injectParent(caseReport, resultRow);
		
		// put the predefined value for the param code and the result
		PredefinedResultList predResList = PredefinedResultList.getAll();

		String recordType = summInfo.getCode(CustomStrings.SUMMARIZED_INFO_TYPE);
		String sampAnAsses = caseReport.getCode(CustomStrings.CASE_INFO_ASSESS);

		PredefinedResult defaultResult = predResList.get(recordType, sampAnAsses);
		
		// add the param base term and the related default result
		boolean added = addParamAndResult(resultRow, defaultResult, codeCol, resultCol);
		
		if (added) {
			// code crack to save the row id for the resId field
			// otherwise its default value will be corrupted
			resultRow.save();
			
			resultRow.initialize();
			resultRow.updateFormulas();
			addParamAndResult(resultRow, defaultResult, codeCol, resultCol);
			resultRow.put(CustomStrings.RESULT_TEST_TYPE, testTypeCode);
			
			resultRow.update();
		}
	}
	
	public static void createDefaultResults(Report report, 
			SummarizedInfo summInfo, TableRow caseReport) throws IOException {

		createDefaultResult(report, summInfo, caseReport, 
				PredefinedResultHeader.SCREENING_PARAM_CODE, 
				PredefinedResultHeader.SCREENING_RESULT, 
				CustomStrings.RESULT_SCREENING_TEST);
		
		createDefaultResult(report, summInfo, caseReport, 
				PredefinedResultHeader.CONFIRMATORY_PARAM_CODE, 
				PredefinedResultHeader.CONFIRMATORY_RESULT, 
				CustomStrings.SUMMARIZED_INFO_CONFIRMATORY_TEST);
		
		createDefaultResult(report, summInfo, caseReport, 
				PredefinedResultHeader.DISCRIMINATORY_PARAM_CODE, 
				PredefinedResultHeader.DISCRIMINATORY_RESULT, 
				CustomStrings.RESULT_DISCRIMINATORY_TEST);
		
		createDefaultResult(report, summInfo, caseReport, 
				PredefinedResultHeader.GENOTYPING, 
				null, 
				CustomStrings.SUMMARIZED_INFO_MOLECULAR_TEST);
	}
	
	
	private static boolean addParamAndResult(TableRow result, PredefinedResult defValues, 
			PredefinedResultHeader codeCol, PredefinedResultHeader resultCol) {
		
		String paramCodeBaseTerm = defValues.get(codeCol);
		String resultValue = resultCol == null ? "" : defValues.get(resultCol);
		
		if (paramCodeBaseTerm != null && resultValue != null) {
			result.put(CustomStrings.PARAM_CODE_BASE_TERM_COL, paramCodeBaseTerm);
			result.put(CustomStrings.RESULT_TEST_RESULT, resultValue);
			return true;
		}
		
		return false;
	}
}

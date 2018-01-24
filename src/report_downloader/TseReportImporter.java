package report_downloader;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import amend_manager.ReportImporter;
import dataset.Dataset;
import dataset.DatasetList;
import formula.FormulaException;
import table_relations.Relation;
import table_skeleton.TableCell;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_report.TseReport;
import tse_summarized_information.SummarizedInfo;
import xlsx_reader.TableSchemaList;

/**
 * Download and import a dataset, managing also all the amendments
 * @author avonva
 *
 */
public class TseReportImporter extends ReportImporter {

	private static final Logger LOGGER = LogManager.getLogger(TseReportImporter.class);
	
	// temporary variables
	private TseReport report;
	private Collection<SummarizedInfo> summInfos;
	private HashMap<String, TableRow> cases;  // caseId, case

	/**
	 * Download and import a dataset, managing also all the amendments
	 * @param datasetVersions a list with all the dataset versions. 
	 * This is needed to manage amendments.
	 */
	public TseReportImporter(DatasetList datasetVersions) {
		super(datasetVersions, CustomStrings.RES_ID_COLUMN, 
				CustomStrings.SENDER_DATASET_ID_COLUMN);
		
		this.summInfos = new ArrayList<>();
		this.cases = new HashMap<>(); 
	}

	/**
	 * Check if a row is a summarized information row or not
	 * if not the row is an analytical result
	 * @param row
	 * @return
	 */
	private boolean isSummarizedInfo(TableRow row) {
		return row.getCode(CustomStrings.PARAM_TYPE_COL)
				.equals(CustomStrings.SUMMARIZED_INFO_PARAM_TYPE);
	}
	
	/**
	 * TODO to be implemented!!
	 * @param row
	 * @return
	 */
	private boolean isRGT(TableRow row) {
		/*
		 * TODO 
		 */
		return false;
	}

	/**
	 * Import all the summarized information into the db
	 * @param report
	 * @param datasetRows
	 * @throws FormulaException 
	 * @throws ParseException 
	 */
	private void importSummarizedInformation(TseReport report, Collection<TableRow> datasetRows) throws FormulaException, ParseException {

		// first process the summarized information
		for (TableRow row : datasetRows) {

			// if we have a summarized information
			// import it
			if (isSummarizedInfo(row)) {

				SummarizedInfo si = extractSummarizedInfo(report, row);

				// save it in the database
				si.save();
				
				LOGGER.info("Imported summ info; progId=" + si.getProgId());
				
				// save it in the cache
				summInfos.add(si);
			}
		}
	}

	/**
	 * Import all the cases and analytical results
	 * @param report
	 * @param summInfo
	 * @param datasetRows
	 * @throws FormulaException 
	 * @throws ParseException 
	 */
	private void importCasesAndResults(TseReport report, Collection<TableRow> datasetRows) throws FormulaException, ParseException {
		
		// process the cases and analytical results
		for (TableRow row : datasetRows) {

			if (!isSummarizedInfo(row)) {
				
				// foreign key with the summarized information
				String progId = row.getLabel(CustomStrings.RESULT_PROG_ID);
				
				// get the summarized info related to the case/result
				SummarizedInfo summInfo = getSummInfoByProgId(progId);
				
				if (summInfo == null && !isRGT(row)) {
					ParseException e = new ParseException("No summarized information found in dataset with progId=" + progId, -1);
					LOGGER.error("Cannot create cases and results without the related summarized information", e);
					throw e;
				}
				
				// TODO if summInfo == null && isRGT(row) => create summarized information
				// using the row data and then import case and results
				
				// import the case
				TableRow caseInfo = importCase(report, summInfo, row);
				
				LOGGER.info("Imported case/sample with database id=" + caseInfo.getDatabaseId() 
					+ ", sampId=" + caseInfo.getCode(CustomStrings.CASE_INFO_SAMPLE_ID));
				
				// import the result
				TableRow result = importResult(report, summInfo, caseInfo, row);
				
				LOGGER.info("Imported analytical result with database id=" + result.getDatabaseId());
			}
		}
	}
	
	/**
	 * Import the case if possible
	 * @param report
	 * @param row
	 * @return
	 * @throws FormulaException 
	 * @throws ParseException 
	 */
	private TableRow importCase(TseReport report, SummarizedInfo summInfo, TableRow row) throws FormulaException, ParseException {

		// extract the case from the row
		TableRow currentCaseInfo = extractCase(report, summInfo, row);

		// import the case info if not already imported
		if (currentCaseInfo.getDatabaseId() == -1) {

			// import case in the db
			currentCaseInfo.save();

			String sampId = currentCaseInfo.getLabel(CustomStrings.CASE_INFO_SAMPLE_ID);
			
			if (sampId == null) {
				LOGGER.error("No sample id was found for " + currentCaseInfo);
				return currentCaseInfo;
			}
			
			// save the case in the cache by its sample id
			cases.put(sampId, currentCaseInfo);
		}
		
		return currentCaseInfo;
	}
	
	/**
	 * Import the result into the db
	 * @param report
	 * @param summInfo
	 * @param caseInfo
	 * @param row
	 * @return
	 * @throws ParseException 
	 */
	private TableRow importResult(TseReport report, SummarizedInfo summInfo, TableRow caseInfo, TableRow row) throws ParseException {

		// then import the analytical result
		TableRow result = extractAnalyticalResult(report, summInfo, caseInfo, row);

		// save the result into the db
		result.save();
		
		return result;
	}

	/**
	 * Extract the summarized information data from the current row
	 * @param report
	 * @param row
	 * @return
	 * @throws FormulaException 
	 * @throws ParseException 
	 */
	private SummarizedInfo extractSummarizedInfo(TseReport report, TableRow row) throws FormulaException, ParseException {

		// set the summarized information schema
		row.setSchema(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));

		HashMap<String, TableCell> rowValues = new HashMap<>();

		TSEFormulaDecomposer decomposer = new TSEFormulaDecomposer(row);
		rowValues.putAll(decomposer.decompose(CustomStrings.SUMMARIZED_INFO_SAMP_MAT_CODE));
		rowValues.putAll(decomposer.decompose(CustomStrings.SUMMARIZED_INFO_PROG_INFO));
		
		// copy values into the summarized information
		SummarizedInfo summInfo = new SummarizedInfo(row);
		
		for (String key : rowValues.keySet()) {
			summInfo.put(key, rowValues.get(key));
		}

		// set the report as parent of the summ info
		Relation.injectParent(report, summInfo);
		
		// add pref and settings as information
		try {
			Relation.injectGlobalParent(summInfo, CustomStrings.PREFERENCES_SHEET);
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Cannot inject global parent=" + CustomStrings.PREFERENCES_SHEET, e);
		}

		try {
			Relation.injectGlobalParent(summInfo, CustomStrings.SETTINGS_SHEET);
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Cannot inject global parent=" + CustomStrings.SETTINGS_SHEET, e);
		}
		
		// set also the summarized information type using
		// the species
		String type = summInfo.getTypeBySpecies();
		summInfo.setType(type);

		return summInfo;
	}
	
	/**
	 * Extract the case row from the analytical result row
	 * @param report
	 * @param summInfo
	 * @param row
	 * @return
	 * @throws FormulaException 
	 * @throws ParseException 
	 */
	private TableRow extractCase(TseReport report, SummarizedInfo summInfo, TableRow row) throws FormulaException, ParseException {

		// set schema (required for next step), we are processing a result row,
		// even if we are extracting the case information data!
		row.setSchema(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
		
		TableCell sampId = row.get(CustomStrings.RESULT_SAMPLE_ID);
		
		if (sampId == null)
			throw new ParseException("Missing sampId", -1);
		
		// create empty case report
		TableRow caseReport;

		// if not already added
		if (cases.get(sampId.getLabel()) == null) {

			// create the case info (we do not copy the data, since this row
			// is actually an analytical result and we just need to 
			// extract the relevant information)
			caseReport = new TableRow(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));

			HashMap<String, TableCell> rowValues = new HashMap<>();

			TSEFormulaDecomposer decomposer = new TSEFormulaDecomposer(row);

			rowValues.putAll(decomposer.decompose(CustomStrings.RESULT_EVAL_INFO));
			rowValues.putAll(decomposer.decompose(CustomStrings.RESULT_SAMP_UNIT_IDS));
			rowValues.putAll(decomposer.decompose(CustomStrings.RESULT_SAMP_EVENT_INFO));
			
			HashMap<String, TableCell> sampMatCodeDecomposed = 
					decomposer.decompose(CustomStrings.SUMMARIZED_INFO_SAMP_MAT_CODE);
			
			TableCell part = sampMatCodeDecomposed.get(CustomStrings.SUMMARIZED_INFO_PART);
			rowValues.put(CustomStrings.SUMMARIZED_INFO_PART, part);

			// save sample id
			rowValues.put(CustomStrings.CASE_INFO_SAMPLE_ID, sampId);

			rowValues.putAll(decomposer.decompose(CustomStrings.RESULT_SAMP_EVENT_INFO));
			rowValues.putAll(decomposer.decompose(CustomStrings.RESULT_SAMP_MAT_INFO));
			
			rowValues.put(CustomStrings.RESULT_SAMP_AREA, 
					row.get(CustomStrings.RESULT_SAMP_AREA));
			
			rowValues.put(CustomStrings.RESULT_SAMP_DAY, row.get(CustomStrings.RESULT_SAMP_DAY));

			// store all the values into the case report
			for (String key : rowValues.keySet()) {
				caseReport.put(key, rowValues.get(key));
			}

			// set the report/summ info as parent of case report
			Relation.injectParent(report, caseReport);
			Relation.injectParent(summInfo, caseReport);
		}
		else {

			// else if already present, get it from the cache
			caseReport = cases.get(sampId.getLabel());
		}

		return caseReport;
	}

	/**
	 * Extract the analytical result data from the current row
	 * @param report
	 * @param summInfo
	 * @param caseInfo
	 * @param row
	 * @return
	 * @throws ParseException 
	 */
	private TableRow extractAnalyticalResult(TseReport report, SummarizedInfo summInfo, 
			TableRow caseInfo, TableRow row) throws ParseException {

		// set the summarized information schema
		row.setSchema(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));

		// decompose param code
		TSEFormulaDecomposer decomposer = new TSEFormulaDecomposer(row);
		
		HashMap<String, TableCell> rowValues = 
				decomposer.decompose(CustomStrings.PARAM_CODE_COL);

		// save also the test aim with base term and test result
		String paramBaseTerm = decomposer.getBaseTerm(
				row.getCode(CustomStrings.PARAM_CODE_COL));

		// save the base term also
		row.put(CustomStrings.PARAM_CODE_BASE_TERM_COL, paramBaseTerm);
		
		String resQualValue = row.getCode(CustomStrings.RESULT_TEST_RESULT);
		
		// only if we have the test result put also the test aim
		if (!resQualValue.isEmpty()) {
			String testAim = paramBaseTerm + "$" + resQualValue;
			row.put(CustomStrings.RESULT_TEST_AIM, testAim);
		}

		// copy values into the row
		TableRow result = new TableRow(row);
		for (String key : rowValues.keySet()) {
			result.put(key, rowValues.get(key));
		}

		// set the report as parent of the summ info
		Relation.injectParent(report, result);
		Relation.injectParent(summInfo, result);
		Relation.injectParent(caseInfo, result);
		
		return result;
	}

	/**
	 * Given a prog id of an analytical result, get the summarized
	 * information which is related to it
	 * @param progId
	 * @return
	 */
	private SummarizedInfo getSummInfoByProgId(String progId) {

		for (SummarizedInfo info : summInfos) {
			String progId2 = info.getLabel(CustomStrings.SUMMARIZED_INFO_PROG_ID);
			if (progId2.equals(progId)) {
				return info;
			}
		}

		return null;
	}

	@Override
	public TableRow importDatasetMetadata(Dataset dataset) {
		
		// extract the information from the dataset
		// and insert the report into the database
		this.report = TseReport.fromDataset(dataset);
		
		report.save();
		
		return this.report;
	}
	
	@Override
	public void importDatasetRows(List<TableRow> rows) throws FormulaException, ParseException {
		
		// first import the summarized information
		importSummarizedInformation(report, rows);

		// then import cases and results
		importCasesAndResults(report, rows);
	}
}

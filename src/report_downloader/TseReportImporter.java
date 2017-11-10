package report_downloader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import amend_manager.ReportImporter;
import dataset.Dataset;
import dataset.DatasetList;
import formula.FormulaDecomposer;
import formula.FormulaException;
import table_relations.Relation;
import table_skeleton.TableColumn;
import table_skeleton.TableColumnValue;
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

	// temporary variables
	private TseReport report;
	private Collection<SummarizedInfo> summInfos;
	private HashMap<String, TableRow> cases;  // caseId, case

	/**
	 * Download and import a dataset, managing also all the amendments
	 * @param datasetVersions a list with all the dataset versions. 
	 * This is needed to manage amendments.
	 */
	public TseReportImporter(DatasetList<Dataset> datasetVersions) {
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
	 * Import all the summarized information into the db
	 * @param report
	 * @param datasetRows
	 * @throws FormulaException 
	 */
	private void importSummarizedInformation(TseReport report, Collection<TableRow> datasetRows) throws FormulaException {

		// first process the summarized information
		for (TableRow row : datasetRows) {

			// if we have a summarized information
			// import it
			if (isSummarizedInfo(row)) {

				SummarizedInfo si = extractSummarizedInfo(report, row);

				// save it in the database
				si.save();
				
				System.out.println("Imported summ info " + si.getId());
				
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
	 */
	private void importCasesAndResults(TseReport report, Collection<TableRow> datasetRows) throws FormulaException {
		
		// process the cases and analytical results
		for (TableRow row : datasetRows) {

			if (!isSummarizedInfo(row)) {
				
				// foreign key with the summarized information
				String progId = row.getLabel(CustomStrings.RESULT_PROG_ID);
				
				// get the summarized info related to the case/result
				SummarizedInfo summInfo = getSummInfoByProgId(progId);
				
				// import the case
				TableRow caseInfo = importCase(report, summInfo, row);
				
				System.out.println("Imported caseInfo " + caseInfo.getId());
				
				// import the result
				TableRow result = importResult(report, summInfo, caseInfo, row);
				
				System.out.println("Imported result " + result.getId());
			}
		}
	}
	
	/**
	 * Import the case if possible
	 * @param report
	 * @param row
	 * @return
	 * @throws FormulaException 
	 */
	private TableRow importCase(TseReport report, SummarizedInfo summInfo, TableRow row) throws FormulaException {

		// extract the case from the row
		TableRow currentCaseInfo = extractCase(report, summInfo, row);

		// import the case info if not already imported
		if (currentCaseInfo.getId() == -1) {

			// import case in the db
			currentCaseInfo.save();

			String sampId = getCaseSampleId(currentCaseInfo);
			
			if (sampId == null) {
				System.err.println("No sample id was found for " + currentCaseInfo);
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
	 */
	private TableRow importResult(TseReport report, SummarizedInfo summInfo, TableRow caseInfo, TableRow row) {

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
	 */
	private SummarizedInfo extractSummarizedInfo(TseReport report, TableRow row) throws FormulaException {

		// set the summarized information schema
		row.setSchema(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));

		HashMap<String, TableColumnValue> rowValues = new HashMap<>();

		// split compound fields and add them to the summarized information
		rowValues.putAll(decomposeField(CustomStrings.SUMMARIZED_INFO_SAMP_MAT_CODE, row, true, true));
		rowValues.putAll(decomposeField(CustomStrings.SUMMARIZED_INFO_PROG_INFO, row, true));
		
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
		}

		try {
			Relation.injectGlobalParent(summInfo, CustomStrings.SETTINGS_SHEET);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// set also the summarized information type using
		// the species
		String type = summInfo.getTypeBySpecies();
		summInfo.setType(type);

		return summInfo;
	}

	private String getCaseSampleId(TableRow row) throws FormulaException {
		
		HashMap<String, TableColumnValue> sampIdDecomposed = getDecomposedSampleId(row);
		
		TableColumnValue sampIdValue;
		if (sampIdDecomposed != null && sampIdDecomposed.get(CustomStrings.RESULT_SAMPLE_ID) != null)
			sampIdValue = sampIdDecomposed.get(CustomStrings.RESULT_SAMPLE_ID);
		else {
			return null;
		}
		
		if (sampIdValue == null || sampIdValue.getLabel() == null)
			return null;
		
		String sampId = sampIdValue.getLabel();
		
		return sampId;
	}
	
	private HashMap<String, TableColumnValue> getDecomposedSampleId(TableRow row) throws FormulaException {
		
		TableColumnValue value = row.get(CustomStrings.RESULT_SAMPLE_ID);
		
		HashMap<String, TableColumnValue> sampIdDecomposed = new HashMap<String, TableColumnValue>();
		sampIdDecomposed.put(CustomStrings.RESULT_SAMPLE_ID, value);
		
		return sampIdDecomposed;
	}
	
	/**
	 * Extract the case row from the analytical result row
	 * @param report
	 * @param summInfo
	 * @param row
	 * @return
	 * @throws FormulaException 
	 */
	private TableRow extractCase(TseReport report, SummarizedInfo summInfo, TableRow row) throws FormulaException {

		// set schema (required for next step), we are processing a result row,
		// even if we are extracting the case information data!
		row.setSchema(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
		
		String sampId = getCaseSampleId(row);
		
		// create empty case report
		TableRow caseReport;

		// if not already added
		if (cases.get(sampId) == null) {

			// create the case info (we do not copy the data, since this row
			// is actually an analytical result and we just need to 
			// extract the relevant information)
			caseReport = new TableRow(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));

			HashMap<String, TableColumnValue> rowValues = new HashMap<>();

			// retrieve the case id from the result row
			HashMap<String, TableColumnValue> evalInfoDecomposed = 
					decomposeField(CustomStrings.RESULT_EVAL_INFO, row, true);
			
			// add the decomposed eval info
			rowValues.putAll(evalInfoDecomposed);

			// save the samp unit ids decomposed
			HashMap<String, TableColumnValue> sampUnitIdsDecomposed = 
					decomposeField(CustomStrings.RESULT_SAMP_UNIT_IDS, row, true);
			rowValues.putAll(sampUnitIdsDecomposed);

			// save samp event decomposed
			HashMap<String, TableColumnValue> sampEventInfoDecomposed = 
					decomposeField(CustomStrings.RESULT_SAMP_EVENT_INFO, row, true);
			rowValues.putAll(sampEventInfoDecomposed);
			
			// extract the part from the analytical result
			HashMap<String, TableColumnValue> sampMatCodeDecomposed = 
					decomposeField(CustomStrings.RESULT_SAMP_MAT_CODE, row, true);

			TableColumnValue part = sampMatCodeDecomposed.get(CustomStrings.SUMMARIZED_INFO_PART);
			rowValues.put(CustomStrings.SUMMARIZED_INFO_PART, part);

			// save sample id
			HashMap<String, TableColumnValue> sampIdDecomposed = getDecomposedSampleId(row);
			rowValues.putAll(sampIdDecomposed);

			// save samp mat info
			HashMap<String, TableColumnValue> sampMatInfo = 
					decomposeField(CustomStrings.RESULT_SAMP_MAT_INFO, row, true);
			rowValues.putAll(sampMatInfo);
			
			// save the samp area
			HashMap<String, TableColumnValue> sampArea = 
					decomposeField(CustomStrings.RESULT_SAMP_AREA, row, false);
			rowValues.putAll(sampArea);
			
			// save the samp day
			HashMap<String, TableColumnValue> sampDay = 
					decomposeField(CustomStrings.RESULT_SAMP_DAY, row, false);
			rowValues.putAll(sampDay);

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
			caseReport = cases.get(sampId);
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
	 */
	private TableRow extractAnalyticalResult(TseReport report, SummarizedInfo summInfo, 
			TableRow caseInfo, TableRow row) {

		// set the summarized information schema
		row.setSchema(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));

		HashMap<String, TableColumnValue> rowValues = new HashMap<>();

		// parse param code (NOTE cannot use decompose since here we have a too complex formula...)
		String paramCode = row.getCode(CustomStrings.PARAM_CODE_COL);
		String[] split = paramCode.split("#");
		
		String resQualValue = row.getCode(CustomStrings.RESULT_TEST_RESULT);
		
		// put the base term into the param code base term
		if (split.length >= 1) {
			String paramBase = split[0];
			row.put(CustomStrings.PARAM_CODE_BASE_TERM_COL, paramBase);
			
			String testResult = paramBase + "$" + resQualValue;
			row.put(CustomStrings.RESULT_TEST_AIM, testResult);
		}
		
		// at least two pieces, then save alleles
		if (split.length >= 2) {
			
			String[] facets = split[0].split("$");
			
			// get alleles from param
			if (facets.length >= 2) {
				
				// decompose param code to get alleles
				row.put(CustomStrings.RESULT_ALLELE_1, facets[0]);
				row.put(CustomStrings.RESULT_ALLELE_2, facets[1]);
			}
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

	private HashMap<String, TableColumnValue> decomposeField(String columnId, 
			TableRow row, boolean label) throws FormulaException {
		return this.decomposeField(columnId, row, label, false);
	}
	
	/**
	 * Decompose a field into its children
	 * @param columnId
	 * @param row
	 * @return hashmap with the children values
	 * @throws FormulaException 
	 */
	private HashMap<String, TableColumnValue> decomposeField(String columnId, 
			TableRow row, boolean label, boolean forSummarized) throws FormulaException {

		TableColumn column = row.getSchema().getById(columnId);
		
		String formula;
		if (label)
			formula = column.getLabelFormula();
		else
			formula = column.getCodeFormula();
		
		String rowValue = row.getCode(columnId);

		FormulaDecomposer decomposer = new FormulaDecomposer(formula, rowValue);

		HashMap<String, String> values = new HashMap<>();

		switch(columnId) {
		case CustomStrings.SUMMARIZED_INFO_SAMP_MAT_CODE:  // foodex code
			if (forSummarized) {
				values = decomposer.decomposeFoodexCode(false);
			}
			else
				values = decomposer.decomposeRelationFieldAsFoodex();
			break;
		case CustomStrings.PARAM_CODE_COL:
			values = decomposer.decomposeFoodexCode(true);
			break;
		case CustomStrings.SUMMARIZED_INFO_PROG_INFO:
			values = decomposer.decomposeSimpleField("$", true);
			break;
		case CustomStrings.RESULT_SAMP_EVENT_INFO:
			
			try {
				if (forSummarized)
					values = decomposer.decomposeSimpleField("$", true);
				else {
					values = decomposer.decomposeRelationField("$", true);
				}
			} catch (FormulaException e) {
				e.printStackTrace();
			}
			break;
			
		case CustomStrings.RESULT_EVAL_INFO:
		case CustomStrings.RESULT_SAMP_UNIT_IDS:
		case CustomStrings.RESULT_SAMP_MAT_INFO:
			try {
				values = decomposer.decomposeRelationField("$", true);
			} catch (FormulaException e) {
				e.printStackTrace();
			}
			break;
		case CustomStrings.RESULT_SAMPLE_ID:
		case CustomStrings.RESULT_SAMP_AREA:
		case CustomStrings.RESULT_SAMP_DAY:
			try {
				values = decomposer.decomposeRelationField("$", false);
			} catch (FormulaException e) {
				e.printStackTrace();
			}
			break;
		}

		HashMap<String, TableColumnValue> rowValues = new HashMap<>();

		for (String key : values.keySet()) {

			String currentValue = values.get(key);

			TableColumnValue tbv = new TableColumnValue();
			tbv.setCode(currentValue);
			tbv.setLabel(currentValue);

			rowValues.put(key, tbv);
		}

		return rowValues;
	}

	@Override
	public void importDatasetMetadata(Dataset dataset) {
		
		// extract the information from the dataset
		// and insert the report into the database
		this.report = TseReport.fromDataset(dataset);
		report.save();
	}
	
	@Override
	public void importDatasetRows(List<TableRow> rows) throws FormulaException {
		
		// first import the summarized information
		importSummarizedInformation(report, rows);

		// then import cases and results
		importCasesAndResults(report, rows);
	}
}

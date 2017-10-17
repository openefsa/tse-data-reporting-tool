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
	 */
	private void importSummarizedInformation(TseReport report, Collection<TableRow> datasetRows) {

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
	 */
	private void importCasesAndResults(TseReport report, Collection<TableRow> datasetRows) {
		
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
	 */
	private TableRow importCase(TseReport report, SummarizedInfo summInfo, TableRow row) {

		// extract the case from the row
		TableRow currentCaseInfo = extractCase(report, summInfo, row);

		// import the case info if not already imported
		if (currentCaseInfo.getId() == -1) {

			// import case in the db
			currentCaseInfo.save();

			// save the case in the cache by its case id
			String caseId = currentCaseInfo.get(CustomStrings.CASE_INFO_CASE_ID).getCode();
			cases.put(caseId, currentCaseInfo);
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
	 */
	private SummarizedInfo extractSummarizedInfo(TseReport report, TableRow row) {

		// set the summarized information schema
		row.setSchema(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));

		HashMap<String, TableColumnValue> rowValues = new HashMap<>();

		// split compound fields and add them to the summarized information
		rowValues.putAll(decomposeField(CustomStrings.SUMMARIZED_INFO_SAMP_MAT_CODE, row, true));
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

	/**
	 * Extract the case row from the analytical result row
	 * @param report
	 * @param summInfo
	 * @param row
	 * @return
	 */
	private TableRow extractCase(TseReport report, SummarizedInfo summInfo, TableRow row) {

		// set schema (required for next step), we are processing a result row,
		// even if we are extracting the case information data!
		row.setSchema(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
		
		// retrieve the case id from the result row
		HashMap<String, TableColumnValue> evalInfoDecomposed = 
				decomposeField(CustomStrings.RESULT_EVAL_INFO, row, true);

		// get the case id from the row
		String caseId = evalInfoDecomposed.get(CustomStrings.CASE_INFO_CASE_ID).getLabel();

		// create empty case report
		TableRow caseReport;

		// if not already added
		if (cases.get(caseId) == null) {

			// create the case info (we do not copy the data, since this row
			// is actually an analytical result and we just need to 
			// extract the relevant information)
			caseReport = new TableRow(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));

			HashMap<String, TableColumnValue> rowValues = new HashMap<>();

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

			// save sample id
			HashMap<String, TableColumnValue> sampId = 
					decomposeField(CustomStrings.RESULT_SAMPLE_ID, row, true);
			rowValues.putAll(sampId);

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
			caseReport = cases.get(caseId);
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
		
		// at least two pieces
		if (split.length >= 2) {
			
			String[] facets = split[0].split("$");
			
			// get alleles from param
			if (facets.length >= 2) {
				
				TableColumnValue allele1 = new TableColumnValue();
				TableColumnValue allele2 = new TableColumnValue();
				allele1.setCode(facets[0]);
				allele2.setCode(facets[1]);
				
				// decompose param code to get alleles
				rowValues.put(CustomStrings.RESULT_ALLELE_1, allele1);
				rowValues.put(CustomStrings.RESULT_ALLELE_2, allele2);
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

	/**
	 * Decompose a field into its children
	 * @param columnId
	 * @param row
	 * @return hashmap with the children values
	 */
	private HashMap<String, TableColumnValue> decomposeField(String columnId, TableRow row, boolean label) {

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
			values = decomposer.decomposeFoodexCode(false);
			break;
		case CustomStrings.PARAM_CODE_COL:
			values = decomposer.decomposeFoodexCode(true);
			break;
		case CustomStrings.SUMMARIZED_INFO_PROG_INFO:
			values = decomposer.decomposeSimpleField("$", true);
			break;
		case CustomStrings.RESULT_EVAL_INFO:
		case CustomStrings.RESULT_SAMP_UNIT_IDS:
		case CustomStrings.RESULT_SAMP_EVENT_INFO:
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
	public void importDatasetRows(List<TableRow> rows) {
		
		// first import the summarized information
		importSummarizedInformation(report, rows);

		// then import cases and results
		importCasesAndResults(report, rows);
	}
}

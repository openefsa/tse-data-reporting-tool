package tse_case_report;

import table_skeleton.TableRow;
import tse_config.CustomStrings;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

/**
 * Case report object of TSE data collection
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class CaseReport extends TableRow {

	public CaseReport(TableRow row) {
		super(row);
	}

	public CaseReport() {
		super(getCaseSchema());
	}

	public static TableSchema getCaseSchema() {
		return TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET);
	}
}

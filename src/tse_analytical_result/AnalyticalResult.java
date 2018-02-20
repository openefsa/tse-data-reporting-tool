package tse_analytical_result;

import table_skeleton.TableRow;
import tse_config.CustomStrings;
import xlsx_reader.TableSchemaList;

public class AnalyticalResult extends TableRow {

	public AnalyticalResult(TableRow row) {
		super(row);
	}
	
	public AnalyticalResult() {
		super(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
	}
}

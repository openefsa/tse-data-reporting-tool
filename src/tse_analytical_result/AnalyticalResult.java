package tse_analytical_result;

import table_skeleton.TableRow;
import tse_config.CustomStrings;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class AnalyticalResult extends TableRow {

	public AnalyticalResult() {
		super(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
	}

	public AnalyticalResult(TableRow row) {
		super(row);
	}

	/**
	 * the method check if the row is of type Analytical Result
	 * 
	 * @author shahaal
	 * @param row
	 * @return
	 */
	public static boolean isAnalyticalResult(TableRow row) {
		return row.getSchema().equals(getAnlyticalResult());
	}

	/**
	 * get the analytical result type
	 * 
	 * @author shahaal
	 * @return
	 */
	public static TableSchema getAnlyticalResult() {
		return TableSchemaList.getByName(CustomStrings.RESULT_SHEET);
	}
}

package tse_analytical_result;

import java.util.ArrayList;
import java.util.Collection;

import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_report.TseTableRow;
import xlsx_reader.TableSchemaList;

public class AnalyticalResult extends TableRow implements TseTableRow {

	public AnalyticalResult(TableRow row) {
		super(row);
	}
	
	public AnalyticalResult() {
		super(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
	}

	@Override
	public Collection<TseTableRow> getChildren() {
		return new ArrayList<>();
	}
}

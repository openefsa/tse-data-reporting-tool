package tse_analytical_result;

import java.util.ArrayList;
import java.util.Collection;

import table_skeleton.TableRow;
import tse_report.TseTableRow;

public class AnalyticalResult extends TableRow implements TseTableRow {

	public AnalyticalResult(TableRow row) {
		super(row);
	}

	@Override
	public Collection<TseTableRow> getChildren() {
		return new ArrayList<>();
	}
}

package tse_case_report;

import java.util.ArrayList;
import java.util.Collection;

import table_database.TableDao;
import table_skeleton.TableRow;
import tse_analytical_result.AnalyticalResult;
import tse_config.CustomStrings;
import tse_report.TseTableRow;
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
}

package tse_database;

import java.util.ArrayList;
import java.util.Collection;

import table_database.TableDao;
import table_relations.Relation;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_report.Report;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class TseReportDao extends TableDao {

	public TseReportDao() {
		super(Report.getReportSchema());
	}
	
	/**
	 * Get all the report children, that is, summ info,
	 * cases and results. This assumes that each child
	 * contains a reference to the report id it belongs to.
	 * @return
	 */
	public Collection<TableRow> getAllReportChildren(Report report) {
		
		Collection<TableRow> output = new ArrayList<>();
		
		TableSchema[] childrenSchemas = new TableSchema[] {
				TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET),
				TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET),
				TableSchemaList.getByName(CustomStrings.RESULT_SHEET)
		};
		
		int reportId = report.getId();

		// open dao of children and get their rows
		// related to the current report
		for (TableSchema childSchema : childrenSchemas) {
			TableDao dao = new TableDao(childSchema);
			Collection<TableRow> children = dao.getByParentId(CustomStrings.REPORT_SHEET, reportId);
			output.addAll(children);
		}
		
		return output;
	}

}

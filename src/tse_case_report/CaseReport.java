package tse_case_report;

import java.util.ArrayList;
import java.util.Collection;

import table_database.TableDao;
import table_skeleton.TableRow;
import tse_analytical_result.AnalyticalResult;
import tse_config.CustomStrings;
import tse_report.TseTableRow;
import tse_validator.ResultValidator;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

/**
 * Case report object of TSE data collection
 * @author avonva
 *
 */
public class CaseReport extends TableRow implements TseTableRow {

	public CaseReport(TableRow row) {
		super(row);
	}
	
	public CaseReport() {
		super(getCaseSchema());
	}
	
	public static TableSchema getCaseSchema() {
		return TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET);
	}
	
	/**
	 * Get all the results
	 * @return
	 */
	public Collection<TseTableRow> getChildren() {
		
		Collection<TseTableRow> output = new ArrayList<>();
		
		TableSchema caseSchema = TableSchemaList.getByName(CustomStrings.RESULT_SHEET);
		
		TableDao dao = new TableDao();
		Collection<TableRow> children = dao.getByParentId(caseSchema, CustomStrings.CASE_INFO_SHEET, 
				this.getDatabaseId(), true, "desc");
		
		// create it as result
		for (TableRow child : children) {
			output.add(new AnalyticalResult(child));
		}
		
		return output;
	}
	
	public boolean hasResults() {
		return !this.getChildren().isEmpty();
	}
	
	public void updateChildrenErrors() {
		
		// check children errors
		boolean error = false;
		ResultValidator resultValidator = new ResultValidator();
		for (TseTableRow r : this.getChildren()) {
			
			AnalyticalResult result = (AnalyticalResult) r;
			
			if (resultValidator.getWarningLevel(result) > 0) {
				this.setChildrenError();
				error = true;
				break;
			}
		}
		
		if (!error) {
			this.removeChildrenError();
		}
		
		this.update();
	}
}

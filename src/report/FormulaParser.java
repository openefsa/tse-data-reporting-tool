package report;

import java.util.ArrayList;
import java.util.Collection;

import xlsx_reader.ReportTableHeaders.XlsxHeader;

/**
 * Parse the formulas of the row and return them
 * @author avonva
 *
 */
public class FormulaParser {

	private TableRow row;
	

	public FormulaParser(TableRow row) {
		this.row = row;
	}
	
	/**
	 * Get the next formula if present
	 * @param columnField the field of the {@link TableColumn}
	 * which needs to be processed. This is not a row column,
	 * is a field of the column schema, as "mandatory", "editable"...
	 * @return
	 */
	public Formula parse(TableColumn column, String columnField) {

		Formula formula = new Formula(row, column, columnField);
		return formula;
	}
	
	/**
	 * Get the formulas for all the row columns for the chosen
	 * column property selected in {@code columnField}, as defaultValue,
	 * defaultCode.. (see {@link XlsxHeader})
	 * @param columnField
	 * @return
	 */
	public ArrayList<Formula> parse(String columnField) {

		ArrayList<Formula> formulas = new ArrayList<>();

		// for each column of the schema create the formula
		for (TableColumn column : row.getSchema()) {
			formulas.add(new Formula(row, column, columnField));
		}
		
		return formulas;
	}
	
	/**
	 * Get all the columns that can be updated automatically
	 * @return
	 */
	private Collection<TableColumn> getUpdateableFields() {
		
		Collection<TableColumn> cols = new ArrayList<>();

		// for each column of the row
		for (TableColumn column : this.row.getSchema()) {

			// update is not done on editable columns
			// since they are changed directly by
			// the user
			if (this.row.get(column.getId()) != null && column.isEditable())
				continue;
			
			cols.add(column);
		}
		
		return cols;
	}
}

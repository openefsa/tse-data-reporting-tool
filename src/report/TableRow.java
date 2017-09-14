package report;

import java.util.ArrayList;
import java.util.HashMap;

import xlsx_reader.ReportTableHeaders.XlsxHeader;
import xlsx_reader.TableSchema;
import xml_reader.Selection;

/**
 * Generic element of a {@link Report}.
 * @author avonva
 *
 */
public class TableRow {

	public enum RowStatus {
		OK,
		INCOMPLETE,
		ERROR,
	};
	
	private HashMap<String, TableColumnValue> values;
	private TableSchema schema;
	
	/**
	 * Create a report row
	 * @param schema columns properties of the row
	 */
	public TableRow(TableSchema schema) {
		this.values = new HashMap<>();
		this.schema = schema;
		create();
		updateFormulas();
	}
	
	/**
	 * Get a string variable value from the data
	 * @param key
	 * @return
	 */
	public TableColumnValue get(String key) {
		return values.get(key);
	}
	
	/**
	 * Put a selection into the data
	 * @param key
	 * @param value
	 */
	public void put(String key, TableColumnValue value) {
		values.put(key, value);
	}
	
	/**
	 * Put a string into the data, only for raw columns not picklists
	 * use {@link #put(String, Selection)} for picklists
	 * @param key
	 * @param value
	 */
	public void put(String key, String value) {
		
		if (schema.getById(key) != null && schema.getById(key).isPicklist()) {
			System.err.println("Wrong use of ReportRow.put(String,String), "
					+ "use Report.put(String,Selection) instead for picklist columns");
			return;
		}
		
		TableColumnValue row = new TableColumnValue();
		row.setLabel(value);
		values.put(key, row);
	}
	
	private void create() {
		
		// create a slot for each column of the table
		for (TableColumn col : schema) {

			TableColumnValue sel = new TableColumnValue();
			
			sel.setCode(col.getDefaultCode());
			sel.setLabel(col.getDefaultValue());
			
			this.put(col.getId(), sel);
		}
	}
	
	/**
	 * Update the values of the rows applying the columns formulas
	 * (Compute all the automatic values)
	 */
	public void updateFormulas() {
		
		// solve the formula for default code and default value
		FormulaSolver solver = new FormulaSolver(this);
		
		ArrayList<Formula> labels = solver.solveAll(XlsxHeader.DEFAULTVALUE.getHeaderName());
		ArrayList<Formula> codes = solver.solveAll(XlsxHeader.DEFAULTCODE.getHeaderName());
		
		assert(labels.size() == codes.size());

		// save labels into the row
		for (Formula f : labels) {
			
			// do not change editable columns (they do not have
			// formulas and would delete the user values)
			if (f.getColumn().isEditable())
				continue;
			
			TableColumnValue colVal = new TableColumnValue();
			colVal.setLabel(f.getSolvedFormula());
			colVal.setCode(f.getSolvedFormula());
			this.put(f.getColumn().getId(), colVal);
		}
		
		// save codes into the row
		for (Formula f : codes) {
			
			// do not change editable columns (they do not have
			// formulas and would delete the user values)
			// update codes just for picklists
			if (f.getColumn().isEditable() || !f.getColumn().isPicklist())
				continue;
			
			TableColumnValue colVal = this.get(f.getColumn().getId());
			colVal.setCode(f.getSolvedFormula());
			this.put(f.getColumn().getId(), colVal);
		}
	}

	
	public TableSchema getSchema() {
		return schema;
	}
	
	/**
	 * Get the status of the row
	 * @return
	 */
	public RowStatus getStatus() {
		
		RowStatus status = RowStatus.OK;
		
		if (!areMandatoryFilled())
			status = RowStatus.ERROR;
		
		return status;
	}
	
	/**
	 * Check if all the mandatory fields are filled
	 * @return
	 */
	public boolean areMandatoryFilled() {
		
		for (TableColumn column : schema) {
			
			if (column.isMandatory(this) && emptyField(column))
				return false;
		}
		
		return true;
	}
	
	/**
	 * Check if a column value is empty or not
	 * @param col
	 * @return
	 */
	private boolean emptyField(TableColumn col) {
		
		TableColumnValue value = this.get(col.getId());
		
		if (value == null || value.getLabel() == null 
				|| value.getLabel().isEmpty())
			return true;
		
		return false;
	}
	
	@Override
	public String toString() {
		
		StringBuilder print = new StringBuilder();
		
		for (String key : this.values.keySet()) {
			
			print.append("Column: " + key);
			
			// add code for picklists
			if (schema.getById(key) != null && schema.getById(key).isPicklist())
				print.append(" code=" + values.get(key).getCode());
			
			print.append(";value=" + values.get(key).getLabel());

			print.append("\n");
		}
		return print.toString();
	}
}

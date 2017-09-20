package table_skeleton;

import java.util.HashMap;

import duplicates_detector.Checkable;
import table_database.TableDao;
import xlsx_reader.TableHeaders.XlsxHeader;
import xml_catalog_reader.Selection;
import xlsx_reader.TableSchema;

/**
 * Generic element of a {@link Report}.
 * @author avonva
 *
 */
public class TableRow implements Checkable {

	public enum RowStatus {
		OK,
		POSITIVE_MISSING,
		INCONCLUSIVE_MISSING,
		MANDATORY_MISSING,
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
	}
	
	public TableRow(TableSchema schema, String initialColumnId, TableColumnValue initialValue) {
		this(schema);
		this.values.put(initialColumnId, initialValue);
	}
	
	/**
	 * Set the database id
	 * @param id
	 */
	public void setId(int id) {
		
		String index = String.valueOf(id);
		TableColumnValue idValue = new TableColumnValue();
		idValue.setCode(index);
		idValue.setLabel(index);
		
		this.values.put(schema.getTableIdField(), idValue);
	}
	
	/**
	 * Get the database id if present
	 * otherwise return -1
	 * @return
	 */
	public int getId() {
		
		int id = -1;
		
		try {
			
			TableColumnValue value = this.values.get(schema.getTableIdField());
			
			if (value != null && value.getCode() != null)
				id = Integer.valueOf(value.getCode());
			
		} catch (NumberFormatException e) {}
		
		return id;
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
	 * @param label
	 */
	public void put(String key, String label) {
		
		if (schema.getById(key) != null && schema.getById(key).isPicklist()) {
			System.err.println("Wrong use of ReportRow.put(String,String), "
					+ "use Report.put(String,Selection) instead for picklist columns");
			return;
		}
		
		TableColumnValue row = new TableColumnValue();
		row.setCode(label);
		row.setLabel(label);
		values.put(key, row);
	}

	/**
	 * Initialize the row with the default values
	 */
	public void initialize() {
		
		// create a slot for each column of the table
		for (TableColumn col : schema) {

			// skip foreign keys
			if (col.isForeignKey())
				continue;
			
			TableColumnValue sel = new TableColumnValue();
			
			//sel.setCode(col.getDefaultCode());
			//sel.setLabel(col.getDefaultValue());
			
			// set the default values for the editable columns
			// this is done just when the row is created and not
			// when the row is computed, since they are default
			// values with formulas, not real automatic values with formulas
			//if (col.isEditable()) {
				FormulaSolver solver = new FormulaSolver(this);
				Formula code = solver.solve(col, XlsxHeader.DEFAULT_CODE.getHeaderName());
				Formula label = solver.solve(col, XlsxHeader.DEFAULT_VALUE.getHeaderName());

				sel.setCode(code.getSolvedFormula());
				sel.setLabel(label.getSolvedFormula());
			//}

			this.put(col.getId(), sel);
		}
	}
	
	/**
	 * Update the code or the value of the row
	 * using a solved formula
	 * @param f
	 * @param fieldHeader
	 */
	public void update(Formula f, String fieldHeader) {
		
		// skip editable columns
		if (f.getColumn().isEditable())
			return;
		
		XlsxHeader h = XlsxHeader.fromString(fieldHeader);
		
		if (h == null)
			return;
		
		TableColumnValue colVal = this.get(f.getColumn().getId());
		
		if (h == XlsxHeader.CODE_FORMULA && !f.getSolvedFormula().isEmpty()) {
			colVal.setCode(f.getSolvedFormula());
		}
		else if (h == XlsxHeader.LABEL_FORMULA && !f.getSolvedFormula().isEmpty()) {
			colVal.setLabel(f.getSolvedFormula());
			
			// set label in the code if it is empty
			//if (colVal.getCode().isEmpty())
			//	colVal.setCode(f.getSolvedFormula());
		}
		else // else do nothing
			return;

		this.put(f.getColumn().getId(), colVal);
	}
	
	/**
	 * Update the values of the rows applying the columns formulas
	 * (Compute all the automatic values)
	 */
	public void updateFormulas() {
		
		// solve the formula for default code and default value
		FormulaSolver solver = new FormulaSolver(this);
		
		// note that this automatically updates the row
		// while solving formulas
		solver.solveAll(XlsxHeader.CODE_FORMULA.getHeaderName());
		solver.solveAll(XlsxHeader.LABEL_FORMULA.getHeaderName());
	}
	
	/**
	 * Update the row in the database
	 */
	public void save() {
		TableDao dao = new TableDao(this.schema);
		dao.update(this);
	}
	
	/**
	 * Delete permanently the row from the database
	 */
	public void delete() {
		TableDao dao = new TableDao(this.schema);
		dao.delete(this.getId());
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
			status = RowStatus.MANDATORY_MISSING;
		
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
	
	/**
	 * Check if equal
	 */
	public boolean sameAs(Checkable arg0) {
		
		if (!(arg0 instanceof TableRow))
			return false;
		
		TableRow other = (TableRow) arg0;
		
		// cannot compare rows with different schema
		if (!this.schema.equals(other.schema))
			return false;
		
		// for each column of the row
		for (String key : this.values.keySet()) {
			
			// skip the id of the table since we do
			// not have a column for that in the schema
			if (key.equals(schema.getTableIdField()))
				continue;
			
			// get the current column object
			TableColumn col = this.schema.getById(key);
			
			// continue searching if we have not a natural key field
			if (!col.isNaturalKey())
				continue;
			
			// here we are comparing a part of the natural key
			TableColumnValue value1 = this.get(key);
			TableColumnValue value2 = this.get(key);
			
			// if a field of the natural key is
			// different then the two rows are different
			if (!value1.equals(value2))
				return false;
		}
		
		// if we have arrived here, all the natural
		// keys are equal, therefore we have the same row
		return true;
	}
	
	@Override
	public String toString() {
		
		StringBuilder print = new StringBuilder();
		
		for (String key : this.values.keySet()) {
			
			print.append("Column: " + key);

			print.append(" code=" + values.get(key).getCode());
			
			print.append(";value=" + values.get(key).getLabel());

			print.append("\n");
		}
		return print.toString();
	}
}

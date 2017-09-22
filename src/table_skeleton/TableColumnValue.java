package table_skeleton;

import xml_catalog_reader.Selection;

/**
 * Single pair (code,label) contained in a {@link TableRow}
 * it represents a single value assigned to a {@link TableColumn}.
 * @author avonva
 *
 */
public class TableColumnValue {
	
	String code;
	String label;
	
	public TableColumnValue() {
		this.code = "";
		this.label = "";
	}
	
	public TableColumnValue(Selection sel) {
		this.code = sel.getCode();
		this.label = sel.getDescription();
	}
	
	
	public void setCode(String code) {

		this.code = code;
		
		// empty code means that we do not
		// need the field => we set the label
		// as code
		if (code == null || code.isEmpty())
			this.code = label;
	}
	
	public void setLabel(String label) {
		this.label = label;
		
		if (this.label == null)
			this.label = "";
	}
	
	public String getCode() {
		return code;
	}
	
	public String getLabel() {
		return label;
	}
	
	/**
	 * Check if the value is empty
	 * @return
	 */
	public boolean isEmpty() {
		return code.isEmpty() && label.isEmpty();
	}
	
	@Override
	public boolean equals(Object arg0) {
		if (!(arg0 instanceof TableColumnValue))
			return super.equals(arg0);
		
		TableColumnValue other = (TableColumnValue) arg0;
		
		// check code and label
		return (this.getCode().equals(other.getCode())
				&& this.getLabel().equals(other.getLabel()));
	}
	
	@Override
	public String toString() {
		return "TableColumnValue: code=" + code + ";label=" + label;
	}
}
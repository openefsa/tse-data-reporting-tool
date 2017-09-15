package report;

import xml_reader.Selection;

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
	}
	
	public String getCode() {
		return code;
	}
	
	public String getLabel() {
		return label;
	}
	
	@Override
	public String toString() {
		return "TableColumnValue: code=" + code + ";label=" + label;
	}
}
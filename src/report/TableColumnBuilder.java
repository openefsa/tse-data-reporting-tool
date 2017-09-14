package report;

import app_config.BooleanValue;
import report.TableColumn.ColumnType;

public class TableColumnBuilder {

	private String id;            // key which identifies the column
	private String code;          // column code
	private String label;         // column name showed to the user
	private String tip;           // column tip to help the user
	private ColumnType type;      // integer, string, picklist...
	private String mandatory;     // the column needs to be filled?
	private String editable;      // if the user can edit the value
	private String visible;       // if the user can see the column
	private String picklistKey;   // code of the list from which we pick the selectable values
	private String picklistFilter;
	private String defaultCode;   // default code of the column (only for picklists)
	private String defaultValue;  // default value of the column
	private String putInOutput;   // if the column value should be exported in the .xml
	private int order;
	
	public TableColumnBuilder() {
		this.mandatory = BooleanValue.getTrueValue();
		this.editable = BooleanValue.getTrueValue();
		this.type = ColumnType.STRING;
		this.visible = BooleanValue.getTrueValue();
		this.defaultCode = "";
		this.defaultValue = "";
		this.putInOutput = BooleanValue.getTrueValue();
		this.tip = "";
		this.order = 0;
	}
	
	public TableColumnBuilder setId(String id) {
		this.id = id;
		return this;
	}
	
	public TableColumnBuilder setCode(String code) {
		this.code = code;
		return this;
	}
	
	public TableColumnBuilder setLabel(String label) {
		this.label = label;
		return this;
	}
	
	public TableColumnBuilder setTip(String tip) {
		this.tip = tip;
		return this;
	}
	
	public TableColumnBuilder setEditable(String editable) {
		this.editable = editable;
		return this;
	}
	
	public TableColumnBuilder setMandatory(String mandatory) {
		this.mandatory = mandatory;
		return this;
	}
	
	public TableColumnBuilder setVisible(String visible) {
		this.visible = visible;
		return this;
	}
	
	public TableColumnBuilder setDefaultCode(String defaultCode) {
		this.defaultCode = defaultCode;
		return this;
	}
	
	public TableColumnBuilder setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	public TableColumnBuilder setPutInOutput(String putInOutput) {
		this.putInOutput = putInOutput;
		return this;
	}
	
	public TableColumnBuilder setOrder(int order) {
		this.order = order;
		return this;
	}
	
	/**
	 * Set the type of the column. Do not use
	 * this method for {@link ColumnType#PICKLIST}
	 * use {@link #setPicklistKey(String)} instead.
	 * @param type
	 */
	public TableColumnBuilder setType(ColumnType type) {
		this.type = type;
		return this;
	}
	
	public TableColumnBuilder setType(String type) {
		
		ColumnType colType = null;
		
		try {
			colType = ColumnType.fromString(type);
		} catch (IllegalArgumentException e) {
			colType = ColumnType.STRING;
		}

		this.type = colType;
		return this;
	}
	
	/**
	 * Set the key which identifies a list which
	 * contains the values that can be set
	 * to the column (catalogue column). Note
	 * that this will set the type of the column
	 * to {@link ColumnType#PICKLIST}
	 * @param listKey
	 */
	public TableColumnBuilder setPicklistKey(String listKey) {
		this.picklistKey = listKey;
		this.setType(ColumnType.PICKLIST);
		return this;
	}
	
	public void setPicklistFilter(String picklistFilter) {
		this.picklistFilter = picklistFilter;
	}
	
	public TableColumn build() {
		
		TableColumn col = new TableColumn(id, code, label, tip, type, mandatory, editable, 
				visible, defaultCode, defaultValue, putInOutput, order);
		
		if (type == ColumnType.PICKLIST && picklistKey == null) {
			System.err.println("Cannot set type to picklist without specifying list key!");
			return null;
		}
		
		if (type == ColumnType.PICKLIST && picklistKey != null) {
			col.setPicklistKey(picklistKey);
			
			// set the filter if present
			if (picklistFilter != null)
				col.setPicklistFilter(picklistFilter);
		}
		
		return col;
	}
}

package mocks;

import java.util.Collection;

public class Table {
	private String name;
	private Collection<Column> columns;
	
	public Table(String name, Collection<Column> columns) {
		this.name = name;
		this.columns = columns;
	}
	
	public void addColumn(Column col) {
		this.columns.add(col);
	}
	
	public String getName() {
		return name;
	}
	
	public Column getColumn(String colName) {
		for (Column c: columns) {
			if (c.getName().equals(colName))
				return c;
		}
		return null;
	}
	
	public void addContraintTo(String colName, String contraint) {
		
		for (Column c: columns) {
			if (c.getName().equals(colName))
				c.addContraint(contraint);
		}
	}
	
	public Collection<Column> getColumns() {
		return columns;
	}
}

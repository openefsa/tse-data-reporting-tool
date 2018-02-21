package mocks;

import java.util.ArrayList;
import java.util.Collection;

public class DatabaseMock {
	
	private Collection<Table> tables;
	
	public DatabaseMock() {
		this.tables = new ArrayList<>();
	}
	
	public DatabaseMock(Collection<Table> tables) {
		this.tables = tables;
	}
	
	public Collection<Table> getTables() {
		return tables;
	}
	
	public void addTable(Table table) {
		this.tables.add(table);
	}
	
	public Table getTable(String tableName) {
		
		for(Table t: tables) {
			if(t.getName().equals(tableName))
				return t;
		}
		
		return null;
	}
	
	public Column getColumn(String tableName, String colName) {
		Table t = getTable(tableName);
		if (t == null)
			return null;
		
		return t.getColumn(colName);
	}
	
	public void addColumnTo(String tableName, Column col) {
		
		Table t = getTable(tableName);
		
		if (t == null)
			return;
		
		t.addColumn(col);
	}
	
	public void addContraintTo(String tableName, String colName, String contraint) {
		
		Table t = getTable(tableName);

		if (t == null)
			return;

		for (Column c: t.getColumns()) {
			if (c.getName().equals(colName))
				c.addContraint(contraint);
		}
	}
	
	public void removeContraintTo(String tableName, String colName, String contraint) {
		
		Table t = getTable(tableName);

		if (t == null)
			return;

		for (Column c: t.getColumns()) {
			if (c.getName().equals(colName))
				c.removeContraint(contraint);
		}
	}
}

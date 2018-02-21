package mocks;

import java.util.ArrayList;
import java.util.Collection;

import table_skeleton.TableColumn.ColumnType;

public class Column {
	
	private String name;
	private ColumnType type;
	private Collection<String> contraints;
	
	public Column(String name, ColumnType type) {
		this.name = name;
		this.type = type;
		this.contraints = new ArrayList<>();
	}
	
	public void addContraint(String contraint) {
		this.contraints.add(contraint);
	}
	
	public void removeContraint(String contraint) {
		this.contraints.remove(contraint);
	}
	
	public String getName() {
		return name;
	}
	
	public ColumnType getType() {
		return type;
	}
	
	public Collection<String> getContraints() {
		return contraints;
	}
	
	public void setType(ColumnType type) {
		this.type = type;
	}
}

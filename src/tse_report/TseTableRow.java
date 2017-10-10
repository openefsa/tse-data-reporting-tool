package tse_report;

import java.util.Collection;

import xlsx_reader.TableSchema;

public interface TseTableRow {
	public int getId();
	public Collection<TseTableRow> getChildren();
	public int save();
	public TableSchema getSchema();
}

package mocks;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import table_database.ForeignKey;
import table_database.IDatabaseBuilder;
import table_skeleton.TableColumn;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class DatabaseBuilderMock implements IDatabaseBuilder {

	private DatabaseMock db;
	private File tablesSchema;
	
	public DatabaseBuilderMock(File tablesSchema) {
		this.tablesSchema = tablesSchema;
		this.db = new DatabaseMock();
	}
	
	/**
	 * Get the db
	 * @return
	 */
	public DatabaseMock getDb() {
		return db;
	}
	
	@Override
	public void create(String path) throws IOException {
		TableSchemaList schemas = TableSchemaList.getAll(tablesSchema.getPath());
		
		for(TableSchema s: schemas) {
			try {
				createTable(s);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void createTable(TableSchema table) throws SQLException, IOException {
		
		Collection<Column> columns = new ArrayList<>();
		for (TableColumn c: table) {
			columns.add(new Column(c.getId(), c.getType()));
		}
		
		db.addTable(new Table(table.getSheetName(), columns));
	}

	@Override
	public void addColumnToTable(TableSchema schema, TableColumn column) throws IOException, SQLException {
		Column c = new Column(column.getId(), column.getType());
		if (column.isForeignKey())
			c.addContraint("FK");
		db.addColumnTo(schema.getSheetName(), c);
	}

	@Override
	public void addForeignKey(TableSchema schema, TableColumn column) throws IOException, SQLException {
		db.addContraintTo(schema.getSheetName(), column.getId(), "FK");
	}

	@Override
	public void removeForeignKey(TableSchema schema, TableColumn column) throws IOException, SQLException {
		db.removeContraintTo(schema.getSheetName(), column.getId(), "FK");
	}

	@Override
	public ForeignKey getForeignKeyByColumnName(String fkTableName, String foreignKeyColName) throws SQLException {
		
		Column col = db.getTable(fkTableName).getColumn(foreignKeyColName);
		
		for(String c: col.getContraints()) {
			if (c.equals(foreignKeyColName)) {
				return new ForeignKey(fkTableName, foreignKeyColName, c);
			}
		}
		
		return null;
		
	}
}

package database;

import java.io.IOException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import report.TableColumn.ColumnType;
import xlsx_reader.ReportTableHeaders.XlsxHeader;
import xlsx_reader.XlsxReader;

/**
 * This class receives as input an .xlsx file which contains the
 * definitions of the tables/columns of the database, and it creates
 * a query to create these tables/columns with SQL.
 * @author avonva
 *
 */
public class DatabaseStructureCreator extends XlsxReader {

	private StringBuilder query;  // it contains the query to create the database
	private ColumnType currentColumnType;
	private String currentColumnId;
	
	/**
	 * Initialize the creator.
	 * @param filename the .xlsx file which contains the tables schema
	 * @throws IOException
	 */
	public DatabaseStructureCreator(String filename) throws IOException {
		super(filename);
	}

	/**
	 * Get a complete query to generate the database
	 * @return 
	 * @throws IOException
	 */
	public String getQuery() throws IOException {
		
		query = new StringBuilder();
		
		// for each excel sheet create the table with the proper columns
		// which are defined in the columns schema
		for (Sheet sheet : getSheets()) {
			
			// skip the special sheet
			if (RelationParser.isRelationsSheet(sheet.getSheetName()))
				continue;
			
			addTableStatement(sheet.getSheetName());
		}

		return query.toString();
	}

	/**
	 * Add a single table statement with all the columns defined
	 * in the sheet rows
	 * @param sheetName
	 * @throws IOException
	 */
	private void addTableStatement(String sheetName) throws IOException {
		
		// add the "create table" statement
		// using the sheet name as table name
		addCreateStatement(sheetName);
		
		// add the primary key
		addPrimaryKeyStatement(sheetName + "Id");
		
		// add the columns to the statement (also primary key)
		addColumnsStatement(sheetName);
	}
	
	/**
	 * Add the create table statement
	 * @param tableName
	 */
	private void addCreateStatement(String tableName) {
		
		query.append("create table APP.")
			.append(tableName)
			.append("(\n");
	}
	
	/**
	 * Add a primary key to the table, it should be the first variable
	 * @param primaryKeyName
	 */
	private void addPrimaryKeyStatement(String primaryKeyName) {

		query.append(primaryKeyName)
			.append(" integer not null primary key generated always as identity (start with 1, increment by 1),\n");
	}
	
	/**
	 * Add all the columns statement using the id field of the row of the sheet
	 * as column name. All the columns are by default varchar.
	 * @param sheetName
	 * @throws IOException
	 */
	private void addColumnsStatement(String sheetName) throws IOException {
		
		// read the sheet to activate processCell and start/end row methods
		this.read(sheetName);
	}
	
	@Override
	public void processCell(String header, String value) {
		
		XlsxHeader h = null;
		try {
			h = XlsxHeader.fromString(header);  // get enum from string
		}
		catch(IllegalArgumentException e) {
			return;
		}
		
		// we need just the ID to create the table
		if (h == XlsxHeader.ID) {
			this.currentColumnId = value;
		}
		else if (h == XlsxHeader.TYPE) {

			try {
				this.currentColumnType = ColumnType.fromString(value);
			} catch (IllegalArgumentException e) {
				this.currentColumnType = ColumnType.STRING;
			}
		}
	}

	@Override
	public void startRow(Row row) {}

	@Override
	public void endRow(Row row) {

		// append the id name as variable name
		// set the field as string
		if (this.currentColumnType == ColumnType.FOREIGNKEY) {
			query.append(this.currentColumnId)
			.append(" integer not null\n");
		}
		else {
			query.append(this.currentColumnId)
			.append(" varchar(1000)");
		}
		
		int last = row.getSheet().getLastRowNum();
		
		boolean isLast = row.getRowNum() == last;
		
		// if not last row, then add also the
		// comma to be able to add other 
		// columns to the table
		if (!isLast) {
			query.append(",\n");
		}
		else {
			// else if last row, then close the
			// create table statement and put a semicolon
			query.append(");\n\n");
		}
	}
}

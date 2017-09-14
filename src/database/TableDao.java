package database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import report.TableColumnValue;
import report.TableColumn;
import report.TableRow;
import xlsx_reader.TableSchema;
import xml_reader.Selection;
import xml_reader.XmlLoader;

/**
 * Dao which communicates with the database and all the tables
 * that follow a {@link TableSchema}. These tables are automatically
 * generated starting from the excel file, therefore the dao
 * adapts the query using their structure.
 * @author avonva
 *
 */
public class TableDao {

	private String tableName;
	private TableSchema schema;
	
	public TableDao(TableSchema schema) {
		this.tableName = "APP." + schema.getSheetName();
		this.schema = schema;
		this.schema.sortById();
	}
	
	/**
	 * Get the query needed to add a row to the table
	 * @return
	 */
	private String getAddQuery() {
		
		StringBuilder query = new StringBuilder();
		query.append("insert into " + tableName + " (");
		
		// set the columns names
		Iterator<TableColumn> iterator = schema.iterator();
		while (iterator.hasNext()) {
			TableColumn col = iterator.next();
			query.append(col.getId());
			
			// append the comma if there is another field
			if(iterator.hasNext())
				query.append(",");
			else
				query.append(")");  // else, close statement
		}
		
		// values statement
		query.append(" values (");
		
		// add the ?
		iterator = schema.iterator();
		while (iterator.hasNext()) {
			
			// go to the next
			iterator.next();
			
			query.append("?");
			
			// append the comma if there is another field
			if(iterator.hasNext())
				query.append(",");
			else
				query.append(")");  // else, close statement
		}

		return query.toString();
	}
	
	private boolean isRelationId(String id) throws IOException {
		
		if (schema.getRelations() == null)
			return false;
		
		for (Relation r : schema.getRelations()) {
			if(r.getForeignKey().equals(id))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Set the parameter of the statement using the row values
	 * and the table name
	 * @param row
	 * @param stmt
	 * @throws SQLException
	 */
	private void setParameters(TableRow row, PreparedStatement stmt) throws SQLException {
		
		for (int i = 0; i < schema.size(); ++i) {
			
			TableColumn col = schema.get(i);			
			
			String value = null;
			
			TableColumnValue sel = row.get(col.getId());

			if (col.isPicklist() || col.isForeignKey())
				value = sel.getCode();
			else
				value = sel.getLabel();

			// If we have a relation ID => then convert into integer
			try {
				
				if (isRelationId(col.getId()))
					stmt.setInt(1 + i, Integer.valueOf(value));
				else
					stmt.setString(1 + i, value);
				
			} catch (NumberFormatException | IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Add a new row to the table
	 * @param row
	 * @return
	 */
	public boolean add(TableRow row) {
		
		boolean ok = true;
		
		try (Connection con = Database.getConnection(); 
				PreparedStatement stmt = con.prepareStatement(getAddQuery());) {
			
			// set the row values in the parameters
			setParameters(row, stmt);
			
			// insert the element
			stmt.executeUpdate();
			
		} catch (SQLException e) {
			
			e.printStackTrace();
			ok = false;
			
		}
		
		return ok;
	}
	
	/**
	 * Delete all the rows from the table
	 * @param row
	 * @return
	 */
	public boolean removeAll() {
		
		boolean ok = true;
		
		String query = "delete from " + tableName;
		
		try (Connection con = Database.getConnection(); 
				PreparedStatement stmt = con.prepareStatement(query);) {
			
			stmt.executeUpdate();
			
		} catch (SQLException e) {
			e.printStackTrace();
			ok = false;
		}
		
		return ok;
	}
	
	/**
	 * Get a row from the result set
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public TableRow getByResultSet(ResultSet rs) throws SQLException {
		
		TableRow row = new TableRow(schema);
		
		// put the id
		int id = rs.getInt(schema.getTableIdField());
		TableColumnValue sel = new TableColumnValue();
		sel.setCode(String.valueOf(id));
		sel.setLabel(String.valueOf(id));
		row.put(schema.getTableIdField(), sel);
		
		for (TableColumn column : schema) {
			
			TableColumnValue selection = null;
			
			// create foreign key if necessary
			if (column.isForeignKey()) {
				
				// the foreign key is an integer id
				int value = rs.getInt(column.getId());
				
				selection = new TableColumnValue();
				
				// we don't need the description for foreign id
				selection.setCode(String.valueOf(value));
				
				row.put(column.getId(), selection);
			}
			else {
				
				String value = rs.getString(column.getId());
				
				// if we have a picklist, we need both code and description
				if (column.isPicklist()) {
					
					String code = String.valueOf(value);
					
					// get the description from the .xml using the code
					selection = new TableColumnValue(XmlLoader.getByPicklistKey(column.getPicklistKey()).getElementByCode(code));
				}
				else {
					
					// if simple element, then it is sufficient the
					// description (which is the label)
					selection = new TableColumnValue();
					selection.setLabel(String.valueOf(value));
				}
			}
			
			// insert the element into the row
			row.put(column.getId(), selection);
		}
		
		return row;
	}
	
	/**
	 * Get all the rows from the table
	 * @param row
	 * @return
	 */
	public Collection<TableRow> getAll() {
		
		Collection<TableRow> rows = new ArrayList<>();

		String query = "select * from " + tableName;
		
		try (Connection con = Database.getConnection(); 
				PreparedStatement stmt = con.prepareStatement(query);) {
			
			try (ResultSet rs = stmt.executeQuery();) {
				while (rs.next()) {
					
					TableRow row = getByResultSet(rs);
					
					if (row != null)
						rows.add(row);
				}
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return rows;
	}

	/**
	 * Get the row by its id
	 * @param id
	 * @return
	 */
	public TableRow getById(int id) {
		
		TableRow row = null;

		String query = "select * from " + tableName + " where " + schema.getTableIdField() + " = ?";
		
		try (Connection con = Database.getConnection(); 
				PreparedStatement stmt = con.prepareStatement(query);) {
			
			stmt.setInt(1, id);
			
			try (ResultSet rs = stmt.executeQuery();) {
				if (rs.next()) {
					row = getByResultSet(rs);
				}
			}
			catch (SQLException e) {
				e.printStackTrace();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return row;
	}
}

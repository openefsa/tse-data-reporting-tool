package xlsx_reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import app_config.AppPaths;
import database.Relation;
import database.RelationParser;
import report.TableColumn;

public class TableSchema extends ArrayList<TableColumn> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String sheetName;
	private String tableIdField;
	private Collection<Relation> relations;
	
	/**
	 * Set the sheet name related to the schema
	 * @param sheetName
	 */
	public void setSheetName(String sheetName) {
		this.sheetName = sheetName;
		this.tableIdField = sheetName + "Id";
		try {
			this.relations = fetchRelations();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the sheet name related to the schema
	 * @return
	 */
	public String getSheetName() {
		return sheetName;
	}
	
	public String getTableIdField() {
		return tableIdField;
	}
	
	/**
	 * Get all the relations related to the table
	 * @return
	 * @throws IOException
	 */
	private Collection<Relation> fetchRelations() throws IOException {
		
		Collection<Relation> out = new ArrayList<>();
		
		RelationParser parser = new RelationParser(AppPaths.CONFIG_FILE);
		Collection<Relation> rs = parser.read();
		parser.close();

		for (Relation r : rs) {
			if (r.getChild().equals(sheetName))
				out.add(r);
		}
		
		return out;
	}
	
	/**
	 * Get the relationships with other tables
	 * @return
	 */
	public Collection<Relation> getRelations() {
		return relations;
	}
	
	/**
	 * Get the relation which is related to the {@code parentId}
	 * @param parentId
	 * @return
	 */
	public Relation getRelationByParentId(String parentId) {
		for (Relation r : relations) {
			if (r.getParent().equals(parentId))
				return r;
		}
		return null;
	}
	
	/**
	 * Get a column by its key id
	 * @param key
	 * @return
	 */
	public TableColumn getById(String id) {

		for (TableColumn c : this) {
			if (c.equals(id))
				return c;
		}
		
		return null;
	}
	
	/**
	 * Get a list of columns that match the code field
	 * @param code
	 * @return
	 */
	public Collection<TableColumn> getByCode(String code) {

		Collection<TableColumn> out = new ArrayList<>();
		
		for (TableColumn c : this) {
			if (c.getCode().equals(code))
				out.add(c);
		}
		
		return out;
	}
	
	@Override
	public boolean add(TableColumn arg0) {
		
		boolean added = super.add(arg0);
		
		if (added)
			sort();
		
		return added;
	}
	
	/**
	 * Sort columns by sorting id
	 */
	public void sort() {
		Collections.sort(this);
	}
	
	/**
	 * Sort columns by id
	 */
	public void sortById() {
		Collections.sort(this, new Comparator<TableColumn>() {

			@Override
			public int compare(TableColumn arg0, TableColumn arg1) {
				return arg0.getId().compareTo(arg1.getId());
			}
		});
	}
}

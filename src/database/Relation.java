package database;

import java.io.IOException;

import app_config.AppPaths;
import report.TableRow;
import xlsx_reader.SchemaReader;
import xlsx_reader.TableSchema;

public class Relation {

	private String parent;
	private String child;
	
	public Relation(String parent, String child) {
		this.parent = parent;
		this.child = child;
	}
	
	public String getParent() {
		return parent;
	}
	
	public String getChild() {
		return child;
	}
	
	public String getForeignKey() {
		return parent + "Id";
	}
	
	public TableRow getParentValue(int parentId) {
		
		// search in the parent data
		TableDao dao = new TableDao(getParentSchema());
		
		// get the first (and unique) value related to this
		// relation from the parent data
		return dao.getById(parentId);
	}

	/**
	 * get the schema of the parent
	 * @return
	 */
	public TableSchema getParentSchema() {
		
		try {
			
			SchemaReader reader = new SchemaReader(AppPaths.CONFIG_FILE);
			
			reader.read(getParent());
			
			TableSchema schema = reader.getSchema();
			
			reader.close();
			
			return schema;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}

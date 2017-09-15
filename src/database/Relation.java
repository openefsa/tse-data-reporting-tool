package database;

import java.io.IOException;
import java.util.HashMap;

import app_config.AppPaths;
import report.TableRow;
import xlsx_reader.SchemaReader;
import xlsx_reader.TableSchema;

public class Relation {
	
	// caches for each table of the database, using the
	// table name as key
	private static HashMap<String, TableRow> parentValueCache;
	private static HashMap<String, Integer> lastIds;
	
	private String parent;
	private String child;
	
	public Relation(String parent, String child) {
		
		// initialize cache if necessary
		if (parentValueCache == null)
			parentValueCache = new HashMap<>();
		
		// initialize cache if necessary
		if (lastIds == null)
			lastIds = new HashMap<>();
		
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
		return foreignKeyFromParent(parent);
	}
	
	public static String foreignKeyFromParent(String parent) {
		return parent + "Id";
	}
	
	@Override
	public String toString() {
		return "Relation: " + parent + " 1=>N " + child;
	}
	
	/**
	 * Get the value of the parent in the parent table
	 * using the foreign id of the child
	 * @param parentId
	 * @return
	 */
	public TableRow getParentValue(int parentId) {
		
		Integer lastUsedId = lastIds.get(parent);
		
		// if we are not requiring the same parentId
		// update cache
		if (lastUsedId == null || parentId != lastUsedId) {
			
			// search in the parent data
			TableDao dao = new TableDao(getParentSchema());
			
			// get the first (and unique) value related to this
			// relation from the parent data
			parentValueCache.put(parent, dao.getById(parentId));
			lastIds.put(parent, parentId);
		}
		
		// return the cached value
		return parentValueCache.get(parent);
	}
	
	/**
	 * Update the cache of the parent if it was changed externally
	 * @param parentId
	 */
	public static void updateCache(TableRow parentValue) {
		
		String tablename = parentValue.getSchema().getSheetName();
		
		Integer lastUsedId = lastIds.get(tablename);
		
		if (lastUsedId != null && lastUsedId == parentValue.getId()) {
			parentValueCache.put(tablename, parentValue);
		}
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

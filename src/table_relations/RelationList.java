package table_relations;

import java.io.IOException;
import java.util.Collection;

import app_config.AppPaths;

public class RelationList {
	
	private static Collection<Relation> relationsCache;

	/**
	 * Get all the relations contained in the excel sheet {@link AppPaths#RELATIONS_SHEET}
	 * @return
	 * @throws IOException
	 */
	public static Collection<Relation> getAll() throws IOException {
		
		if (relationsCache == null) {
			
			RelationParser parser = new RelationParser(AppPaths.TABLES_SCHEMA_FILE);
			relationsCache = parser.read();
			parser.close();
		}

		return relationsCache;
	}
}

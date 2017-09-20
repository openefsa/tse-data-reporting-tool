package table_database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.poi.ss.usermodel.Row;

import tse_config.AppPaths;
import xlsx_reader.XlsxReader;

/**
 * Parser for the special sheet "Relations" which
 * identifies all the table relationships
 * @author avonva
 *
 */
public class RelationParser extends XlsxReader {
	
	private Collection<Relation> relations;
	private String parentTable;
	private String childTable;
	
	public RelationParser(String filename) throws IOException {
		super(filename);
		this.relations = new ArrayList<>();	
	}
	
	/**
	 * Read all the relations and returns it
	 * @return
	 * @throws IOException
	 */
	public Collection<Relation> read() throws IOException {
		super.read(AppPaths.RELATIONS_SHEET);
		return relations;
	}
	
	public static boolean isRelationsSheet(String sheetName) {
		return AppPaths.RELATIONS_SHEET.equals(sheetName);
	}

	@Override
	public void processCell(String header, String value) {
		
		
		RelationHeader h = null;
		try {
			h = RelationHeader.fromString(header);  // get enum from string
		}
		catch(IllegalArgumentException e) {
			return;
		}

		if(h == null)
			return;
		
		switch(h) {
		case PARENTTABLE:
			this.parentTable = value;
			break;
		case CHILDTABLE:
			this.childTable = value;
			break;
		}
	}

	@Override
	public void startRow(Row row) {}

	@Override
	public void endRow(Row row) {
		
		if(parentTable == null || childTable == null)
			return;
		
		// create a new table relation and put it into the collection
		Relation r = new Relation(parentTable, childTable);
		relations.add(r);
	}
}

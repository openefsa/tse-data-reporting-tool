package table_importer;

import java.util.Collection;

import table_database.TableDao;
import table_relations.Relation;
import table_skeleton.TableRow;
import xlsx_reader.TableSchema;

public class TableImporter {

	/**
	 * Copy all the children of a parent table into the children
	 * of another parent table.
	 * @param childSchema schema of the children
	 * @param parentToCopy parent whose rows will be copied
	 * @param parentToWrite parent whose rows will be replaced by the copied ones
	 */
	public static void copyByParent(TableSchema childSchema, 
			TableRow parentToCopy, TableRow parentToWrite) {
		
		TableDao dao = new TableDao(childSchema);
		
		String parentTable = parentToCopy.getSchema().getSheetName();
		int parentToCopyId = parentToCopy.getId();
		
		// load all the rows of the parent we want to copy
		Collection<TableRow> rowsToCopy = dao.getByParentId(parentTable, parentToCopyId);
		
		// remove all the rows from the parent we want to override
		TableDao writeDao = new TableDao(childSchema);
		int parentToWriteId = parentToWrite.getId();
		writeDao.removeByParentId(parentTable, parentToWriteId);
		
		// for each copied row, insert it into the
		// parentToWrite table
		for (TableRow row : rowsToCopy) {
			
			// set as new parent the parentToWrite parent
			Relation.injectParent(parentToWrite, row);
			
			// add the row
			writeDao.add(row);
		}
	}
}

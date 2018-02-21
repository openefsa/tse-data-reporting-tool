package table_database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import mocks.Column;
import mocks.DatabaseBuilderMock;
import mocks.DatabaseMock;
import mocks.Table;
import tse_config.CustomStrings;

public class DatabaseUpdaterTest {

	private DatabaseBuilderMock dbBuilder;
	
	private File oldSchema;
	private File newSchema;
	
	@Before
	public void init() throws IOException {
		
		oldSchema = new File("test-files" 
				+ System.getProperty("file.separator") + "tablesSchema-old.xlsx");
		
		newSchema = new File("test-files" 
				+ System.getProperty("file.separator") + "tablesSchema-new.xlsx");
		
		this.dbBuilder = new DatabaseBuilderMock(oldSchema);
	}
	
	@Test
	public void testUpdate() throws IOException, SQLException {
		
		// create the old schema
		dbBuilder.create("");
		
		DatabaseMock db = dbBuilder.getDb();
		Table resultSheetBefore = db.getTable(CustomStrings.RESULT_SHEET);
		
		assertNull(resultSheetBefore);
		
		DatabaseUpdater updater = new DatabaseUpdater(dbBuilder);
		updater.update(oldSchema, newSchema);
		
		Table resultSheetAfter = db.getTable(CustomStrings.RESULT_SHEET);
		assertNotNull(resultSheetAfter);
		
		// new column created
		Column evalComCol = db.getColumn(CustomStrings.CASE_INFO_SHEET, "evalCom");
		assertNotNull(evalComCol);
		
		// the fk constraint is added with the new column
		Column fk = db.getColumn(CustomStrings.CASE_INFO_SHEET, "SummarizedInformationId");
		assertNotNull(fk);
		assertEquals(1, fk.getContraints().size());
		assertEquals("FK", fk.getContraints().iterator().next());
		
		// the report id col was removed from the schema, but it is still
		// present into the database. The FK constraint is removed
		Column reportFk = db.getColumn(CustomStrings.CASE_INFO_SHEET, "ReportId");
		assertNotNull(reportFk);
		assertEquals(0, reportFk.getContraints().size());
	}
}

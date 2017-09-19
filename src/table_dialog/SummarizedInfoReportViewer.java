package table_dialog;

import java.io.IOException;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import app_config.AppPaths;
import database.Relation;
import table_skeleton.SummarizedInformationSchema;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import xlsx_reader.TableSchema;
import xml_config_reader.Selection;

/**
 * Class which allows adding and editing a summarized information report.
 * @author avonva
 *
 */
public class SummarizedInfoReportViewer extends ReportViewer {
	
	/**
	 * Create the selector and report table
	 * @param parent
	 */
	public SummarizedInfoReportViewer(Composite parent) {
		super(parent, "TSEs monitoring data (aggregated level)", 
				AppPaths.SUMMARIZED_INFO_SHEET);
	}
	
	/**
	 * Create a new row with default values
	 * @param element
	 * @return
	 * @throws IOException 
	 */
	public TableRow createNewRow(TableSchema schema, Selection element) throws IOException {

		TableRow row = new TableRow(schema);
		
		row.put(SummarizedInformationSchema.TYPE, new TableColumnValue(element));

		// add preferences and settings
		Relation.injectGlobalParent(row, AppPaths.PREFERENCES_SHEET);
		Relation.injectGlobalParent(row, AppPaths.SETTINGS_SHEET);
		
		return row;
	}

	@Override
	public void addMenuItems(Menu menu) {
		// TODO add case report, add random genotyping...
		
	}
}

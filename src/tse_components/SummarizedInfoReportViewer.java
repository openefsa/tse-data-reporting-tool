package tse_components;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.swt.widgets.Shell;

import table_database.Relation;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import tse_config.AppPaths;
import tse_config.SelectionsNames;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Class which allows adding and editing a summarized information report.
 * @author avonva
 *
 */
public class SummarizedInfoReportViewer extends TableDialogWithMenu {
	
	public SummarizedInfoReportViewer(Shell parent) {
		
		super(parent, "", "TSEs monitoring data (aggregated level)", 
				true, true, false, false);
		
		// add 300 px in height
		addDialogHeight(300);
		
		// specify title and list of the selector
		setSelectorLabelText("Add data related to monitoring of:");
		setSelectorList(SelectionsNames.TSE_LIST);
		
		// add the parents of preferences and settings
		try {
			addParentTable(Relation.getGlobalParent(AppPaths.PREFERENCES_SHEET));
			addParentTable(Relation.getGlobalParent(AppPaths.SETTINGS_SHEET));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void setParentFilter(TableRow parentFilter) {
		// enable/disable the selector when a report is opened/closed
		setSelectorEnabled(parentFilter != null);
		super.setParentFilter(parentFilter);
	}

	/**
	 * Create a new row with default values
	 * @param element
	 * @return
	 * @throws IOException 
	 */
	@Override
	public TableRow createNewRow(TableSchema schema, Selection element) {

		TableColumnValue value = new TableColumnValue(element);
		
		// create a new row with the type column already set
		TableRow row = new TableRow(schema, SummarizedInformationSchema.TYPE, value);

		return row;
	}

	@Override
	public String getSchemaSheetName() {
		return AppPaths.SUMMARIZED_INFO_SHEET;
	}

	@Override
	public boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {
		return true;
	}

	@Override
	public Collection<TableRow> loadInitialRows(TableSchema schema, TableRow parentFilter) {
		return null;
	}
}

package table_dialog;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.swt.widgets.Shell;

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
	
	public SummarizedInfoReportViewer(Shell parent) {
		super(parent, "TSEs monitoring data (aggregated level)", "TSEs monitoring data (aggregated level)", 
				true, true, false, false);
	}

	/**
	 * Create a new row with default values
	 * @param element
	 * @return
	 * @throws IOException 
	 */
	@Override
	public TableRow createNewRow(TableSchema schema, Selection element) throws IOException {

		TableRow row = new TableRow(schema);

		row.put(SummarizedInformationSchema.TYPE, new TableColumnValue(element));

		// add preferences and settings
		Relation.injectGlobalParent(row, AppPaths.PREFERENCES_SHEET);
		Relation.injectGlobalParent(row, AppPaths.SETTINGS_SHEET);

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
}

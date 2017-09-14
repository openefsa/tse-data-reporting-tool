package report;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import database.TableDao;
import settings_interface.DataDialog;
import xlsx_reader.TableSchema;

/**
 * Form to create a new report
 * @author avonva
 *
 */
public class ReportCreatorDialog extends DataDialog {
	
	public ReportCreatorDialog(Shell parent) {
		super(parent, "New report", "Creation of a new report", true);
	}
	
	@Override
	public String getSchemaSheet() {
		return AppPaths.REPORT_SHEET;
	}

	@Override
	public Collection<TableRow> loadContents(TableSchema schema) {
		
		Collection<TableRow> rows = new ArrayList<>();
		rows.add(new TableRow(schema));
		return rows;
	}

	@Override
	public void apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {
		
		// TODO check first if the Report is in the DCF

		// add the report to the database
		for (TableRow row : rows) {
			TableDao dao = new TableDao(schema);
			dao.add(row);
		}
	}
}

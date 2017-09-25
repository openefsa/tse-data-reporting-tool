package tse_components;

import java.util.Collection;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import table_database.TableDao;
import table_dialog.TableDialog;
import table_dialog.TableViewWithHelp.RowCreationMode;
import table_skeleton.TableRow;
import tse_config.CustomPaths;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

public class ReportListDialog extends TableDialog {

	private Listener listener;
	
	public ReportListDialog(Shell parent, String title, String message) {
		super(parent, title, message, false, RowCreationMode.NONE, true);
	}

	@Override
	public String getSchemaSheetName() {
		return CustomPaths.REPORT_SHEET;
	}

	@Override
	public Collection<TableRow> loadInitialRows(TableSchema schema, TableRow parentTable) {
		TableDao dao = new TableDao(schema);
		return dao.getAll();
	}

	@Override
	public boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {

		if (selectedRow == null) {
			warnUser("Error", "A report was not selected!");
			return false;
		}
		
		if (listener != null) {
			Event event = new Event();
			event.data = selectedRow;
			listener.handleEvent(event);
		}
		
		return true;
	}
	
	public void setListener(Listener listener) {
		this.listener = listener;
	}

	@Override
	public Menu createMenu() {
		return null;
	}

	@Override
	public TableRow createNewRow(TableSchema schema, Selection type) {
		return null;
	}

	@Override
	public void processNewRow(TableRow row) {}
}

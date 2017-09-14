package user_interface;

import java.util.Collection;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import database.TableDao;
import report.TableRow;
import settings_interface.DataDialog;
import xlsx_reader.TableSchema;

public class ReportListDialog extends DataDialog {

	private Listener listener;
	
	public ReportListDialog(Shell parent, String title, String message) {
		super(parent, title, message, false);
	}

	@Override
	public String getSchemaSheet() {
		return AppPaths.REPORT_SHEET;
	}

	@Override
	public Collection<TableRow> loadContents(TableSchema schema) {
		TableDao dao = new TableDao(schema);
		return dao.getAll();
	}

	@Override
	public void apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {
		if (listener != null) {
			Event event = new Event();
			event.data = selectedRow;
			listener.handleEvent(event);
		}
	}
	
	public void setListener(Listener listener) {
		this.listener = listener;
	}
}

package settings_interface;

import java.util.Collection;

import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import database.TableDao;
import report.TableRow;
import xlsx_reader.TableSchema;

public class PreferencesDialog extends DataDialog {

	public PreferencesDialog(Shell parent) {
		super(parent, "Tse preferences", "Tse preferences", true);
	}

	@Override
	public String getSchemaSheet() {
		return AppPaths.PREFERENCES_SHEET;
	}

	@Override
	public Collection<TableRow> loadContents(TableSchema schema) {
		
		TableDao dao = new TableDao(schema);
		Collection<TableRow> objs = dao.getAll();
		
		// if no options were set, add an empty row
		if (objs.isEmpty())
			objs.add(new TableRow(schema));
		
		return objs;
	}

	@Override
	public void apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {
		
		// update preferences
		TableDao dao = new TableDao(schema);
		dao.removeAll();
		
		for (TableRow row : rows)
			dao.add(row);
	}
}

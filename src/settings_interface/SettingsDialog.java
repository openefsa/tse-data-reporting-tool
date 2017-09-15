package settings_interface;

import java.util.Collection;

import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import database.Relation;
import database.TableDao;
import report.TableRow;
import xlsx_reader.TableSchema;

/**
 * Dialog which is used to set global settings for the application
 * @author avonva
 *
 */
public class SettingsDialog extends DataDialog {
	
	private OptionsChangedListener listener;
	
	public SettingsDialog(Shell parent) {
		super(parent, "User settings", "User settings", true);
	}
	
	public void setListener(OptionsChangedListener listener) {
		this.listener = listener;
	}

	@Override
	public String getSchemaSheet() {
		return AppPaths.SETTINGS_SHEET;
	}

	@Override
	public Collection<TableRow> loadContents(TableSchema schema) {
		
		TableDao dao = new TableDao(schema);
		Collection<TableRow> objs = dao.getAll();
		
		// if no options were set, add an empty row
		// to the db, get 
		if (objs.isEmpty()) {
			
			// add a new row
			TableRow row = new TableRow(schema);
			int id = dao.add(row);
			row.setId(id);
			
			objs.add(row);
		}
		
		return objs;
	}

	@Override
	public void apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {
		
		// update settings
		TableDao dao = new TableDao(schema);
		dao.update(selectedRow);
		
		// update the cache of the relations
		Relation.updateCache(selectedRow);
		
		if (listener != null)
			listener.optionChanged(selectedRow);
	}
}

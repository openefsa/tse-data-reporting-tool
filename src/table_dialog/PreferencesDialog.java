package table_dialog;

import java.io.IOException;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import table_skeleton.TableRow;
import xlsx_reader.TableSchema;
import xml_config_reader.Selection;

/**
 * Preferences of the report
 * @author avonva
 *
 */
public class PreferencesDialog extends OptionsDialog {
	
	public PreferencesDialog(Shell parent) {
		super(parent, "Tse preferences", "Tse preferences", true);
	}
	
	@Override
	public String getSchemaSheetName() {
		return AppPaths.PREFERENCES_SHEET;
	}

	@Override
	public Menu createMenu() {
		return null;
	}

	@Override
	public TableRow createNewRow(TableSchema schema, Selection type) throws IOException {
		return null;
	}
}

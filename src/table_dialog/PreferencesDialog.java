package table_dialog;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;

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
}

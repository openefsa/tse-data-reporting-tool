package tse_config;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import table_dialog.RowValidatorLabelProvider;
import table_skeleton.TableRow;
import tse_validator.SimpleRowValidatorLabelProvider;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

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
		return CustomPaths.PREFERENCES_SHEET;
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

	@Override
	public RowValidatorLabelProvider getValidator() {
		return new SimpleRowValidatorLabelProvider();
	}
}

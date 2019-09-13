package tse_options;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import i18n_messages.TSEMessages;
import table_dialog.DialogBuilder;
import table_dialog.RowValidatorLabelProvider;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_validator.SimpleRowValidatorLabelProvider;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Preferences of the report
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class PreferencesDialog extends OptionsDialog {
	
	public static final String WINDOW_CODE = "Preferences";
	
	public PreferencesDialog(Shell parent) {
		super(parent, TSEMessages.get("pref.title"), WINDOW_CODE);
	}
	
	@Override
	public String getSchemaSheetName() {
		return CustomStrings.PREFERENCES_SHEET;
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

	@Override
	public void addWidgets(DialogBuilder viewer) {
		viewer.addHelp(TSEMessages.get("pref.help.title"))
			.addTable(CustomStrings.PREFERENCES_SHEET, true);
	}

	@Override
	public void nextLevel() {
		// TODO Auto-generated method stub
		
	}
}

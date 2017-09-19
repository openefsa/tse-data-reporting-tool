package table_dialog;

import javax.xml.soap.SOAPException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import app_config.PropertiesReader;
import dataset.Dataset;
import dataset.DatasetList;
import webservice.GetDatasetList;

/**
 * Dialog which is used to set global settings for the application
 * @author avonva
 *
 */
public class SettingsDialog extends OptionsDialog {

	public SettingsDialog(Shell parent) {
		super(parent, "User settings", "User settings", true);
	}

	@Override
	public String getSchemaSheetName() {
		return AppPaths.SETTINGS_SHEET;
	}

	@Override
	public Menu createMenu() {
		
		Menu menu = new Menu(getDialog());
		MenuItem test = new MenuItem(menu, SWT.PUSH);
		test.setText("Test connection");
		
		test.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				try {

					// change the cursor to wait
					getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
					
					// get dataset list
					GetDatasetList request = new GetDatasetList(PropertiesReader.getDataCollectionCode());
					DatasetList list = request.getlist();
					
					// get the dataset TEST
					Dataset dataset = list.getBySenderId("TEST");
					
					if (dataset != null)
						warnUser("Ok", "Test successfully completed.", SWT.OK);
					else
						warnUser("Error", "Cannot retrieve the TEST dataset");

					// change the cursor to old cursor
					getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
					
				} catch (SOAPException e) {
					warnUser("Error", "Cannot get the TEST dataset. Check your credentials and connection.");
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		return menu;
	}
}

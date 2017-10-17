package tse_options;

import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import app_config.PropertiesReader;
import dataset.Dataset;
import dataset.DatasetList;
import global_utils.Warnings;
import table_dialog.DialogBuilder;
import table_dialog.RowValidatorLabelProvider;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_validator.SimpleRowValidatorLabelProvider;
import user.User;
import webservice.GetDatasetList;
import webservice.MySOAPException;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Dialog which is used to set global settings for the application
 * @author avonva
 *
 */
public class SettingsDialog extends OptionsDialog {

	public SettingsDialog(Shell parent) {
		super(parent, "User settings");
	}

	@Override
	public String getSchemaSheetName() {
		return CustomStrings.SETTINGS_SHEET;
	}
	
	@Override
	public boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {

		boolean closeWindow = super.apply(schema, rows, selectedRow);
		
		TableRow settings = rows.iterator().next();
		
		if (settings == null)
			return closeWindow;
		
		login(settings);
		
		return closeWindow;
	}

	private void login(TableRow settings) {
		
		// get credentials
		TableColumnValue usernameVal = settings.get(CustomStrings.SETTINGS_USERNAME);
		TableColumnValue passwordVal = settings.get(CustomStrings.SETTINGS_PASSWORD);

		if (usernameVal == null || passwordVal == null)
			return;

		// login the user
		String username = usernameVal.getLabel();
		String password = passwordVal.getLabel();

		User.getInstance().login(username, password);
	}
	
	@Override
	public Menu createMenu() {
		
		Menu menu = new Menu(getDialog());
		MenuItem test = new MenuItem(menu, SWT.PUSH);
		test.setText("Test connection");
		
		// check if it is possible to make a get dataset
		// list request and possibly download the
		// test report if present. If not present, it
		// creates the test report and send it to the dcf
		test.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				System.out.println("Test connection: started");
				
				// login the user if not done before
				login(getRows().iterator().next());
				
				// change the cursor to wait
				getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
				
				String title = null;
				String message = null;
				DatasetList<Dataset> list = new DatasetList<Dataset>();
				try {
					
					// get dataset list of the data collection (in test)
					GetDatasetList request = new GetDatasetList(PropertiesReader.getTestDataCollectionCode());
					list = request.getList();
					
				} catch (MySOAPException e) {

					e.printStackTrace();
					
					System.err.println("Test connection: failed.");

					String[] warnings = Warnings.getSOAPWarning(e.getError());
					title = warnings[0];
					message = warnings[1];
				}
				finally {
					// change the cursor to old cursor
					getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
				}
				
				// if we have an error message stop and show the error
				if (message != null) {
					warnUser(title, message);
					return;
				}
				
				// search the test dataset
				String testReportCode = PropertiesReader.getTestReportCode();
				
				// get the dataset of TEST
				DatasetList<Dataset> testList = list.filterBySenderId(testReportCode);
				Dataset dataset = testList.isEmpty() ? null : testList.get(0);
				
				if (dataset != null) {
					System.out.println("Test connection: completed");
					warnUser("Ok", "Test successfully completed.", SWT.OK);
				}
				else {
					System.err.println("Test connection: failed. " + testReportCode + " report cannot be found in the DCF");
					warnUser("Error", "ERR406: " + testReportCode + ": Cannot retrieve the dataset");
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		return menu;
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
		viewer.addHelp("User settings")
			.addTable(CustomStrings.SETTINGS_SHEET, true);
	}
}

package tse_components;

import java.io.IOException;

import javax.xml.soap.SOAPException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import dataset.Dataset;
import dataset.DatasetList;
import table_skeleton.TableRow;
import tse_config.AppPaths;
import tse_config.PropertiesReader;
import webservice.GetDatasetList;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

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
		
		// check if it is possible to make a get dataset
		// list request and possibly download the
		// test report if present. If not present, it
		// creates the test report and send it to the dcf
		test.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				try {

					System.out.println("Test connection: started");
					
					// change the cursor to wait
					getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
					
					// get dataset list
					GetDatasetList request = new GetDatasetList(PropertiesReader.getDataCollectionCode());
					DatasetList list = request.getlist();
					
					// change the cursor to old cursor
					getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
					
					String testReportCode = PropertiesReader.getTestReportCode();
					
					// get the dataset of TEST
					Dataset dataset = list.getBySenderId(testReportCode);
					
					if (dataset != null) {
						System.out.println("Test connection: completed");
						warnUser("Ok", "Test successfully completed.", SWT.OK);
					}
					else {
						System.err.println("Test connection: failed. " + testReportCode + " report cannot be found in the DCF");
						warnUser("Error", testReportCode + ": Cannot retrieve the dataset");
					}
				} catch (SOAPException e) {
					
					System.err.println("Test connection: failed. See exception for details");
					e.printStackTrace();
					
					// change the cursor to old cursor
					getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
					
					warnUser("Error", "Connection error. Check your credentials and connection.");
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
}

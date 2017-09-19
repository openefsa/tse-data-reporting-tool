package user_interface;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import app_config.PropertiesReader;
import database.Database;
import database.TableDao;
import table_dialog.PreferencesDialog;
import table_dialog.SettingsDialog;
import table_skeleton.TableRow;
import xlsx_reader.TableSchema;

public class StartUI {

	/**
	 * Check if the mandatory fields of a generic settings table are filled or not
	 * @param tableName
	 * @return
	 * @throws IOException
	 */
	private static boolean checkSettings(String tableName) throws IOException {
		
		TableDao dao = new TableDao(TableSchema.load(tableName));
		
		Collection<TableRow> data = dao.getAll();
		
		if(data.isEmpty())
			return false;
		
		TableRow firstRow = data.iterator().next();
		
		// check if the mandatory fields are filled or not
		return firstRow.areMandatoryFilled();
	}
	
	/**
	 * Check if the settings were set or not
	 * @return
	 * @throws IOException
	 */
	private static boolean checkSettings() throws IOException {
		return checkSettings(AppPaths.SETTINGS_SHEET);
	}
	
	/**
	 * Check if the preferences were set or not
	 * @return
	 * @throws IOException
	 */
	private static boolean checkPreferences() throws IOException {
		return checkSettings(AppPaths.PREFERENCES_SHEET);
	}
	
	/**
	 * Close the application
	 * @param db
	 * @param display
	 */
	private static void shutdown(Database db, Display display) {

		display.dispose();
		
		// close the database
		db.shutdown();
		
		// exit the application
		System.exit(0);
	}
	
	public static void main(String args[]) throws IOException {
		
		// application start-up message. Usage of System.err used for red chars
		System.out.println("Application Started " + System.currentTimeMillis());
		
		// connect to the database application
		Database db = new Database();
		db.connect();
		
		Display display = new Display();
		Shell shell = new Shell(display);
		
		// set the application name in the shell
		shell.setText(PropertiesReader.getAppName() + " " + PropertiesReader.getAppVersion());
		
		// check preferences
		if (!checkPreferences()) {
			PreferencesDialog pref = new PreferencesDialog(shell);
			pref.open();
			
			// if the preferences were not set
			if (pref.getStatus() == SWT.CANCEL) {
				// close the application
				shutdown(db, display);
			}
		}
		
		// check settings
		if (!checkSettings()) {
			SettingsDialog settings = new SettingsDialog(shell);
			settings.open();
			
			// if the settings were not set
			if (settings.getStatus() == SWT.CANCEL) {
				// close the application
				shutdown(db, display);
			}
		}
		
		
		MainPanel panel = new MainPanel(shell);
	    shell.open();
		
		// Event loop
		while ( !shell.isDisposed() ) {
			if ( !display.readAndDispatch() )
				display.sleep();
		}

		// close the application
		shutdown(db, display);
	}
}

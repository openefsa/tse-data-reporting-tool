package tse_components;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import app_config.PropertiesReader;
import html_viewer.HtmlViewer;
import table_database.Database;
import table_database.TableDao;
import table_skeleton.TableRow;
import tse_config.CustomPaths;
import xlsx_reader.TableSchemaList;

public class StartUI {

	/**
	 * Check if the mandatory fields of a generic settings table are filled or not
	 * @param tableName
	 * @return
	 * @throws IOException
	 */
	private static boolean checkSettings(String tableName) throws IOException {

		TableDao dao = new TableDao(TableSchemaList.getByName(tableName));
		
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
		return checkSettings(CustomPaths.SETTINGS_SHEET);
	}
	
	/**
	 * Check if the preferences were set or not
	 * @return
	 * @throws IOException
	 */
	private static boolean checkPreferences() throws IOException {
		return checkSettings(CustomPaths.PREFERENCES_SHEET);
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
		
		
		new MainPanel(shell);
		
		// set the application icon into the shell
		Image image = new Image(Display.getCurrent(), 
				ClassLoader.getSystemResourceAsStream(PropertiesReader.getAppIcon()));

		if (image != null)
			shell.setImage(image);
		
		// open the shell to the user
	    shell.open();
	    
	    // open also an help view for showing general help
	    File helpFile = new File(AppPaths.HELP_FOLDER + PropertiesReader.getStartupHelpFileName());
	    HtmlViewer help = new HtmlViewer();
	    //TODO help.open(helpFile);
		
		// Event loop
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		// close the application
		shutdown(db, display);
	}
}

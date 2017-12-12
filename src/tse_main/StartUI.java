package tse_main;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import app_config.PropertiesReader;
import global_utils.EFSARCL;
import global_utils.Warnings;
import html_viewer.HtmlViewer;
import i18n_messages.TSEMessages;
import table_database.Database;
import table_database.DatabaseVersionException;
import table_database.TableDao;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_config.DebugConfig;
import tse_config.GeneralWarnings;
import tse_options.PreferencesDialog;
import tse_options.SettingsDialog;
import user.User;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class StartUI {

	private static Display display;
	private static Shell shell;
	
	/**
	 * Check if the mandatory fields of a generic settings table are filled or not
	 * @param tableName
	 * @return
	 * @throws IOException
	 */
	private static boolean checkSettings(String tableName) {

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
	private static boolean checkSettings() {
		return checkSettings(CustomStrings.SETTINGS_SHEET);
	}
	
	/**
	 * Check if the preferences were set or not
	 * @return
	 * @throws IOException
	 */
	private static boolean checkPreferences() {
		return checkSettings(CustomStrings.PREFERENCES_SHEET);
	}
	
	/**
	 * Login the user into the system in order to be able to
	 * perform web-service calls
	 */
	private static void loginUser() {
		
		// get the settings schema table
		TableSchema settingsSchema = TableSchemaList.getByName(CustomStrings.SETTINGS_SHEET);
		
		TableDao dao = new TableDao(settingsSchema);
		
		// get the settings
		TableRow settings = dao.getAll().iterator().next();
		
		if (settings == null)
			return;
		
		// get credentials
		TableColumnValue usernameVal = settings.get(CustomStrings.SETTINGS_USERNAME);
		TableColumnValue passwordVal = settings.get(CustomStrings.SETTINGS_PASSWORD);
		
		if (usernameVal == null || passwordVal == null)
			return;
		
		// login the user
		String username = usernameVal.getLabel();
		String password = passwordVal.getLabel();
		
		User user = User.getInstance();
		user.login(username, password);
	}
	
	/**
	 * Close the application (db + interface)
	 * @param db
	 * @param display
	 */
	private static void shutdown(Database db, Display display) {
		
		System.out.println("Application closed " + System.currentTimeMillis());

		if (display != null)
			display.dispose();
		
		// close the database
		db.shutdown();
		
		// exit the application
		System.exit(0);
	}
	
	/**
	 * Show an error to the user
	 * @param errorCode
	 * @param message
	 */
	private static void showInitError(String message) {
		Display display = new Display();
		Shell shell = new Shell(display);
		Warnings.warnUser(shell, TSEMessages.get("error.title"), message);
		
		shell.dispose();
		display.dispose();
	}
	
	private static int ask(String message) {
		Display display = new Display();
		Shell shell = new Shell(display);
		int val = Warnings.warnUser(shell, TSEMessages.get("warning.title"), 
				message, 
				SWT.YES | SWT.NO | SWT.ICON_WARNING);
		
		shell.dispose();
		display.dispose();
		
		return val;
	}
	
	
	/**
	 * Create the main panel for the user interface
	 * @return
	 */
	private static Shell createUi() {
		
		display = new Display();
		shell = new Shell(display);
		
		// set the application name in the shell
		shell.setText(PropertiesReader.getAppName() + " " + PropertiesReader.getAppVersion());
		
		// open the main panel
		try {
			new MainPanel(shell);
		}
		catch (Exception e) {
			e.printStackTrace();
			GeneralWarnings.showExceptionStack(shell, e);
		}
		
		// set the application icon into the shell
		Image image = new Image(Display.getCurrent(), 
				ClassLoader.getSystemResourceAsStream(PropertiesReader.getAppIcon()));

		if (image != null)
			shell.setImage(image);
		
		return shell;
	}
	
	/**
	 * Start the TSE data reporting tool interface & database
	 * @param args
	 */
	public static void main(String args[]) {
		
		// application start-up message. Usage of System.err used for red chars
		System.out.println("Application started " + System.currentTimeMillis());
		
		// connect to the database application
		Database db = new Database();
		try {
			db.connect();
		} catch (IOException e) {
			e.printStackTrace();
			showInitError(TSEMessages.get("db.init.error", e.getMessage()));
			return;
		}
		
		try {
			
			// initialize the library
			EFSARCL.init();
			
			// check also custom files
			EFSARCL.checkConfigFiles(CustomStrings.PREDEFINED_RESULTS_FILE, 
					AppPaths.CONFIG_FOLDER);
			
		} catch (IOException | SQLException e) {
			e.printStackTrace();
			showInitError(TSEMessages.get("efsa.rcl.init.error", e.getMessage()));
			return;
		} catch (DatabaseVersionException e) {
			e.printStackTrace();
			
			int val = ask(TSEMessages.get("db.need.removal"));
			
			// close application
			if (val == SWT.NO)
				shutdown(db, display);
			else {
				
				// delete the database
				try {
					
					// delete the old database
					db.delete();
					
					// reconnect to the database and
					// create a new one
					db.connect();
					
				} catch (IOException e1) {
					e1.printStackTrace();
					
					showInitError(TSEMessages.get("db.removal.error"));
					
					shutdown(db, display);
				}
			}
		}

		// create the main panel
		createUi();
	    
	    // open also an help view for showing general help
	    if (!DebugConfig.debug) {
		    HtmlViewer help = new HtmlViewer();
		    help.open(PropertiesReader.getStartupHelpURL());
	    }
		
		// open the shell to the user
	    shell.open();
		
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
		else {
			// if settings are not opened, then login the user
			// with the current credentials
			loginUser();
		}
		
		// Event loop
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		// close the application
		shutdown(db, display);
	}
}

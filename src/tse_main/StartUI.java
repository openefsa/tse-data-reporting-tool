package tse_main;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import app_config.PropertiesReader;
import dataset.IDataset;
import global_utils.EFSARCL;
import global_utils.FileUtils;
import global_utils.Warnings;
import html_viewer.HtmlViewer;
import i18n_messages.TSEMessages;
import providers.FormulaService;
import providers.IFormulaService;
import providers.ITableDaoService;
import providers.TableDaoService;
import providers.TseReportService;
import soap.GetAck;
import soap.GetDataset;
import soap.GetDatasetsList;
import soap.SendMessage;
import soap_interface.IGetAck;
import soap_interface.IGetDataset;
import soap_interface.IGetDatasetsList;
import soap_interface.ISendMessage;
import table_database.Database;
import table_database.DatabaseVersionException;
import table_database.ITableDao;
import table_database.TableDao;
import table_skeleton.TableCell;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_config.DebugConfig;
import tse_options.PreferencesDialog;
import tse_options.SettingsDialog;
import user.DcfUser;
import user.User;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class StartUI {

	private static final Logger LOGGER = LogManager.getLogger(StartUI.class);

	private static Display display;
	private static Shell shell;

	/**
	 * Check if the mandatory fields of a generic settings table are filled or not
	 * @param tableName
	 * @return
	 * @throws IOException
	 */
	private static boolean checkSettings(String tableName) {

		TableDao dao = new TableDao();

		Collection<TableRow> data = dao.getAll(TableSchemaList.getByName(tableName));

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

		TableDao dao = new TableDao();

		// get the settings
		TableRow settings = dao.getAll(settingsSchema).iterator().next();

		if (settings == null)
			return;

		// get credentials
		TableCell usernameVal = settings.get(CustomStrings.SETTINGS_USERNAME);
		TableCell passwordVal = settings.get(CustomStrings.SETTINGS_PASSWORD);

		if (usernameVal == null || passwordVal == null)
			return;

		// login the user
		String username = usernameVal.getLabel();
		String password = passwordVal.getLabel();

		DcfUser user = User.getInstance();
		user.login(username, password);
	}

	/**
	 * Close the application (db + interface)
	 * @param db
	 * @param display
	 */
	private static void shutdown(Database db, Display display) {
		
		LOGGER.info("Application closed " + System.currentTimeMillis());

		if (display != null)
			display.dispose();

		// close the database
		if (db != null)
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
	 * Start the TSE data reporting tool interface & database
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String args[]) throws IOException {
		
		try {
			Database db = launch();
			shutdown(db, display);
		}
		catch (Throwable e) {
			e.printStackTrace();
			LOGGER.fatal("Generic error occurred", e);
			
			Warnings.createFatal(TSEMessages.get("generic.error", 
					PropertiesReader.getSupportEmail())).open(shell);
		}
	}

	private static Database launch() {
		
		// application start-up message. Usage of System.err used for red chars
		LOGGER.info("Application started " + System.currentTimeMillis());

		// connect to the database application
		Database db = new Database();

		try {
			db.connect();
		} catch (IOException e) {
			LOGGER.error("Database not found or incompatible", e);
			showInitError(TSEMessages.get("db.init.error", e.getMessage()));
			return null;
		}

		try {

			FileUtils.createFolder(CustomStrings.PREFERENCE_FOLDER);

			// initialize the library
			EFSARCL.init();

			// check also custom files
			EFSARCL.checkConfigFiles(CustomStrings.PREDEFINED_RESULTS_FILE, 
					AppPaths.CONFIG_FOLDER);

		} catch (IOException | SQLException e) {
			LOGGER.fatal("Cannot initialize the EFSARCL library and accessory files", e);
			showInitError(TSEMessages.get("efsa.rcl.init.error", e.getMessage()));
			return db;
		} catch (DatabaseVersionException e) {
			
			LOGGER.warn("Old version of the database found", e);

			int val = ask(TSEMessages.get("db.need.removal"));

			// close application
			if (val == SWT.NO)
				return db;
			else {

				// delete the database
				try {

					// delete the old database
					db.delete();

					// reconnect to the database and
					// create a new one
					db.connect();

				} catch (IOException e1) {
					LOGGER.fatal(e1);
					showInitError(TSEMessages.get("db.removal.error"));

					return db;
				}
			}
		}

		// create the main panel
		display = new Display();
		shell = new Shell(display);

		// set the application name in the shell
		shell.setText(PropertiesReader.getAppName() + " " + PropertiesReader.getAppVersion());

		// init services
		ITableDao dao = new TableDao();
		ITableDaoService daoService = new TableDaoService(dao);
		IFormulaService formulaService = new FormulaService(daoService);
		
		IGetAck getAck = new GetAck();
		IGetDatasetsList<IDataset> getDatasetsList = new GetDatasetsList<>();
		ISendMessage sendMessage = new SendMessage();
		IGetDataset getDataset = new GetDataset();
		
		TseReportService reportService = new TseReportService(getAck, getDatasetsList, 
				sendMessage, getDataset, daoService, formulaService);
		
		// open the main panel
		
		try {
			new MainPanel(shell, reportService, daoService);
		}
		catch (Throwable e) {
			e.printStackTrace();
			LOGGER.fatal("Generic error occurred", e);
			
			Warnings.createFatal(TSEMessages.get("generic.error", 
					PropertiesReader.getSupportEmail())).open(shell);
			
			return null;
		}

		// set the application icon into the shell
		Image image = new Image(Display.getCurrent(), 
				ClassLoader.getSystemResourceAsStream(PropertiesReader.getAppIcon()));

		if (image != null)
			shell.setImage(image);

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
				return db;
			}
		}

		// check settings
		if (!checkSettings()) {
			SettingsDialog settings = new SettingsDialog(shell, reportService, daoService);
			settings.open();

			// if the settings were not set
			if (settings.getStatus() == SWT.CANCEL) {
				// close the application
				return db;
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

		return db;
	}
}

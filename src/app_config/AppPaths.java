package app_config;

public class AppPaths {
	/**
	 * Path where all the .xml configuration files are stored
	 */
	public static final String XML_FOLDER = "data" + System.getProperty("file.separator");
	public static final String CONFIG_FOLDER = "config" + System.getProperty("file.separator");
	public static final String CONFIG_FILE = CONFIG_FOLDER + "config.xlsx";
	
	public static final String DB_FOLDER = "database" + System.getProperty("file.separator");
	
	public static final String SUMMARIZED_INFO_SHEET = "SummarizedInformation";
	public static final String PREFERENCES_SHEET = "Preferences";
	public static final String SETTINGS_SHEET = "Settings";
	public static final String REPORT_SHEET = "Report";
	public static final String RELATIONS_SHEET = "Relations";
}

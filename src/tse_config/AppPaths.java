package tse_config;

public class AppPaths {
	/**
	 * Path where all the .xml configuration files are stored
	 */
	public static final String XML_FOLDER = "data" + System.getProperty("file.separator");
	public static final String CONFIG_FOLDER = "config" + System.getProperty("file.separator");
	public static final String HELP_FOLDER = "help" + System.getProperty("file.separator");
	
	public static final String TABLES_SCHEMA_FILE = CONFIG_FOLDER + "tablesSchema.xlsx";
	public static final String APP_CONFIG_FILE = CONFIG_FOLDER + "appConfig.xml";
	
	public static final String DB_FOLDER = "database" + System.getProperty("file.separator");
	
	public static final String CASE_INFO_SHEET = "CasesInformation";
	public static final String SUMMARIZED_INFO_SHEET = "SummarizedInformation";
	public static final String PREFERENCES_SHEET = "Preferences";
	public static final String SETTINGS_SHEET = "Settings";
	public static final String SETTINGS_USERNAME = "username";
	public static final String SETTINGS_PASSWORD = "password";
	public static final String SETTINGS_ORG_CODE = "orgCode";
	public static final String REPORT_SHEET = "Report";
	public static final String REPORT_YEAR = "reportYear";
	public static final String REPORT_MONTH = "reportMonth";
	public static final String REPORT_SENDER_ID = "reportSenderId";
	public static final String RELATIONS_SHEET = "Relations";
	public static final String HELP_SHEET = "Help";
}

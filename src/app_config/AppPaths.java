package app_config;

public class AppPaths {
	
	public static final String XML_FOLDER = "data" + System.getProperty("file.separator");
	public static final String CONFIG_FOLDER = "config" + System.getProperty("file.separator");
	public static final String HELP_FOLDER = "help" + System.getProperty("file.separator");
	public static final String TEMP_FOLDER = "temp" + System.getProperty("file.separator");
	
	public static final String TABLES_SCHEMA_FILE = CONFIG_FOLDER + "tablesSchema.xlsx";
	public static final String APP_CONFIG_FILE = CONFIG_FOLDER + "appConfig.xml";
	
	public static final String DB_FOLDER = "database" + System.getProperty("file.separator");
	
	public static final String RELATIONS_SHEET = "Relations";
	public static final String TABLES_SHEET = "Tables";
	public static final String MESSAGE_CONFIG_SHEET = "messageConfig";
}

package app_config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Class to read an xml used to store the properties
 * @author avonva
 *
 */
public class PropertiesReader {

	private static final String APP_NAME_PROPERTY = "Application.Name";
	private static final String APP_VERSION_PROPERTY = "Application.Version";
	private static final String APP_DC_CODE_PROPERTY = "Application.DataCollectionCode";
	private static final String APP_TEST_REPORT_PROPERTY = "Application.TestReportCode";
	
	/**
	 * Read the application properties from the xml file
	 * @return
	 */
	public static Properties getProperties(String filename) {
		
		Properties properties = null;

		try {
			
			properties = new Properties();

			// fileStream from default properties xml file
			FileInputStream in = new FileInputStream(filename);
			properties.loadFromXML(in);

			in.close();
		}
		catch (IOException e) {
			System.err.println("The default properties file was not found. Please check!");
		}
		
		return properties;
	}
	
	/**
	 * Get the application name from the properties file
	 * @return
	 */
	public static String getAppName() {
		return getValue(APP_NAME_PROPERTY, "not found");
	}
	
	/**
	 * Get the version of the application from the 
	 * properties file
	 * @return
	 */
	public static String getAppVersion() {
		return getValue(APP_VERSION_PROPERTY, "not found");
	}
	
	
	/**
	 * Get the data collection code for which the 
	 * application was created
	 * @return
	 */
	public static String getDataCollectionCode() {
		return getValue(APP_DC_CODE_PROPERTY, "not found");
	}
	
	/**
	 * Get the test report code
	 * @return
	 */
	public static String getTestReportCode() {
		return getValue(APP_TEST_REPORT_PROPERTY, "not found");
	}
	
	/**
	 * Get a property value given the key
	 * @param property
	 * @return
	 */
	private static String getValue(String property, String defaultValue) {
		
		Properties prop = PropertiesReader.getProperties(AppPaths.APP_CONFIG_FILE);
		
		if ( prop == null )
			return defaultValue;
		
		return prop.getProperty(property);
	}
}

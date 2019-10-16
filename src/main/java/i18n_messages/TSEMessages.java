package i18n_messages;

import java.util.Locale;
import java.util.ResourceBundle;

import i18n_messages.RCLBundle;

public class TSEMessages {
	
	private static final String BUNDLE_NAME = "i18n_messages.tse_messages_en";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);

	private static RCLBundle bundle = new RCLBundle(RESOURCE_BUNDLE);
	
	public static String get(String key, String... values) {
		return bundle.get(key, values);
	}
	
	/* uncomment if want to load external messages
	//external jar proprieties file
	private static final String BUNDLE_NAME = "config\\tse_messages_en.properties";
	private static ResourceBundle RESOURCE_BUNDLE;
	private static RCLBundle bundle;
	
	public TSEMessages() {
		try {
			RESOURCE_BUNDLE = new PropertyResourceBundle(Files.newInputStream(Paths.get(BUNDLE_NAME)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String get(String key, String... values) {
		
		//interal proprieties file
		//return bundle.get(key, values);
	
		//external proprieties file
		try {
			RESOURCE_BUNDLE = new PropertyResourceBundle(Files.newInputStream(Paths.get(BUNDLE_NAME)));
			bundle = new RCLBundle(RESOURCE_BUNDLE);
			return bundle.get(key, values);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}*/
}

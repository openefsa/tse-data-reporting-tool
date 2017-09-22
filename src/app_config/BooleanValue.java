package app_config;

/**
 * Features to handle boolean values in string format
 * @author avonva
 *
 */
public class BooleanValue {

	private static final String[] yes = new String[] {"yes", "1", "y", "true"};
	private static final String[] no = new String[] {"no", "0", "n", "false"};
	
	/**
	 * Check if the string value indicates a true value or not
	 * @param value
	 * @return
	 */
	public static boolean isTrue(String value) {
		
		for (String string : yes) {
			if (string.equalsIgnoreCase(value))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Get a true value in string format
	 * @return
	 */
	public static String getTrueValue() {
		return yes[0];
	}
	
	/**
	 * Get a false value in string format
	 * @return
	 */
	public static String getFalseValue() {
		return no[0];
	}
}

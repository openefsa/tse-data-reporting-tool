package user;

public class User {

	private static User user;
	
	private String username;
	private String password;
	
	/**
	 * Get an instance of the current user
	 */
	public static User getInstance() {
		
		// get the instance if it is present
		// or create it otherwise
		if ( user == null )
			user = new User();
		
		return user;
	}
	
	/**
	 * Login the user
	 * @param username
	 * @param password
	 */
	public void login(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	/**
	 * Get the saved dcf username
	 * @return
	 */
	public String getUsername() {
		return username;
	}
	
	/**
	 * Get the saved dcf password
	 * @return
	 */
	public String getPassword() {
		return password;
	}
}

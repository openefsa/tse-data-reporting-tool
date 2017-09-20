package table_database;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;

import tse_config.AppPaths;

/**
 * Class which creates the application database. It uses an .xlsx file
 * to define the structure (tables/columns) of the database.
 * @author avonva
 *
 */
public class DatabaseCreator {
	
	private static final String DB_URL = "jdbc:derby:" + AppPaths.DB_FOLDER + ";create=true";
	
	private DatabaseStructureCreator queryCreator;
	
	/**
	 * Create a database using an .xlsx file schema
	 * to create the tables and columns
	 * @param filename
	 * @throws IOException 
	 */
	public DatabaseCreator(String filename) throws IOException {
		this.queryCreator = new DatabaseStructureCreator(filename);
	}
	
	/**
	 * Create the application database in the defined path
	 * @param path
	 * @throws IOException
	 */
	public void create(String path) throws IOException {
		
		String query = this.queryCreator.getQuery();
		
		// create the database
		try {

			// set a "create" connection
			DriverManager.getConnection(DB_URL);

			// sql script to create the database
			SQLScriptExec script = new SQLScriptExec(DB_URL, query);

			script.exec();

		} catch ( IOException | SQLException e ) {
			e.printStackTrace();
			return;
		}
	}
}

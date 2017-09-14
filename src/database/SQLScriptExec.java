package database;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLScriptExec {

	private String query;
	private String dbUrl;

	SQLScriptExec(String dbUrl, String query) throws FileNotFoundException {
		this.dbUrl = dbUrl;
		this.query = query;
	}

	/**
	 * After you detect the start of a comment this procedure will continue
	 * reading the comment until the character couple star and slash are found
	 * return true if there are still data available else false
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	private boolean readAndDiscardComment (Reader reader) throws IOException {

		int data = reader.read();
		boolean found = false;
		while ( ( data != -1 ) && ( ( (char) data ) != '*' ) && ( !found ) ) {
			if ( data != -1 ) {
				data = reader.read();
				if ( data == -1 ) {
					return false;
				} else if ( data == '/' ) {
					found = true;
					continue;
				}
			} else {
				return false;
			}
		}
		return found;
	}

	/**
	 * return true if there are still data available else false
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	private boolean readAndDiscardLine (Reader reader) throws IOException {

		int data = reader.read();
		boolean found = false;
		// I scan the line until the end of line
		while ( ( data != -1 ) && ( data != 10 ) && ( !found ) ) {
			if ( data != -1 ) {
				/* I have to check if there is the character '/' */
				data = reader.read();
				if ( data == -1 ) {
					return false;
				}
			} else {
				return false;
			}
		}
		return found;
	}

	/**
	 * @warning SQL statement can be maximum 2000 characters
	 * @throws IOException
	 */
	public void exec() throws IOException {
		
		Reader reader = new InputStreamReader(new ByteArrayInputStream(query.getBytes("UTF-8")));

		String bufSQL = "";
		int data = reader.read();

		boolean dataAvailable = true;

		while ( dataAvailable ) {

			while ( ( data != -1 ) && ( data != ';' ) && ( data != '/' ) ) {

				bufSQL = bufSQL + (char) data;

				data = reader.read();

			}

			/*
			 * If I stopped on a ; it means that I have to execute a command
			 * with what I have in the buffer
			 */

			if ( data == ';' ) {
				/* it is indeed the end of a statement which I will now execute */
				if ( !executeSQLStatement( dbUrl, bufSQL ) ) {
					System.err.println( "Error in the statement execution, script execution cancelled" );
					dataAvailable = false;
					continue;
				} else {
					dataAvailable = true;
					bufSQL = "";
					data = reader.read();
					continue;
				}

			}

			/*
			 * I need to read another element to understand if it is a comment
			 * or not
			 */

			int nextData = reader.read();

			if ( ( nextData == -1 ) ) {
				dataAvailable = false;
				continue;
			}
			if ( ( nextData == '*' ) ) {
				dataAvailable = readAndDiscardComment( reader );
				continue;
			}

			if ( ( nextData == '/' ) ) {
				dataAvailable = readAndDiscardLine( reader );
				continue;
			}

			bufSQL = bufSQL + nextData;
		}

		reader.close();
	}
	
	/**
	 * Used with SQLScriptExec.java to run an sql script file
	 * @param dbURL
	 * @param sql
	 * @return
	 */
	public static boolean executeSQLStatement (String dbURL, String sql) {
		
		try {
			
			Connection con = DriverManager.getConnection(dbURL);
			Statement stmt = con.createStatement();
			
			stmt.execute(sql);
			
			stmt.close();
			con.close();
			
			return true;
			
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		
		return false;
	}
}

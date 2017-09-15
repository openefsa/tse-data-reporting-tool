package user_interface;

import java.io.IOException;
import java.sql.SQLException;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import database.Database;

public class StartUI {

	public static void main(String args[]) throws IOException {
		
		// application start-up message. Usage of System.err used for red chars
		System.out.println( "Application Started " + System.currentTimeMillis() );
		
		// connect to the database application
		Database db = new Database();
		db.connect();
		
		Display display = new Display();
		Shell shell = new Shell(display);
		
		// set the application name in the shell
		shell.setText("TSEs");
		
		MainPanel panel = new MainPanel(shell);

	    shell.open();
		
		// Event loop
		while ( !shell.isDisposed() ) {
			if ( !display.readAndDispatch() )
				display.sleep();
		}

		display.dispose();
		
		// close the database
		db.shutdown();
		
		// exit the application
		System.exit(0);
	}
}

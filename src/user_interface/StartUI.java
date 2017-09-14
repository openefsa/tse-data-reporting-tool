package user_interface;

import java.io.IOException;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import database.Database;

public class StartUI {

	public static void main(String args[]) throws IOException {
		
		// application start-up message. Usage of System.err used for red chars
		System.out.println( "Application Started " + System.currentTimeMillis() );
		
		// connect to the database application
		Database db = new Database();
		db.connect(AppPaths.DB_FOLDER);
		
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
		
		// exit the application
		System.exit(0);
	}
}

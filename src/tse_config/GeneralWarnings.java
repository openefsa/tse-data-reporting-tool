package tse_config;

import javax.swing.JOptionPane;

import org.eclipse.swt.widgets.Shell;

import app_config.PropertiesReader;
import i18n_messages.TSEMessages;

public class GeneralWarnings {

	public static void showExceptionStack(Shell shell, Exception e) {
		
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement ste : e.getStackTrace()) {
	        sb.append("\n\tat ");
	        sb.append(ste);
	    }
	    String trace = sb.toString();
		
		
		JOptionPane.showMessageDialog(null, TSEMessages.get("generic.error", PropertiesReader.getSupportEmail(), trace), 
				TSEMessages.get("error.title"), JOptionPane.ERROR_MESSAGE);
	}
}

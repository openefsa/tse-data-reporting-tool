package tse_config;

import javax.swing.JOptionPane;

import org.eclipse.swt.widgets.Shell;

public class GeneralWarnings {

	public static void showExceptionStack(Shell shell, String title, Exception e) {
		
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement ste : e.getStackTrace()) {
	        sb.append("\n\tat ");
	        sb.append(ste);
	    }
	    String trace = sb.toString();
		
		
		JOptionPane.showMessageDialog(null, "XERRX: Generic runtime error." 
				+ " Please contact zoonoses_support@efsa.europa.eu. Error message " + trace, 
				"Generic error", JOptionPane.ERROR_MESSAGE);
	}
}

package tse_config;

import org.eclipse.swt.widgets.Shell;

import global_utils.Warnings;

public class GeneralWarnings {

	
	public static void showExceptionStack(Shell shell, String title, Exception e) {
		
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement ste : e.getStackTrace()) {
	        sb.append("\n\tat ");
	        sb.append(ste);
	    }
	    String trace = sb.toString();
		
		Warnings.warnUser(shell, title, "XERRX: Generic runtime error. Please contact zoonoses_support@efsa.europa.eu. Error message " 
				+ trace);
	}
}

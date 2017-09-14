package user_interface;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Shell;

import report.TableRow;
import report_interface.ReportViewer;

public class MainPanel {

	private Shell shell;
	private MainMenu menu;
	private ReportViewer reportViewer;
	
	/**
	 * Create the main user interface
	 */
	public MainPanel(Shell shell) {
		this.shell = shell;
		create();
	}
	
	/**
	 * Open the main user interface
	 */
	public void open() {
		this.shell.open();
	}
	
	public void setEnabled(boolean enabled) {
		this.reportViewer.setEnabled(enabled);
	}
	
	public void setReport(TableRow report) {
		this.reportViewer.setReport(report);
	}
	
	/**
	 * Create the interface
	 */
	private void create() {

		shell.setLayout(new GridLayout());
		
		this.menu = new MainMenu(this, shell);
		this.reportViewer = new ReportViewer(shell);
	}
}

package user_interface;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Shell;

import table_dialog.SummarizedInfoReportViewer;
import table_skeleton.TableRow;

public class MainPanel {

	private Shell shell;
	private MainMenu menu;
	private SummarizedInfoReportViewer reportViewer;
	
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
	
	public void openReport(TableRow report) {
		this.reportViewer.setParentTable(report);
	}
	public void closeReport() {
		this.reportViewer.clear();
	}
	
	/**
	 * Create the interface
	 */
	private void create() {

		shell.setLayout(new GridLayout());
		
		this.menu = new MainMenu(this, shell);
		this.reportViewer = new SummarizedInfoReportViewer(shell);
	}
}

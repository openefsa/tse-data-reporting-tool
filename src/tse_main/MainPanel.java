package tse_main;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Shell;

import app_config.GlobalManager;
import tse_report.TseReport;
import tse_summarized_information.SummarizedInfoDialog;

public class MainPanel {

	private Shell shell;
	private MainMenu menu;
	private SummarizedInfoDialog reportViewer;
	
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
		this.reportViewer.setRowCreationEnabled(enabled);
	}
	
	/**
	 * Open a report the main table
	 * @param report
	 */
	public void openReport(TseReport report) {
		this.reportViewer.setParentFilter(report);
		GlobalManager.getInstance().setOpenedReport(report);
	}
	
	/**
	 * Get the opened report
	 * @return
	 */
	public TseReport getOpenedReport() {
		return this.reportViewer.getReport();
	}
	
	/**
	 * Refresh the view
	 */
	public void refresh() {
		openReport(getOpenedReport());
	}
	
	public void closeReport() {
		this.reportViewer.clear();
		GlobalManager.getInstance().setOpenedReport(null);
	}
	
	/**
	 * Create the interface
	 */
	private void create() {

		shell.setLayout(new GridLayout());
		
		this.menu = new MainMenu(this, shell);
		this.reportViewer = new SummarizedInfoDialog(shell);
	}
}

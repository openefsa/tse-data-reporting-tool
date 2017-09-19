package user_interface;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import database.TableDao;
import table_dialog.SummarizedInfoReportViewer;
import table_skeleton.TableRow;
import xlsx_reader.TableSchema;

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
	
	public void loadParentRecords(TableRow report) {
		this.reportViewer.loadParentRecords(report);
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

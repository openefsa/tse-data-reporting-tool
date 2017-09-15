package user_interface;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import database.TableDao;
import report.TableRow;
import report_interface.ReportViewer;
import xlsx_reader.TableSchema;

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
	public void setPreferences(TableRow pref) {
		this.reportViewer.setPreferences(pref);
	}
	public void setSettings(TableRow sett) {
		this.reportViewer.setSettings(sett);
	}
	
	/**
	 * Create the interface
	 */
	private void create() {

		shell.setLayout(new GridLayout());
		
		this.menu = new MainMenu(this, shell);
		this.reportViewer = new ReportViewer(shell);
		
		// load the settings
		try {
			TableRow p = loadOptions(AppPaths.PREFERENCES_SHEET);
			TableRow s = loadOptions(AppPaths.SETTINGS_SHEET);
			
			if (p != null)
				setPreferences(p);
			
			if (s != null)
				setSettings(s);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Load the options (preferences/settings)
	 * @param sheetName
	 * @return
	 * @throws IOException
	 */
	private TableRow loadOptions(String sheetName) throws IOException {
		
		TableSchema schema = TableSchema.load(sheetName);
		
		TableDao dao = new TableDao(schema);
		
		Collection<TableRow> opts = dao.getAll();
		
		if (opts.isEmpty())
			return null;
		
		return opts.iterator().next();
	}
}

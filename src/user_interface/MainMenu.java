package user_interface;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import table_dialog.PreferencesDialog;
import table_dialog.ReportCreatorDialog;
import table_dialog.ReportListDialog;
import table_dialog.SettingsDialog;
import table_skeleton.TableRow;

/**
 * Create the main menu of the application in the given shell
 * @author avonva
 *
 */
public class MainMenu {

	private MainPanel mainPanel;
	private Shell shell;

	private Menu main;
	private Menu fileMenu;

	private MenuItem file;
	private MenuItem preferences;
	private MenuItem settings;

	private MenuItem newReport;
	private MenuItem openReport;
	private MenuItem closeReport;
	private MenuItem sendReport;
	private MenuItem downloadReport;

	public MainMenu(MainPanel mainPanel, Shell shell) {
		this.shell = shell;
		this.mainPanel = mainPanel;
		create();
	}

	private void create() {

		// create menus
		this.main = new Menu(shell, SWT.BAR);
		this.fileMenu = new Menu(shell, SWT.DROP_DOWN);

		this.file = new MenuItem(main, SWT.CASCADE);
		this.file.setText("File");
		this.file.setMenu(fileMenu);

		this.preferences = new MenuItem(main, SWT.PUSH);
		this.preferences.setText("Preferences");

		this.settings = new MenuItem(main, SWT.PUSH);
		this.settings.setText("Settings");

		// add buttons to the file menu
		this.newReport = new MenuItem(fileMenu, SWT.PUSH);
		this.newReport.setText("New report");

		this.newReport.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ReportCreatorDialog dialog = new ReportCreatorDialog(shell);
				dialog.setButtonText("Create");
				dialog.open();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});

		this.openReport = new MenuItem(fileMenu, SWT.PUSH);
		this.openReport.setText("Open report");
		
		this.openReport.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ReportListDialog dialog = new ReportListDialog(shell, "Reports", "Open a report");
				dialog.setButtonText("Open");
				dialog.setListener(new Listener() {
					
					@Override
					public void handleEvent(Event arg0) {						
						
						if (!(arg0.data instanceof TableRow))
							return;

						TableRow report = (TableRow) arg0.data;

						mainPanel.setEnabled(true);
						mainPanel.openReport(report);
						
						// enable things accordingly
						newReport.setEnabled(false);
						openReport.setEnabled(false);
						closeReport.setEnabled(true);
					}
				});
				dialog.open();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		this.closeReport = new MenuItem(fileMenu, SWT.PUSH);
		this.closeReport.setText("Close report");
		
		this.closeReport.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				mainPanel.closeReport();
				
				// enable things accordingly
				newReport.setEnabled(true);
				openReport.setEnabled(true);
				closeReport.setEnabled(false);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		// by default we do not have a report opened at the beginning
		this.closeReport.setEnabled(false);
		
		this.sendReport = new MenuItem(fileMenu, SWT.PUSH);
		this.sendReport.setText("Send report");
		this.sendReport.setEnabled(false);
		
		this.downloadReport = new MenuItem(fileMenu, SWT.PUSH);
		this.downloadReport.setText("Download report");

		// open preferences
		this.preferences.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				PreferencesDialog dialog = new PreferencesDialog(shell);
				dialog.open();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});

		// open settings
		this.settings.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				SettingsDialog dialog = new SettingsDialog(shell);
				dialog.open();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});



		// set the menu
		this.shell.setMenuBar(main);
	}

	public Menu getMenu() {
		return main;
	}
}

package tse_components;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import table_database.TableDao;
import table_importer.TableImporter;
import table_skeleton.TableRow;
import tse_config.AppPaths;
import xlsx_reader.TableSchema;

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
	private MenuItem importReport;
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
		
		this.fileMenu.addListener(SWT.Show, new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {
				
				// enable report only if there is a report in the database
				try {
					TableDao dao = new TableDao(TableSchema.load(AppPaths.REPORT_SHEET));
					openReport.setEnabled(!dao.getAll().isEmpty());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

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
						
						
						dialog.getDialog().setCursor(dialog.getDialog()
								.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
						
						mainPanel.openReport(report);
						
						dialog.getDialog().setCursor(dialog.getDialog()
								.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
						
						// enable things accordingly
						newReport.setEnabled(false);
						openReport.setEnabled(false);
						closeReport.setEnabled(true);
						importReport.setEnabled(true);
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
				importReport.setEnabled(false);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		// by default we do not have a report opened at the beginning
		this.closeReport.setEnabled(false);
		
		
		// add buttons to the file menu
		this.importReport = new MenuItem(fileMenu, SWT.PUSH);
		this.importReport.setText("Import aggregated data");
		this.importReport.setEnabled(false);
		this.importReport.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ReportListDialog dialog = new ReportListDialog(shell, "Reports", "Import a report");
				dialog.setButtonText("Import");
				dialog.setListener(new Listener() {
					
					@Override
					public void handleEvent(Event arg0) {
						
						if (!(arg0.data instanceof TableRow))
							return;

						// import the report into the opened report
						TableRow report = (TableRow) arg0.data;
						
						// copy the report summarized information into the opened one
						try {
							
							TableSchema childSchema = TableSchema.load(AppPaths.SUMMARIZED_INFO_SHEET);
							
							
							dialog.getDialog().setCursor(dialog.getDialog()
									.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
							
							TableImporter.copyByParent(childSchema, 
									report, mainPanel.getOpenedReport());
							
							mainPanel.refresh();
							
						} catch (IOException e) {
							e.printStackTrace();
						}
						finally {
							dialog.getDialog().setCursor(dialog.getDialog()
									.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
						}
					}
				});
				
				dialog.open();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
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

package tse_main;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.xml.sax.SAXException;

import app_config.AppPaths;
import app_config.DebugConfig;
import dataset.Dataset;
import message.SendMessageException;
import report_downloader.ReportImporter;
import table_database.TableDao;
import table_importer.TableImporter;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_options.PreferencesDialog;
import tse_options.SettingsDialog;
import tse_report.DownloadReportDialog;
import tse_report.Report;
import tse_report.ReportCreatorDialog;
import tse_report.ReportListDialog;
import warn_user.Warnings;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;
import xml_catalog_reader.Selection;

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

				TableSchema schema = Report.getReportSchema();
				
				if (schema == null)
					return;
				
				TableDao dao = new TableDao(schema);

				boolean hasReport = !dao.getAll().isEmpty();
				boolean isReportOpened = mainPanel.getOpenedReport() != null;
				boolean editable = isReportOpened && mainPanel.getOpenedReport().isEditable();

				openReport.setEnabled(hasReport);
				sendReport.setEnabled(isReportOpened);
				importReport.setEnabled(editable);
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
				ReportListDialog dialog = new ReportListDialog(shell, "Open a report");
				dialog.setButtonText("Open");
				dialog.setListener(new Listener() {
					
					@Override
					public void handleEvent(Event arg0) {						
						
						if (!(arg0.data instanceof Report))
							return;

						Report report = (Report) arg0.data;

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
				
				ReportListDialog dialog = new ReportListDialog(shell, "Import a report");
				dialog.setButtonText("Import");
				dialog.setListener(new Listener() {
					
					@Override
					public void handleEvent(Event arg0) {

						if (!(arg0.data instanceof TableRow))
							return;

						// import the report into the opened report
						TableRow report = (TableRow) arg0.data;

						// copy the report summarized information into the opened one
						TableSchema childSchema = TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET);

						if (childSchema == null)
							return;

						dialog.getDialog().setCursor(dialog.getDialog()
								.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

						TableImporter.copyByParent(childSchema, 
								report, mainPanel.getOpenedReport());

						mainPanel.refresh();

						dialog.getDialog().setCursor(dialog.getDialog()
								.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));

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
		
		this.sendReport.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				TableRow report = mainPanel.getOpenedReport();
				
				if (report == null) {
					System.err.println("Cannot send opened report since no report is currently opened!");
					return;
				}
				
				shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
				
				String title = "Success";
				String message = "Report successfully sent to the dcf.";
				int icon = SWT.ICON_INFORMATION;
				
				try {

					FileActions.exportAndSendReport(report);
					mainPanel.refresh();
					
				} catch (IOException e) {
					e.printStackTrace();
					
					title = "Error";
					message = "Errors occurred during the export of the report.";
					icon = SWT.ICON_ERROR;
					
				} catch (SOAPException e) {
					e.printStackTrace();
					
					title = "Connection error";
					message = "Cannot connect to the DCF. Please check your connections and credentials.";
					icon = SWT.ICON_ERROR;
					
				} catch (SAXException | ParserConfigurationException e) {
					e.printStackTrace();
					
					title = "Error";
					message = "Errors occurred during the creation of the report. Please check if the " 
							+ AppPaths.MESSAGE_GDE2_XSD + " file is correct. Received error: " + e.getMessage();
					icon = SWT.ICON_ERROR;
					
				} catch (SendMessageException e) {
					e.printStackTrace();
					
					title = "Error";
					message = "Send message failed. Received error: " + e.getMessage();
					icon = SWT.ICON_ERROR;
				}

				finally {
					shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
				}
				
				// warn the user
				Warnings.warnUser(shell, title, message, icon);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		this.downloadReport = new MenuItem(fileMenu, SWT.PUSH);
		this.downloadReport.setText("Download report");
		this.downloadReport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				DownloadReportDialog dialog = new DownloadReportDialog(shell);
				
				Dataset selectedDataset = dialog.getSelectedDataset();
				
				if (selectedDataset == null)
					return;
				
				// populate the dataset with the dcf information
				try {
					
					shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
					
					Dataset populatedDataset = selectedDataset.populate();
					
					ReportImporter importer = new ReportImporter(populatedDataset);
					importer.start();
					
				} catch (SOAPException e) {
					e.printStackTrace();
					Warnings.warnUser(shell, "Error", "Check your connection and credentials");
				}
				finally {
					shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
				}
			}
		});
		

		if (DebugConfig.debug) {
			MenuItem exportReport = new MenuItem(fileMenu, SWT.PUSH);
			exportReport.setText("[DEBUG] Export report");
			exportReport.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					try {
						FileActions.exportReport(mainPanel.getOpenedReport(), 
								"report " + System.currentTimeMillis() + ".xml");
					} catch (IOException | ParserConfigurationException | SAXException e) {
						e.printStackTrace();
					}
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {}
			});
			
			MenuItem deleteReport = new MenuItem(fileMenu, SWT.PUSH);
			deleteReport.setText("[DEBUG] Delete report");
			deleteReport.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					ReportListDialog dialog = new ReportListDialog(shell, "Delete a report");
					dialog.setButtonText("Delete");
					dialog.setListener(new Listener() {
						
						@Override
						public void handleEvent(Event arg0) {
							
							if (!(arg0.data instanceof Report))
								return;

							Report report = (Report) arg0.data;
							report.delete();
						}
					});
					dialog.open();
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {}
			});
		}
		

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

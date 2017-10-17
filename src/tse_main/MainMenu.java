package tse_main;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

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

import app_config.DebugConfig;
import app_config.PropertiesReader;
import message.MessageConfigBuilder;
import message_creator.OperationType;
import report.ReportException;
import report_downloader.TseReportDownloader;
import table_database.TableDao;
import table_importer.TableImporter;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_options.PreferencesDialog;
import tse_options.SettingsDialog;
import tse_report.ReportCreatorDialog;
import tse_report.ReportListDialog;
import tse_report.TseReport;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

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
	private MenuItem downloadReport;
	private MenuItem exitApplication;

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

				TableSchema schema = TseReport.getReportSchema();
				
				if (schema == null)
					return;
				
				TableDao dao = new TableDao(schema);

				boolean hasReport = !dao.getAll().isEmpty();
				boolean isReportOpened = mainPanel.getOpenedReport() != null;
				boolean editable = isReportOpened && mainPanel.getOpenedReport().isEditable();

				newReport.setEnabled(!isReportOpened);
				openReport.setEnabled(hasReport);
				closeReport.setEnabled(isReportOpened);
				downloadReport.setEnabled(!isReportOpened);
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
						
						if (!(arg0.data instanceof TseReport))
							return;

						TseReport report = (TseReport) arg0.data;

						mainPanel.setEnabled(true);
						
						
						dialog.getDialog().setCursor(dialog.getDialog()
								.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
						
						mainPanel.openReport(report);
						
						dialog.getDialog().setCursor(dialog.getDialog()
								.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
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

						// copy the data into the selected report
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
		
		this.downloadReport = new MenuItem(fileMenu, SWT.PUSH);
		this.downloadReport.setText("Download report");
		this.downloadReport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				TseReportDownloader downloader = new TseReportDownloader(shell);
				downloader.download(CustomStrings.VALID_SENDER_ID_PATTERN);
			}
		});
		
		this.exitApplication = new MenuItem(fileMenu, SWT.PUSH);
		this.exitApplication.setText("Close application");
		this.exitApplication.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				shell.close();
				shell.dispose();
			}
		});
		

		if (DebugConfig.debug) {
			MenuItem exportReport = new MenuItem(fileMenu, SWT.PUSH);
			exportReport.setText("[DEBUG] Export report");
			exportReport.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {

					TseReport report = mainPanel.getOpenedReport();

					MessageConfigBuilder config = report.getDefaultExportConfiguration(OperationType.REPLACE);
					try {
						report.export(config);
					} catch (IOException | ParserConfigurationException | SAXException | ReportException e) {
						e.printStackTrace();
					}
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {}
			});
			
			MenuItem reportVersions = new MenuItem(fileMenu, SWT.PUSH);
			reportVersions.setText("[DEBUG] Print report versions");
			reportVersions.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					
					TseReport report = mainPanel.getOpenedReport();
					System.out.println(report.getAllVersions());
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
							
							if (!(arg0.data instanceof TseReport))
								return;

							TseReport report = (TseReport) arg0.data;
							report.delete();
						}
					});
					dialog.open();
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {}
			});
		}
		
		MenuItem getDc = new MenuItem(fileMenu, SWT.PUSH);
		getDc.setText("[DEBUG] Print current data collection");
		getDc.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				TseReport report = mainPanel.getOpenedReport();
				
				if (report == null) {
					System.out.println(PropertiesReader.getDataCollectionCode());
					return;
				}
				
				System.out.println(PropertiesReader.getDataCollectionCode(report.getYear()));
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		

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

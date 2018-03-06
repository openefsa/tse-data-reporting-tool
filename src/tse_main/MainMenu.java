package tse_main;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
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

import amend_manager.AmendException;
import app_config.PropertiesReader;
import dataset.Dataset;
import formula.FormulaException;
import global_utils.Message;
import global_utils.Warnings;
import i18n_messages.TSEMessages;
import message.MessageConfigBuilder;
import message_creator.OperationType;
import providers.IFormulaService;
import providers.ITableDaoService;
import providers.TseReportService;
import report.ReportException;
import report.ReportSendOperation;
import report_downloader.TseReportDownloader;
import report_downloader.TseReportImporter;
import soap.DetailedSOAPException;
import table_database.TableDao;
import table_skeleton.TableRow;
import table_skeleton.TableVersion;
import test_case.EnumPicker;
import tse_config.CustomStrings;
import tse_config.DebugConfig;
import tse_options.PreferencesDialog;
import tse_options.SettingsDialog;
import tse_report.ReportCreatorDialog;
import tse_report.ReportListDialog;
import tse_report.TseReport;
import user_interface.ProxySettingsDialog;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

/**
 * Create the main menu of the application in the given shell
 * @author avonva
 *
 */
public class MainMenu {

	private static final Logger LOGGER = LogManager.getLogger(MainMenu.class);
	
	private TseReportService reportService;
	private ITableDaoService daoService;
	private IFormulaService formulaService;
	
	private MainPanel mainPanel;
	private Shell shell;

	private Menu main;
	private Menu fileMenu;

	private MenuItem file;
	private MenuItem preferences;
	private MenuItem settings;
	private MenuItem proxyConfig;

	private MenuItem newReport;
	private MenuItem openReport;
	private MenuItem closeReport;
	private MenuItem importReport;
	private MenuItem downloadReport;
	private MenuItem exportReport;
	private MenuItem exitApplication;

	public MainMenu(MainPanel mainPanel, Shell shell, TseReportService reportService, ITableDaoService daoService,
			IFormulaService formulaService) {
		this.shell = shell;
		this.mainPanel = mainPanel;
		this.reportService = reportService;
		this.daoService = daoService;
		this.formulaService = formulaService;
		create();
	}

	private void create() {

		// create menus
		this.main = new Menu(shell, SWT.BAR);
		this.fileMenu = new Menu(shell, SWT.DROP_DOWN);

		this.file = new MenuItem(main, SWT.CASCADE);
		this.file.setText(TSEMessages.get("file.item"));
		this.file.setMenu(fileMenu);
		
		this.fileMenu.addListener(SWT.Show, new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {

				// enable report only if there is a report in the database

				TableSchema schema = TseReport.getReportSchema();
				
				if (schema == null)
					return;
				
				TableDao dao = new TableDao();

				boolean hasReport = !dao.getAll(schema).isEmpty();
				boolean isReportOpened = mainPanel.getOpenedReport() != null;
				boolean editable = isReportOpened && mainPanel.getOpenedReport().isEditable();

				newReport.setEnabled(!isReportOpened);
				openReport.setEnabled(hasReport);
				closeReport.setEnabled(isReportOpened);
				downloadReport.setEnabled(!DebugConfig.disableFileFuncs && !isReportOpened);
				
				// can only export valid reports
				exportReport.setEnabled(isReportOpened && mainPanel
						.getOpenedReport().getRCLStatus().isValid());
				importReport.setEnabled(!DebugConfig.disableFileFuncs && editable);
			}
		});

		this.preferences = new MenuItem(main, SWT.PUSH);
		this.preferences.setText(TSEMessages.get("pref.item"));

		this.settings = new MenuItem(main, SWT.PUSH);
		this.settings.setText(TSEMessages.get("settings.item"));
		
		this.proxyConfig = new MenuItem(main, SWT.PUSH);
		this.proxyConfig.setText(TSEMessages.get("proxy.config.item"));
		
		this.proxyConfig.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ProxySettingsDialog dialog = new ProxySettingsDialog(shell, 
						SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
				dialog.open();
			}
		});

		// add buttons to the file menu
		this.newReport = new MenuItem(fileMenu, SWT.PUSH);
		this.newReport.setText(TSEMessages.get("new.report.item"));

		this.newReport.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				LOGGER.debug("Opening new report dialog");
				
				ReportCreatorDialog dialog = new ReportCreatorDialog(shell, reportService);
				dialog.setButtonText(TSEMessages.get("new.report.button"));
				dialog.open();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});

		this.openReport = new MenuItem(fileMenu, SWT.PUSH);
		this.openReport.setText(TSEMessages.get("open.report.item"));
		
		this.openReport.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				LOGGER.debug("Opening open report dialog");
				
				ReportListDialog dialog = new ReportListDialog(shell, TSEMessages.get("open.report.title"));
				dialog.setButtonText(TSEMessages.get("open.report.button"));
				
				dialog.open();
				
				TseReport report = dialog.getSelectedReport();
				
				if (report == null)
					return;

				LOGGER.info("Opening report=" + report.getSenderId());
				
				mainPanel.setEnabled(true);
				
				shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
				
				mainPanel.openReport(report);
				
				shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		this.closeReport = new MenuItem(fileMenu, SWT.PUSH);
		this.closeReport.setText(TSEMessages.get("close.report.item"));
		
		this.closeReport.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				LOGGER.info("Closing report=" + mainPanel.getOpenedReport().getSenderId());
				
				mainPanel.closeReport();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		// by default we do not have a report opened at the beginning
		this.closeReport.setEnabled(false);
		
		
		// add buttons to the file menu
		this.importReport = new MenuItem(fileMenu, SWT.PUSH);
		this.importReport.setText(TSEMessages.get("import.report.item"));
		this.importReport.setEnabled(false);
		this.importReport.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				LOGGER.debug("Opening import report dialog");
				
				ReportListDialog dialog = new ReportListDialog(shell, TSEMessages.get("import.report.title"));
				dialog.setButtonText(TSEMessages.get("import.report.button"));
				dialog.open();
				
				// import the report into the opened report
				TableRow report = dialog.getSelectedReport();
				
				if (report == null)
					return;

				// copy the report summarized information into the opened one
				TableSchema childSchema = TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET);

				if (childSchema == null)
					return;

				LOGGER.info("Importing summarized information from report=" 
						+ report.getCode(CustomStrings.SENDER_DATASET_ID_COLUMN)
						+ " to report=" + mainPanel.getOpenedReport().getSenderId());
				
				shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));

				TseSummarizedInfoImporter importer = new TseSummarizedInfoImporter(daoService, formulaService);

				// copy the data into the selected report
				importer.copyByParent(childSchema, report, mainPanel.getOpenedReport());

				mainPanel.refresh();

				shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		this.downloadReport = new MenuItem(fileMenu, SWT.PUSH);
		this.downloadReport.setText(TSEMessages.get("download.report.item"));
		this.downloadReport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				LOGGER.debug("Opening download report dialog");
				
				TseReportDownloader downloader = new TseReportDownloader(shell, reportService, daoService);
				try {
					downloader.download();
				} catch (DetailedSOAPException e) {
					e.printStackTrace();
					LOGGER.error("Download report failed", e);
					Warnings.showSOAPWarning(shell, e);
				}
			}
		});
		
		this.exportReport = new MenuItem(fileMenu, SWT.PUSH);
		exportReport.setText(TSEMessages.get("export.report.item"));
		exportReport.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {

				TseReport report = mainPanel.getOpenedReport();
				
				if (report == null)
					return;
			
				// save the file
				TseFileDialog fileDialog = new TseFileDialog(shell);
				String filename = TableVersion.mergeNameAndVersion(report.getSenderId(), report.getVersion());
				File exportFile = fileDialog.saveXml(filename);
				
				if (exportFile == null)
					return;
				
				shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
				
				ReportSendOperation opSendType = null;
				try {
					Dataset dataset = reportService.getLatestDataset(report);
					opSendType = reportService.getSendOperation(report, dataset);
				} catch (DetailedSOAPException e1) {
					
					shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
					e1.printStackTrace();
					
					Warnings.showSOAPWarning(shell, e1);
					
					Message m = Warnings.create(TSEMessages.get("export.report.no.connection"));
					m.open(shell);
				}
				finally {
					shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
				}
				
				OperationType opType = null;
				
				// if no connection open the dialog
				if (opSendType == null) {
					EnumPicker<OperationType> dialog = new EnumPicker<>(shell, OperationType.class);
					dialog.setTitle(TSEMessages.get("export.report.op.title"));
					dialog.setConfirmText(TSEMessages.get("export.report.op.confirm"));
					dialog.open();
					
					opType = (OperationType) dialog.getSelection();
				}
				else {
					opType = opSendType.getOpType();
				}
				
				if (opType == null)
					return;

				LOGGER.info("Exporting report=" + report.getSenderId());
				
				MessageConfigBuilder messageConfig = reportService.getSendMessageConfiguration(report);
				messageConfig.setOpType(opType);
				messageConfig.setOut(exportFile);
				
				try {
					reportService.export(report, messageConfig);
				} catch (IOException | ParserConfigurationException | SAXException | ReportException e) {
					e.printStackTrace();
					LOGGER.error("Export report failed", e);
				} catch (AmendException e) {
					LOGGER.error("Export report failed", e);
					e.printStackTrace();
					Warnings.warnUser(shell, TSEMessages.get("error.title"), 
							TSEMessages.get("report.empty.error"));
				}
				
				shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		this.exitApplication = new MenuItem(fileMenu, SWT.PUSH);
		this.exitApplication.setText(TSEMessages.get("close.app.item"));
		this.exitApplication.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				shell.close();
				shell.dispose();
			}
		});

		// open preferences
		this.preferences.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				LOGGER.debug("Opening preferences dialog");
				
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
				
				LOGGER.debug("Opening settings dialog");
				
				SettingsDialog dialog = new SettingsDialog(shell, reportService, daoService);
				dialog.open();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		if (DebugConfig.debug) {
			addDebugItems();
		}
		
		// set the menu
		this.shell.setMenuBar(main);
	}

	/**
	 * Add some debug functionalities
	 */
	private void addDebugItems() {
		
		MenuItem reportVersions = new MenuItem(fileMenu, SWT.PUSH);
		reportVersions.setText("[DEBUG] Print report versions");
		reportVersions.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				TseReport report = mainPanel.getOpenedReport();
				
				if (report == null)
					return;
				
				LOGGER.debug("Report versions=" + report.getAllVersions(daoService));
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		MenuItem exportReport = new MenuItem(fileMenu, SWT.PUSH);
		exportReport.setText("[DEBUG] Export report");
		exportReport.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {

				TseReport report = mainPanel.getOpenedReport();
				
				if (report == null)
					return;
				
				EnumPicker<OperationType> dialog = new EnumPicker<>(shell, OperationType.class);
				dialog.open();
				
				OperationType opType = (OperationType) dialog.getSelection();
				
				if (opType == null)
					return;
				
				LOGGER.debug("Exporting report " + report.getSenderId());
				MessageConfigBuilder messageConfig = reportService.getSendMessageConfiguration(report);
				messageConfig.setOpType(opType);
				try {
					reportService.export(report, messageConfig);
				} catch (IOException | ParserConfigurationException | SAXException | ReportException | AmendException e) {
					e.printStackTrace();
					LOGGER.error("Export report failed", e);
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
				dialog.open();

				TseReport report = dialog.getSelectedReport();
				
				if (report == null)
					return;
				
				LOGGER.debug("Report " + report.getSenderId() + " deleted from disk");
				
				report.delete();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
	
	
		MenuItem getDc = new MenuItem(fileMenu, SWT.PUSH);
		getDc.setText("[DEBUG] Print current data collection");
		getDc.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				TseReport report = mainPanel.getOpenedReport();
				
				String dcCode = report == null ? PropertiesReader.getDataCollectionCode()
						: PropertiesReader.getDataCollectionCode(report.getYear());
				
				LOGGER.debug("The tool points to the data collection=" + dcCode);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		MenuItem importReport = new MenuItem(fileMenu, SWT.PUSH);
		importReport.setText("[DEBUG] Import first version .xml report");
		importReport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				TseFileDialog fileDialog = new TseFileDialog(shell);
				File file = fileDialog.loadXml();
				
				if (file == null)
					return;
				
				try {
					TseReportImporter imp = new TseReportImporter(reportService, daoService);
					imp.importFirstDatasetVersion(file);
					
				} catch (XMLStreamException | IOException | 
						FormulaException | ParseException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public Menu getMenu() {
		return main;
	}
}

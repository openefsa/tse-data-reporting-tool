package report_downloader;

import java.io.IOException;
import java.text.ParseException;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import amend_manager.ReportImporter;
import app_config.AppPaths;
import app_config.PropertiesReader;
import data_collection.IDataCollectionsDialog;
import data_collection.IDcfDataCollection;
import data_collection.IDcfDataCollectionsList;
import dataset.Dataset;
import dataset.DatasetList;
import dataset.IDataset;
import dataset.NoAttachmentException;
import formula.FormulaException;
import global_utils.Message;
import global_utils.Warnings;
import i18n_messages.TSEMessages;
import providers.ITableDaoService;
import providers.TseReportService;
import report.DownloadReportDialog;
import report.IDownloadReportDialog;
import report.ReportDownloaderDialog;
import session_manager.TSERestoreableWindowDao;
import soap.DetailedSOAPException;
import tse_config.CustomStrings;
import window_restorer.RestoreableWindow;
import xml_catalog_reader.Selection;
import xml_catalog_reader.XmlContents;
import xml_catalog_reader.XmlLoader;

/**
 * Download a report into the local database.
 * @author avonva
 *
 */
public class TseReportDownloader extends ReportDownloaderDialog {

	private static final Logger LOGGER = LogManager.getLogger(TseReportDownloader.class);
	
	private TseReportService reportService;
	private ITableDaoService daoService;
	
	private RestoreableWindow window;
	private static final String WINDOW_CODE = "TSEReportDownloader";
	private Shell shell;
	
	public TseReportDownloader(Shell shell, TseReportService reportService, ITableDaoService daoService) {
		super(shell, reportService);
		this.shell = shell;
		this.reportService = reportService;
		this.daoService = daoService;
	}

	@Override
	public ReportImporter getImporter(DatasetList allVersions) {
		return new TseReportImporter(allVersions, reportService, daoService);
	}
	
	@Override
	public IDownloadReportDialog getDownloadDialog(IDcfDataCollection dc) {
		
		DownloadReportDialog dialog = new DownloadReportDialog(shell, 
				CustomStrings.VALID_SENDER_ID_PATTERN);
		
		// here the shell is initialized
		this.window = new RestoreableWindow(dialog.getDialog(), WINDOW_CODE);
		window.saveOnClosure(TSERestoreableWindowDao.class);
		
		TableViewer table = dialog.getTable();
		TableViewerColumn yearCol = new TableViewerColumn(table, SWT.NONE);
		yearCol.getColumn().setWidth(80);
		yearCol.getColumn().setText(TSEMessages.get("dataset.header.year"));
		yearCol.setLabelProvider(new ColumnLabelProvider() {
			 @Override
			public String getText(Object element) {

				Dataset dataset = (Dataset) element;

				String senderId = dataset.getSenderId();
				String year = "20" + senderId.substring(2, 4);
				
				XmlContents contents = XmlLoader.getByPicklistKey(AppPaths.YEARS_LIST);
				
				if (contents == null) {
					LOGGER.error("No " + AppPaths.YEARS_LIST + " was found in " + AppPaths.CONFIG_FOLDER);
					return year;
				}
				
				Selection sel = contents.getElementByCode(year);
				
				if (sel == null) {
					LOGGER.error("No value found in " + AppPaths.YEARS_LIST + " for " + year);
					return year;
				}
				
				return sel.getDescription();
			}
		});
		
		TableViewerColumn monthCol = new TableViewerColumn(table, SWT.NONE);
		monthCol.getColumn().setText(TSEMessages.get("dataset.header.month"));
		monthCol.getColumn().setWidth(80);
		monthCol.setLabelProvider(new ColumnLabelProvider() {
			 @Override
			public String getText(Object element) {
				 
				Dataset dataset = (Dataset) element;

				String senderId = dataset.getSenderId();
				String month = senderId.substring(4, 6);
				
				// remove padding if necessart
				if (month.charAt(0) == '0') {
					month = month.substring(1, 2);
				}
				
				XmlContents contents = XmlLoader.getByPicklistKey(AppPaths.MONTHS_LIST);
				
				if (contents == null) {
					LOGGER.error("No " + AppPaths.MONTHS_LIST + " was found in " + AppPaths.CONFIG_FOLDER);
					return month;
				}
				
				Selection sel = contents.getElementByCode(month);
				
				if (sel == null) {
					LOGGER.error("No value found in " + AppPaths.MONTHS_LIST + " for " + month);
					return month;
				}
				
				return sel.getDescription();
			}
		});
		
		dialog.addIdCol();
		dialog.addSenderIdCol();
		dialog.addStatusCol();
		dialog.addRevisionCol();
		
		boolean restored = window.restore(TSERestoreableWindowDao.class);

		if (!restored) {
			dialog.getDialog().pack();
			dialog.getDialog().setSize(dialog.getDialog().getSize().x, 500);
		}
		
		return dialog;
	}

	@Override
	public void manageException(Exception e) {

		Message msg = null;

		IDataset[] list = new Dataset[getAllVersions().size()];
		for (int i = 0; i < getAllVersions().size(); ++i)
			list[i] = getAllVersions().get(i);
		
		if (e instanceof DetailedSOAPException) {
			msg = Warnings.createSOAPWarning((DetailedSOAPException) e);
		}
		else if (e instanceof XMLStreamException
				|| e instanceof IOException) {
			
			msg = Warnings.createFatal(TSEMessages.get("download.bad.format",
					PropertiesReader.getSupportEmail()), list);
		}
		else if (e instanceof FormulaException) { 
			msg = Warnings.createFatal(TSEMessages.get("download.bad.parsing",
					PropertiesReader.getSupportEmail()), list);
		}
		else if (e instanceof NoAttachmentException) {
			msg = Warnings.createFatal(TSEMessages.get("download.no.attachment",
					PropertiesReader.getSupportEmail()), list);
		}
		else if (e instanceof ParseException) {
			msg = Warnings.createFatal(TSEMessages.get("download.bad.parsing",
					PropertiesReader.getSupportEmail()), list);
		}
		else {
			
			msg = Warnings.createFatal(TSEMessages.get("generic.error",
					PropertiesReader.getSupportEmail()), list);
		}
		
		msg.open(shell);
	}

	@Override
	public void end() {
		String title = TSEMessages.get("success.title");
		String message = TSEMessages.get("download.success");
		int style = SWT.ICON_INFORMATION;
		Warnings.warnUser(shell, title, message, style);
	}

	@Override
	public boolean askConfirmation() {
		
		int val = Warnings.warnUser(shell, TSEMessages.get("warning.title"), 
				TSEMessages.get("download.replace"), 
				SWT.YES | SWT.NO | SWT.ICON_WARNING);
		
		return val == SWT.YES;
	}

	@Override
	public IDataCollectionsDialog getDataCollectionsDialog(Shell shell,
			IDcfDataCollectionsList<IDcfDataCollection> list) {
		return new TSEDataCollectionsListDialog(shell, list);
	}
}

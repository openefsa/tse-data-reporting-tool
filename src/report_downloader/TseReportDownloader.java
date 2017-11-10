package report_downloader;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import amend_manager.ReportImporter;
import app_config.AppPaths;
import dataset.Dataset;
import dataset.DatasetList;
import report.DownloadReportDialog;
import report.ReportDownloader;
import tse_config.CustomStrings;
import xml_catalog_reader.Selection;
import xml_catalog_reader.XmlContents;
import xml_catalog_reader.XmlLoader;

/**
 * Download a report into the db
 * @author avonva
 *
 */
public class TseReportDownloader extends ReportDownloader {

	private Shell shell;
	
	public TseReportDownloader(Shell shell) {
		super(shell);
		this.shell = shell;
	}

	@Override
	public ReportImporter getImporter(DatasetList<Dataset> allVersions) {
		return new TseReportImporter(allVersions);
	}

	@Override
	public DownloadReportDialog getDialog() {
		
		DownloadReportDialog dialog = new DownloadReportDialog(shell, 
				CustomStrings.VALID_SENDER_ID_PATTERN);
		
		TableViewer table = dialog.getTable();
		TableViewerColumn yearCol = new TableViewerColumn(table, SWT.NONE);
		yearCol.getColumn().setWidth(80);
		yearCol.getColumn().setText("Year");
		yearCol.setLabelProvider(new ColumnLabelProvider() {
			 @Override
			public String getText(Object element) {
				 
				Dataset dataset = (Dataset) element;

				String senderId = dataset.getSenderId();
				String year = "20" + senderId.substring(2, 4);
				
				XmlContents contents = XmlLoader.getByPicklistKey(AppPaths.YEARS_LIST);
				
				if (contents == null) {
					System.err.println("No " + AppPaths.YEARS_LIST + " was found in " + AppPaths.CONFIG_FOLDER);
					return year;
				}
				
				Selection sel = contents.getElementByCode(year);
				
				if (sel == null) {
					System.err.println("No value found in " + AppPaths.YEARS_LIST + " for " + year);
					return year;
				}
				
				return sel.getDescription();
			}
		});
		
		TableViewerColumn monthCol = new TableViewerColumn(table, SWT.NONE);
		monthCol.getColumn().setText("Month");
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
					System.err.println("No " + AppPaths.MONTHS_LIST + " was found in " + AppPaths.CONFIG_FOLDER);
					return month;
				}
				
				Selection sel = contents.getElementByCode(month);
				
				if (sel == null) {
					System.err.println("No value found in " + AppPaths.MONTHS_LIST + " for " + month);
					return month;
				}
				
				return sel.getDescription();
			}
		});
		
		dialog.addIdCol();
		dialog.addSenderIdCol();
		dialog.addStatusCol();
		dialog.addRevisionCol();
		
		dialog.loadDatasets();

		return dialog;
	}

}

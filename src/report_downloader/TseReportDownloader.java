package report_downloader;

import org.eclipse.swt.widgets.Shell;

import amend_manager.ReportImporter;
import dataset.Dataset;
import dataset.DatasetList;
import report.ReportDownloader;

/**
 * Download a report into the db
 * @author avonva
 *
 */
public class TseReportDownloader extends ReportDownloader {

	public TseReportDownloader(Shell shell) {
		super(shell);
	}

	@Override
	public ReportImporter getImporter(DatasetList<Dataset> allVersions) {
		return new TseReportImporter(allVersions);
	}

}

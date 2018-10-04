package tse_report;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dataset.IDataset;
import providers.IReportService;
import report.EFSAReport;
import report.ThreadFinishedListener;
import soap.DetailedSOAPException;

public class GetReportLatestDatasetThread extends Thread {

	private static final Logger LOGGER = LogManager.getLogger(GetReportLatestDatasetThread.class);
	private IDataset dataset;
	private IReportService reportService;
	private ThreadFinishedListener listener;
	private EFSAReport report;
	
	public GetReportLatestDatasetThread(EFSAReport report, IReportService reportService) {
		this.report = report;
		this.reportService = reportService;
	}
	
	public void setListener(ThreadFinishedListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void run() {
		try {
			dataset = reportService.getDataset(report);
			if (listener != null)
				listener.finished(this);
		} catch (DetailedSOAPException e) {
			e.printStackTrace();
			LOGGER.error("Cannot retrieve latest dataset of report=" + report.getSenderId(), e);
			if (listener != null)
				listener.terminated(this, e);
		}
	}
	
	public IDataset getDataset() {
		return dataset;
	}
}

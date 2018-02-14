package tse_report;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import dataset.IDataset;
import report.EFSAReport;
import report.ReportException;
import report.ThreadFinishedListener;
import soap.DetailedSOAPException;

public class GetReportLatestDatasetThread extends Thread {

	private static final Logger LOGGER = LogManager.getLogger(GetReportLatestDatasetThread.class);
	private IDataset dataset;
	private ThreadFinishedListener listener;
	private EFSAReport report;
	
	public GetReportLatestDatasetThread(EFSAReport report) {
		this.report = report;
	}
	
	public void setListener(ThreadFinishedListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void run() {
		try {
			
			dataset = report.getLatestDataset();
			if (listener != null)
				listener.finished(this);
		} catch (DetailedSOAPException | ReportException e) {
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

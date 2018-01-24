package tse_report;

import dataset.IDataset;
import report.EFSAReport;
import report.ReportException;
import report.ThreadFinishedListener;
import soap.MySOAPException;

public class GetReportLatestDatasetThread extends Thread {

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
		} catch (MySOAPException | ReportException e) {
			e.printStackTrace();
			if (listener != null)
				listener.terminated(this, e);
		}
	}
	
	public IDataset getDataset() {
		return dataset;
	}
}

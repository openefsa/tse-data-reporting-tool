package tse_report;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import config.Environment;
import dataset.IDataset;
import report.EFSAReport;
import report.ReportException;
import report.ReportService;
import report.ThreadFinishedListener;
import soap.DetailedSOAPException;
import soap_interface.IGetDatasetsList;

public class GetReportLatestDatasetThread extends Thread {

	private static final Logger LOGGER = LogManager.getLogger(GetReportLatestDatasetThread.class);
	
	private Environment env;
	private IGetDatasetsList<IDataset> getDatasetList;
	private IDataset dataset;
	private ThreadFinishedListener listener;
	private EFSAReport report;
	
	public GetReportLatestDatasetThread(EFSAReport report, Environment env, IGetDatasetsList<IDataset> getDatasetList) {
		this.report = report;
		this.env = env;
		this.getDatasetList = getDatasetList;
	}
	
	public void setListener(ThreadFinishedListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void run() {
		try {
			
			ReportService reportService = new ReportService(env, getDatasetList);
			dataset = reportService.getLatestDataset(report);
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

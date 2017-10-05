package tse_report;

import javax.xml.soap.SOAPException;

import dataset.Dataset;

public class ReportDownloader {

	private Dataset dataset;
	
	public ReportDownloader(Dataset dataset) {
		this.dataset = dataset;
	}
	
	public void download() throws SOAPException {
		// populate the dataset with the dcf information
		dataset.populate();
	}
}

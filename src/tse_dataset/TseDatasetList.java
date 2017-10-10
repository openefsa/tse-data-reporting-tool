package tse_dataset;

import dataset.Dataset;
import dataset.DatasetList;
import report_downloader.ReportImporter;
import webservice.MySOAPException;

public class TseDatasetList extends DatasetList {
	
	private static final long serialVersionUID = 4588812414665723577L;

	/**
	 * Create the list starting from a standard dataset list
	 * @param list
	 */
	public TseDatasetList(DatasetList list) {
		this.addAll(list);
	}
	
	/**
	 * Download all the datasets and import them into the database
	 * @throws MySOAPException
	 */
	public void downloadAll() throws MySOAPException {
		
		// for each dataset of the list download it
		for (Dataset d: this) {
			
			// populate the dataset with its data
			// (we have just some metadata)
			Dataset populatedDataset = d.populate();
			
			// skip dataset if not retrieved
			if (populatedDataset == null)
				continue;
			
			// import the dataset
			ReportImporter importer = new ReportImporter(populatedDataset);
			importer.start();
		}
	}
}

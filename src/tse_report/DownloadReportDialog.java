package tse_report;

import javax.xml.soap.SOAPException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import app_config.PropertiesReader;
import dataset.Dataset;
import dataset.DatasetList;
import table_dialog.DatasetListDialog;
import tse_config.CustomStrings;
import webservice.GetDatasetList;

/**
 * Show the list of dcf datasets
 * @author avonva
 *
 */
public class DownloadReportDialog extends DatasetListDialog {
	
	private DatasetList<Dataset> allDatasets;
	
	public DownloadReportDialog(Shell parent) {
		
		super(parent, "Available reports", "Download");
		
		parent.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
		
		this.setList(getDownloadableDatasets());
		
		parent.setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
		
		this.open();
	}

	/**
	 * Get a list of all the dcf datasets which are downloadable
	 * @return
	 */
	private DatasetList<Dataset> getDownloadableDatasets() {
		
		GetDatasetList req = new GetDatasetList(PropertiesReader.getDataCollectionCode());
		try {
			
			this.allDatasets = req.getList();
			return allDatasets.getDownloadableDatasets(CustomStrings.VALID_SENDER_ID_PATTERN);
			
		} catch (SOAPException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Get all the versions of the dataset
	 * @return
	 */
	public DatasetList<Dataset> getSelectedDatasetVersions() {

		Dataset dataset = getSelectedDataset();
		
		if (dataset == null) {
			return null;
		}
		
		String senderId = dataset.getDecomposedSenderId();
		
		if (senderId == null)
			return null;
		
		// get all the versions of the dataset
		return this.allDatasets.filterByDecomposedSenderId(senderId);
	}
}

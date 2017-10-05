package tse_report;

import javax.xml.soap.SOAPException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import app_config.PropertiesReader;
import dataset.DatasetList;
import table_dialog.DatasetListDialog;
import webservice.GetDatasetList;

/**
 * Show the list of dcf datasets
 * @author avonva
 *
 */
public class DownloadReportDialog extends DatasetListDialog {
	
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
	private DatasetList getDownloadableDatasets() {
		
		GetDatasetList req = new GetDatasetList(PropertiesReader.getDataCollectionCode());
		try {
			
			return req.getList().getDownloadableDatasets();
			
		} catch (SOAPException e) {
			e.printStackTrace();
			return null;
		}
	}
}

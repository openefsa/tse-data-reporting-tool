package user_objects;

import java.util.ArrayList;

import user_webservice.GetDatasetList;

/**
 * List of dataset received by calling {@link GetDatasetList}
 * @author avonva
 *
 */
public class DatasetList extends ArrayList<Dataset> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Check if the datasets list contain a 
	 * report with the chosen senderId
	 * @param senderId
	 * @return
	 */
	public boolean contains(String senderId) {
		return getBySenderId(senderId) != null;
	}
	
	/**
	 * get a dataset by its sender id
	 * @param senderId
	 * @return
	 */
	public Dataset getBySenderId(String senderId) {
		for (Dataset dataset : this) {
			if (senderId.equals(dataset.getSenderId())) {
				return dataset;
			}
		}
		return null;
	}
}

package dataset;

import webservice.GetDatasetList;

/**
 * Dcf dataset that is downloaded using the {@link GetDatasetList}
 * request.
 * @author avonva
 *
 */
//@XmlRootElement
public class Dataset {
	
	private String id;
	private String senderId;
	private DatasetStatus status;
	
	/**
	 * Set the dataset id
	 * @param id
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * Set the dataset sender id (as IT1708)
	 * @param senderId
	 */
	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}
	
	/**
	 * Set the dataset status
	 * @param status
	 */
	public void setStatus(DatasetStatus status) {
		this.status = status;
	}
	
	/**
	 * Get the dataset id
	 * @return
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Get the dataset sender id (id given
	 * by the data provider)
	 * @return
	 */
	public String getSenderId() {
		return senderId;
	}
	
	/**
	 * Get the dataset status
	 * @return
	 */
	public DatasetStatus getStatus() {
		return status;
	}

	/**
	 * Check if the dataset can be edited or not
	 * @return
	 */
	public boolean isEditable() {

		if (status == null)
			return false;

		return status == DatasetStatus.VALID || status == DatasetStatus.VALID_WITH_WARNINGS
				|| status == DatasetStatus.REJECTED_EDITABLE;
	}

	@Override
	public String toString() {
		return "Dataset: id=" + id + ";senderId=" + senderId + ";status=" + status;
	}
}

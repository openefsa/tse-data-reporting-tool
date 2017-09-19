package dataset;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Enumerator that identifies the status of a {@link Dataset}
 * @author avonva
 *
 */

@XmlRootElement
public enum DatasetStatus {
	
	VALID("Valid"),
	VALID_WITH_WARNINGS("Valid with warnings"),
	REJECTED_EDITABLE("Rejected editable"),
	REJECTED("Rejected"),
	DELETED("Deleted"),
	NOT_RECOGNIZED("Not recognized");
	
	private String status;
	private String step;
	
	private DatasetStatus(String status) {
		this.status = status;
	}
	
	/**
	 * Set the status step
	 * @param step
	 */
	public void setStep(String step) {
		this.step = step;
	}
	
	/**
	 * Get the raw status
	 * @return
	 */
	public String getStatus() {
		return status;
	}
	
	/**
	 * Get the status step
	 * @return
	 */
	public String getStep() {
		return step;
	}
	

	/**
	 * Get the enumerator that matches the {@code text}
	 * @param text
	 * @return
	 */
	public static DatasetStatus fromString(String text) {
		
		for (DatasetStatus b : DatasetStatus.values()) {
			if (b.status.equalsIgnoreCase(text)) {
				return b;
			}
		}
		
		return NOT_RECOGNIZED;
	}
}

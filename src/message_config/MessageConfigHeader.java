package message_config;

public enum MessageConfigHeader {

	XML_TAG("xmlTag"),
	VALUE("value"),
	PUT_IN_HEADER("putInHeader"),
	PUT_IN_OPERATION("putInOperation"),
	IS_DATASET_ROOT("isDatasetRoot");
	
	private String headerName;
	
	/**
	 * Initialize the enumerator with the real 
	 * header name that is present in the xlsx
	 * @param headerName
	 */
	private MessageConfigHeader(String headerName) {
		this.headerName = headerName;
	}
	
	/**
	 * Get the header name related to the enum field
	 * @return
	 */
	public String getHeaderName() {
		return headerName;
	}

	/**
	 * Get the enumerator that matches the {@code text}
	 * @param text
	 * @return
	 */
	public static MessageConfigHeader fromString(String text) {
		
		for (MessageConfigHeader b : MessageConfigHeader.values()) {
			if (b.headerName.equalsIgnoreCase(text)) {
				return b;
			}
		}
		return null;
	}
}

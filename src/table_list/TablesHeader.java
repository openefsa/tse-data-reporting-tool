package table_list;

public enum TablesHeader {
	
	TABLE_NAME("tableName"),
	HTML_FILENAME("htmlFileName"),
	GENERATE_RECORD("generateRecord");
	
	private String headerName;
	
	/**
	 * Initialize the enumerator with the real 
	 * header name that is present in the xlsx
	 * @param headerName
	 */
	private TablesHeader(String headerName) {
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
	public static TablesHeader fromString(String text) {
		
		for (TablesHeader b : TablesHeader.values()) {
			if (b.headerName.equalsIgnoreCase(text)) {
				return b;
			}
		}
		return null;
	}
}

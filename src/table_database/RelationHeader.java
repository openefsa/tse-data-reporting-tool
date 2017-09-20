package table_database;

public enum RelationHeader {
	
	PARENTTABLE("parentTable"),
	CHILDTABLE("childTable");
	
	private String headerName;
	
	/**
	 * Initialize the enumerator with the real 
	 * header name that is present in the xlsx
	 * @param headerName
	 */
	private RelationHeader(String headerName) {
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
	public static RelationHeader fromString(String text) {
		
		for (RelationHeader b : RelationHeader.values()) {
			if (b.headerName.equalsIgnoreCase(text)) {
				return b;
			}
		}
		return null;
	}
}

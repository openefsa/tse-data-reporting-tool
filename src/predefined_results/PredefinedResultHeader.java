package predefined_results;

public enum PredefinedResultHeader {
	
	// put the same content as header
	// in order to have a simple match
	// between enum and header
	RECORD_TYPE("recordType"),
	SOURCE("source"),
	CONFIRMATORY_EXECUTED("confirmatoryExecuted"),
	SAMP_EVENT_ASSES("sampEventAsses"),
	SAMP_EVENT_ASSES_LABEL("sampEventAssesLabel"),
	SCREENING("screening"),
	CONFIRMATORY("confirmatory"),
	DISCRIMINATORY("discriminatory"),
	GENOTYPING_BASE_TERM("genotypingBaseTerm");
	
	private String headerName;
	
	/**
	 * Initialize the enumerator with the real 
	 * header name that is present in the xlsx
	 * @param headerName
	 */
	private PredefinedResultHeader(String headerName) {
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
	public static PredefinedResultHeader fromString(String text) {

		for (PredefinedResultHeader b : PredefinedResultHeader.values()) {
			if (b.headerName.equalsIgnoreCase(text)) {
				return b;
			}
		}
		return null;
	}
}

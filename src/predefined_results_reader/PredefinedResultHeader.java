package predefined_results_reader;

public enum PredefinedResultHeader {
	
	// put the same content as header
	// in order to have a simple match
	// between enum and header
	RECORD_TYPE("recordType"),
	SAMP_AN_ASSES("sampAnAsses"),
	SAMP_AN_ASSES_LABEL("sampAnAsses-label"),
	SCREENING_PARAM_CODE("screeningParamCode"),
	SCREENING_RESULT("screeningResult"),
	CONFIRMATORY_PARAM_CODE("confirmatoryParamCode"),
	CONFIRMATORY_RESULT("confirmatoryResult"),
	DISCRIMINATORY_PARAM_CODE("discriminatoryParamCode"),
	DISCRIMINATORY_RESULT("discriminatoryResult"),
	GENOTYPING("genotyping");
	
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

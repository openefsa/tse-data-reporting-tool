package xlsx_reader;

public class ReportTableHeaders {
	
	public enum XlsxHeader {
		
		// put the same content as header
		// in order to have a simple match
		// between enum and header
		ID("id"),
		CODE("code"),
		XML_TAG("xmlTag"),
		LABEL("label"),
		TIP("tip"),
		TYPE("type"),
		MANDATORY("mandatory"),
		VISIBLE("visible"),
		PICKLISTKEY("picklistKey"),
		PICKLISTFILTER("picklistFilter"),
		EDITABLE("editable"),
		DEFAULTCODE("defaultCode"),
		DEFAULTVALUE("defaultValue"),
		PUTINOUTPUT("putInOutput"),
		ORDER("order");
		
		private String headerName;
		
		/**
		 * Initialize the enumerator with the real 
		 * header name that is present in the xlsx
		 * @param headerName
		 */
		private XlsxHeader(String headerName) {
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
		public static XlsxHeader fromString(String text) {
			
			for (XlsxHeader b : XlsxHeader.values()) {
				if (b.headerName.equalsIgnoreCase(text)) {
					return b;
				}
			}
			return null;
		}
	}
}

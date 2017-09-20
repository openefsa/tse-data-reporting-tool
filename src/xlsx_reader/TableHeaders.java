package xlsx_reader;

public class TableHeaders {
	
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
		PICKLIST_KEY("picklistKey"),
		PICKLIST_FILTER("picklistFilter"),
		EDITABLE("editable"),
		DEFAULT_CODE("defaultCode"),
		DEFAULT_VALUE("defaultValue"),
		PUT_IN_OUTPUT("putInOutput"),
		ORDER("order"),
		NATURAL_KEY("naturalKey");
		
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

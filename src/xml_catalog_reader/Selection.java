package xml_catalog_reader;

/**
 * Class which models a single node of a configuration .xml file
 * It also represents a single cell of the report (i.e. a value of a column of the report table)
 * @author avonva
 *
 */
public class Selection {
	
	private String listId;       // list in which the selection is present (BSE/SCRAPIE...)
	private String code;         // code of the selection (identifies a value of the list)
	private String description;  // label of the selection
	
	public void setListId(String listId) {
		this.listId = listId;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getListId() {
		return listId;
	}
	public String getCode() {
		return code;
	}
	public String getDescription() {
		return description;
	}
	
	@Override
	public boolean equals(Object arg0) {
		Selection other = (Selection) arg0;
		return other.code.equals(code);
	}
	
	public void print() {
		System.out.println("Code=" + code + ";value=" + description + ";listId=" + listId);
	}
	
	@Override
	public String toString() {
		return "<" + XmlNodes.SELECTION + " " + XmlNodes.SELECTION_CODE_ATTR + "=" + code + ">" 
					+ "<" + XmlNodes.DESCRIPTION + ">" + description + "</" + XmlNodes.DESCRIPTION + ">"
			+ "</" + XmlNodes.SELECTION + ">";
	}
}

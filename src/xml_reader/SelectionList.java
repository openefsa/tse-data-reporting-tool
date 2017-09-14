package xml_reader;

import java.util.ArrayList;
import java.util.Collection;

/**
 * List of {@link Selection}, this class represents the structure of a
 * configuration .xml file
 * @author avonva
 *
 */
public class SelectionList {

	private String listCode;                   // name of the main .xml node of the document
	private String id;                         // id of the selection list (e.g. BSE, SCRAPIE)
	private Collection<Selection> selections;  // list of selections contained in the .xml
	
	public SelectionList() {
		selections = new ArrayList<>();
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setListCode(String listCode) {
		this.listCode = listCode;
	}
	
	/**
	 * Add a new selection to the attribute list
	 * @param selection
	 */
	public void add(Selection selection) {
		selections.add(selection);
	}
	
	/**
	 * Get the code of the main .xml node
	 * @return
	 */
	public String getListCode() {
		return listCode;
	}
	
	/**
	 * Get the id of the list
	 * @return
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Get all the selections contained in the list
	 * @return
	 */
	public Collection<Selection> getSelections() {
		return selections;
	}
	
	public Selection getSelectionByCode(String code) {
		for (Selection sel : selections) {
			if (sel.getCode().equals(code))
				return sel;
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return "<" + XmlNodes.SELECTION_LIST + " " + XmlNodes.SELECTION_LIST_ID_ATTR + "=" + id + ">" 
					+ selections 
				+ "</" + XmlNodes.SELECTION_LIST + ">";
	}
}

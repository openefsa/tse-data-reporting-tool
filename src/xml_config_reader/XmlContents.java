package xml_config_reader;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class which represents an entire .xml configuration document
 * @author avonva
 *
 */
public class XmlContents {

	private String code;                         // code of the main node of the .xml
	private Collection<SelectionList> elements;  // list of selection lists of the .xml
	
	public XmlContents() {
		elements = new ArrayList<>();
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public void addElement(SelectionList elem) {
		elements.add(elem);
	}
	
	public String getCode() {
		return code;
	}
	
	public Collection<SelectionList> getElements() {
		return elements;
	}
	
	public SelectionList getList() {

		if (elements.isEmpty())
			return null;
		
		return elements.iterator().next();
	}
	
	/**
	 * Get all the elements of all the lists of the xml
	 * @return
	 */
	public Collection<Selection> getAllListsElements() {
		
		Collection<Selection> objs = new ArrayList<>();
		for (SelectionList list : elements) {
			objs.addAll(list.getSelections());
		}
		
		return objs;
	}
	
	/**
	 * Get an element of the .xml document using its code,
	 * independently on the lists ids
	 * @param code
	 * @return
	 */
	public Selection getElementByCode(String code) {
		Collection<Selection> objs = getAllListsElements();

		for (Selection sel : objs) {
			if (sel.getCode().equals(code))
				return sel;
		}
		
		return null;
	}
	
	/**
	 * Get all the elements of one list of the xml
	 * @return
	 */
	public Collection<Selection> getListElements(String listId) {
		
		Collection<Selection> objs = new ArrayList<>();
		for (SelectionList list : elements) {
			if(list.getId().equals(listId))
				objs.addAll(list.getSelections());
		}
		
		return objs;
	}
	
	/**
	 * Filter the xml lists and get just the one that
	 * matches the passed id.
	 * @param id
	 * @return
	 */
	public SelectionList getListById(String id) {
		
		for (SelectionList list : elements) {
			if (list.getId().equals(id))
				return list;
		}
		
		return null;
	}

	public int size() {
		return elements.size();
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(obj instanceof String) {
			return code.equals((String) obj);
		}

		return super.equals(obj);
	}
	
	@Override
	public String toString() {
		return ("<" + code + ">" + elements + "</" + code + ">").replace("[", "").replace("]", "");
 	}
}

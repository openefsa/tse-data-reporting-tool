package table_dialog;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import tse_config.CatalogLists;
import xml_catalog_reader.Selection;
import xml_catalog_reader.SelectionList;
import xml_catalog_reader.XmlContents;
import xml_catalog_reader.XmlLoader;

/**
 * Combo box which allows selecting a {@link Selection} object from an .xml list.
 * The data are retrieved from the .xml in the data folder.
 * Please specify in {@link #getSelectionListId()} the code of the
 * .xml which contains the list of interest (see {@link CatalogLists})
 * @author avonva
 *
 */
public class CatalogComboViewer {

	private Composite parent;
	private Composite composite;
	private ComboViewer comboBox;

	private String selectionListId;  // filter for the list
	
	/**
	 * Create a combo box which allows selecting a monitoring
	 * method
	 * @param parent
	 */
	public CatalogComboViewer(Composite parent) {
		this.parent = parent;
		create();
	}
	
	/**
	 * Create the combo box and set its input
	 */
	private void create() {
		
		this.composite = new Composite(parent, SWT.NONE);
		this.composite.setLayout(new FillLayout());
		
		this.comboBox = new ComboViewer(composite, SWT.READ_ONLY);
		this.comboBox.setContentProvider(new ContentProvider());
		this.comboBox.setLabelProvider(new LabelProvider());
	}
	
	/**
	 * Set an xml list for the combo box. All the values in the
	 * list will be picked up. If a filter needs to be set, 
	 * please see {@link #setList(String, String)}.
	 * @param selectionListCode
	 */
	public void setList(String selectionListCode) {
		setList(selectionListCode, null);
	}
	
	/**
	 * Set an xml list for the combo box and get only a subset
	 * identified by the selectionId. The selection id identifies
	 * a sub node of the xml list and allows taking just the values
	 * under the matched node.
	 * @param selectionListCode
	 * @param selectionId
	 */
	public void setList(String selectionListCode, String selectionId) {
		
		this.selectionListId = selectionId;
		
		// get the items
		XmlContents items = XmlLoader.getByPicklistKey(selectionListCode);
		
		if (items == null) {
			System.err.println("No file found for: " + selectionListCode);
			return;
		}

		
		// set the input
		this.comboBox.setInput(items);
		
		if (items.size() > 0)
			this.comboBox.getCombo().select(0);
	}
	
	/**
	 * Enable/disable the combo box
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		this.comboBox.getCombo().setEnabled(enabled);
	}
	
	/**
	 * Set a listener which is called if the combo box selection
	 * is changed
	 * @param listener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		comboBox.addSelectionChangedListener(listener);
	}
	
	/**
	 * Get the selected item
	 * @return
	 */
	public Selection getSelectedItem() {
		IStructuredSelection selection = (IStructuredSelection) this.comboBox.getSelection();
		
		if(selection.isEmpty())
			return null;
		
		return (Selection) selection.getFirstElement();
	}
	
	private class ContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {}

		@Override
		public Object[] getElements(Object arg0) {
			
			// set xml selection lists as elements
			XmlContents contents = (XmlContents) arg0;
			
			Collection<Selection> elements = new ArrayList<>();
			
			// Add all the selections to the elements list
			for (SelectionList list : contents.getElements()) {
				
				// filter on the selection id
				if (selectionListId == null || list.getId().equals(selectionListId))
					elements.addAll(list.getSelections());
			}
			
			return elements.toArray();
		}
	}
	
	private class LabelProvider implements ILabelProvider {

		@Override
		public void addListener(ILabelProviderListener arg0) {}

		@Override
		public void dispose() {}

		@Override
		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener arg0) {}

		@Override
		public Image getImage(Object arg0) {
			return null;
		}

		@Override
		public String getText(Object arg0) {

			Selection selection = (Selection) arg0;
			
			// show description of tse
			return selection.getDescription();
		}
	}
}

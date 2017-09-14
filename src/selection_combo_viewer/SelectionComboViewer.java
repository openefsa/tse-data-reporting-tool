package selection_combo_viewer;

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

import app_config.SelectionsNames;
import xml_reader.Selection;
import xml_reader.SelectionList;
import xml_reader.XmlContents;
import xml_reader.XmlLoader;

/**
 * Combo box which allows selecting a {@link Selection} object from a list.
 * The data are retrieved from the .xml in the data folder.
 * Please specify in {@link #getSelectionListId()} the code of the
 * .xml which contains the list of interest (see {@link SelectionsNames})
 * @author avonva
 *
 */
public class SelectionComboViewer {

	private Composite parent;
	private Composite composite;
	private ComboViewer comboBox;
	
	private String selectionListCode;
	private String selectionListId;
	
	/**
	 * Create a combo box which allows selecting a monitoring
	 * method
	 * @param parent
	 */
	public SelectionComboViewer(Composite parent, String selectionListCode) {
		this(parent, selectionListCode, null);
	}
	
	public SelectionComboViewer(Composite parent, String selectionListCode, String selectionListId) {
		this.parent = parent;
		this.selectionListCode = selectionListCode;
		this.selectionListId = selectionListId;
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
		
		// get list elements
		XmlContents items = getItems();
		
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
	
	/**
	 * Get the items of the combo box
	 * @return
	 */
	private XmlContents getItems() {
		return XmlLoader.getByPicklistKey(selectionListCode);
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

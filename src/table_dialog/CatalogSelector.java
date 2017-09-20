package table_dialog;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import xml_catalog_reader.Selection;

/**
 * Class which contains a {@link CatalogComboViewer} and a {@link Button}.
 * The list contains the list of TSE diseases which can be reported.
 * It is possible to select a disease and to confirm the selection by
 * pressing the button. If set, a listener is called when the selection
 * in the list is changed or when the button is pressed (see {@link CatalogChangedListener}).
 * @author avonva
 *
 */
public class CatalogSelector {

	private Composite parent;
	private Composite composite;
	private CatalogComboViewer catalogComboViewer;
	private Button selectBtn;
	
	private Label title;
	
	/**
	 * Create object with tse list and button to confirm a selection
	 * @param parent
	 */
	public CatalogSelector(Composite parent) {
		this.parent = parent;
		create();
	}
	
	/**
	 * Set the label text
	 * @param text
	 */
	public void setLabelText(String text) {
		this.title.setText(text);
	}
	
	/**
	 * Set an xml list for the combo box. All the values in the
	 * list will be picked up. If a filter needs to be set, 
	 * please see {@link #setList(String, String)}.
	 * @param selectionListCode
	 */
	public void setList(String selectionListCode) {
		this.catalogComboViewer.setList(selectionListCode);
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
		this.catalogComboViewer.setList(selectionListCode, selectionId);
	}
	
	
	/**
	 * Enable/disable the panel
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		this.selectBtn.setEnabled(enabled);
		this.catalogComboViewer.setEnabled(enabled);
	}
	
	/**
	 * Create ui
	 */
	private void create() {
		
		this.composite = new Composite(parent, SWT.NONE);
		this.composite.setLayout(new GridLayout(3,false));
		
		this.title = new Label(composite, SWT.NONE);
		
		// combo box with selectable elements
		this.catalogComboViewer = new CatalogComboViewer(composite);
		
		// button to confirm selection
		this.selectBtn = new Button(composite, SWT.PUSH);
		
		// add plus icon to the button
		Image image = new Image(Display.getCurrent(), 
				this.getClass().getClassLoader().getResourceAsStream("add-icon.png"));
		
		this.selectBtn.setImage(image);
	}

	/**
	 * Add a {@link CatalogChangedListener} to the object
	 * @param listener
	 */
	public void addSelectionListener(final CatalogChangedListener listener) {
		
		// add listener to the button
		this.selectBtn.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				listener.catalogConfirmed(catalogComboViewer.getSelectedItem());
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		// add listener to the list
		catalogComboViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				listener.catalogChanged(catalogComboViewer.getSelectedItem());
			}
		});
	}

	/**
	 * Listener which is called when the selection changes or
	 * when the button is pressed.
	 * @author avonva
	 *
	 */
	public interface CatalogChangedListener {
		/**
		 * Called when an element of the list is selected
		 * @param selectedItem
		 */
		public void catalogChanged(Selection selectedItem);
		
		/**
		 * Called when the button is clicked
		 * @param selectedItem
		 */
		public void catalogConfirmed(Selection selectedItem);
	}
}

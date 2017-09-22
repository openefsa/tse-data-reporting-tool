package table_dialog;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import table_dialog.TableViewWithHelp.RowCreationMode;
import xml_catalog_reader.Selection;

/**
 * This class is an UI that allows triggering an action by pressing a button.
 * In particular, it is also possible to choose an element from a list before
 * triggering the action (this is usually used to create a new row, where sometimes
 * you need to add an initial information to the row).
 * <ul>
 * 	<li>UI with button: select {@link #mode} to {@link RowCreationMode#STANDARD}</li>
 * 	<li>UI with catalogue list and button: select {@link #mode} to {@link RowCreationMode#SELECTOR}</li>
 * </ul>
 * Note that when a catalogue value is selected {@link CatalogChangedListener#catalogChanged(Selection)}
 * is called. Moreover, if the button is pressed {@link CatalogChangedListener#catalogConfirmed(Selection)}
 * is called.
 * To select a catalogue call {@link #setList(String)}.
 * Note that also a label is shown on the left of the catalogue selector/button if {@link #setLabelText(String)}
 * is called.
 * @author avonva
 *
 */
public class RowCreatorViewer {

	private Composite parent;
	private Composite composite;
	private CatalogComboViewer catalogComboViewer;
	private Button selectBtn;
	
	private Label title;
	
	private RowCreationMode mode;
	
	/**
	 * Create object with tse list and button to confirm a selection
	 * note that in order to make it visible, you need to set a
	 * label or a list with {@link #setLabelText(String)} or
	 * {@link #setList(String)}
	 * @param parent
	 */
	public RowCreatorViewer(Composite parent, RowCreationMode mode) {
		this.parent = parent;
		this.mode = mode;
		create();
	}
	
	private void open() {
		this.composite.setVisible(true);
		((GridData) this.composite.getLayoutData()).exclude = false;
		this.composite.getParent().layout();
	}
	
	/**
	 * Set the label text
	 * @param text
	 */
	public void setLabelText(String text) {
		this.title.setText(text);
		open();
	}
	
	/**
	 * Set an xml list for the combo box. All the values in the
	 * list will be picked up. If a filter needs to be set, 
	 * please see {@link #setList(String, String)}.
	 * @param selectionListCode
	 */
	public void setList(String selectionListCode) {
		this.catalogComboViewer.setList(selectionListCode);
		open();
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
		open();
	}
	
	
	/**
	 * Enable/disable the panel
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {

		this.selectBtn.setEnabled(enabled);
		
		if (mode == RowCreationMode.SELECTOR)
			this.catalogComboViewer.setEnabled(enabled);
	}
	
	/**
	 * Create ui
	 */
	private void create() {
		
		GridData gd = new GridData();
		gd.exclude = true;
		
		this.composite = new Composite(parent, SWT.NONE);
		this.composite.setLayout(new GridLayout(3,false));
		
		// make composite invisible
		this.composite.setVisible(false);
		this.composite.setLayoutData(gd);
		
		this.title = new Label(composite, SWT.NONE);
		
		if (mode == RowCreationMode.SELECTOR) {
			// combo box with selectable elements
			this.catalogComboViewer = new CatalogComboViewer(composite);
		}
		
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
				
				Selection selectedElem = null;
				
				if (mode == RowCreationMode.SELECTOR)
					selectedElem = catalogComboViewer.getSelectedItem();
				
				listener.catalogConfirmed(selectedElem);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		
		
		if (mode != RowCreationMode.SELECTOR)
			return;
		
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

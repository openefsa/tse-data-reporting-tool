package user_components;

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

import table_dialog.CatalogValuePicker;
import user_config.SelectionsNames;
import xml_catalog_reader.Selection;

/**
 * Class which contains a {@link TseComboViewer} and a {@link Button}.
 * The list contains the list of TSE diseases which can be reported.
 * It is possible to select a disease and to confirm the selection by
 * pressing the button. If set, a listener is called when the selection
 * in the list is changed or when the button is pressed (see {@link SelectorListener}).
 * @author avonva
 *
 */
public class SelectorViewer {

	private Composite parent;
	private Composite composite;
	private CatalogValuePicker list;
	private Button selectBtn;
	
	/**
	 * Create object with tse list and button to confirm a selection
	 * @param parent
	 */
	public SelectorViewer(Composite parent) {
		this.parent = parent;
		create();
	}
	
	/**
	 * Enable/disable the panel
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		this.selectBtn.setEnabled(enabled);
		this.list.setEnabled(enabled);
	}
	
	/**
	 * Create ui
	 */
	private void create() {
		
		this.composite = new Composite(parent, SWT.NONE);
		this.composite.setLayout(new GridLayout(3,false));
		
		Label title = new Label(composite, SWT.NONE);
		title.setText("Add data related to monitoring of:");
		
		// combo box with selectable elements
		this.list = new CatalogValuePicker(composite, 
				SelectionsNames.TSE_LIST);
		
		// button to confirm selection
		this.selectBtn = new Button(composite, SWT.PUSH);
		
		// add plus icon to the button
		Image image = new Image(Display.getCurrent(), 
				this.getClass().getClassLoader().getResourceAsStream("add-icon.png"));
		
		this.selectBtn.setImage(image);
	}

	/**
	 * Add a {@link SelectorListener} to the object
	 * @param listener
	 */
	public void addSelectionListener(final SelectorListener listener) {
		
		// add listener to the button
		this.selectBtn.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				listener.selectionConfirmed(list.getSelectedItem());
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		// add listener to the list
		list.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				listener.selectionChanged(list.getSelectedItem());
			}
		});
	}

	/**
	 * Listener which is called when the selection changes or
	 * when the button is pressed.
	 * @author avonva
	 *
	 */
	public interface SelectorListener {
		/**
		 * Called when an element of the list is selected
		 * @param selectedItem
		 */
		public void selectionChanged(Selection selectedItem);
		
		/**
		 * Called when the button is clicked
		 * @param selectedItem
		 */
		public void selectionConfirmed(Selection selectedItem);
	}
}

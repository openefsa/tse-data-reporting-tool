package table_dialog;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;

import table_dialog.CatalogSelector.CatalogChangedListener;
import table_skeleton.TableRow;
import xlsx_reader.TableSchema;

/**
 * {@link TableView} and {@link HelpViewer} packed together.
 * {@link CatalogSelector} can also be added by setting
 * {@link #addSelector} to true in the constructor.
 * @author avonva
 *
 */
public class TableViewWithHelp {

	private Composite parent;
	private Composite composite;
	private String schemaSheetName;
	private String helpMessage;
	private boolean editable;
	private boolean addSelector;
	
	private HelpViewer helpViewer;
	private CatalogSelector catalogSelector;
	private TableView table;
	
	public TableViewWithHelp(Composite parent, String schemaSheetName, 
			String helpMessage) {
		this(parent, schemaSheetName, helpMessage, true, false);
	}
	
	public TableViewWithHelp(Composite parent, String schemaSheetName, 
			String helpMessage, boolean editable) {
		this(parent, schemaSheetName, helpMessage, editable, false);
	}
	
	public TableViewWithHelp(Composite parent, String schemaSheetName, 
			String helpMessage, boolean editable, boolean addSelector) {
		this.parent = parent;
		this.schemaSheetName = schemaSheetName;
		this.helpMessage = helpMessage;
		this.editable = editable;
		this.addSelector = addSelector;
		create();
	}
	
	public void create() {

		this.composite = new Composite(parent, SWT.NONE);
		this.composite.setLayout(new GridLayout(1,false));
		this.composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		this.helpViewer = new HelpViewer(composite, helpMessage);
		
		// add also the selector if required
		if (addSelector) {
			this.catalogSelector = new CatalogSelector(composite);
			this.catalogSelector.setEnabled(false);
		}
		
		this.table = new TableView(composite, schemaSheetName, editable);
	}
	
	/**
	 * Add listener to the help image
	 * @param listener
	 */
	public void addHelpListener(MouseListener listener) {
		this.helpViewer.setListener(listener);
	}
	
	/**
	 * Enable/disable the creation of new records
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		if (addSelector)
			this.catalogSelector.setEnabled(enabled);
	}
	
	public void setMenu(Menu menu) {
		table.setMenu(menu);
	}
	
	public void add(TableRow row) {
		table.add(row);
	}
	
	public void addAll(Collection<TableRow> row) {
		table.addAll(row);
	}
	
	public void clearTable() {
		table.clear();
	}
	
	public void removeSelectedRow() {
		table.removeSelectedRow();
	}
	
	/**
	 * Set the label text
	 * @param text
	 */
	public void setSelectorLabelText(String text) {
		
		if (!addSelector)
			return;
		
		this.catalogSelector.setLabelText(text);
	}
	
	/**
	 * Set an xml list for the combo box. All the values in the
	 * list will be picked up. If a filter needs to be set, 
	 * please see {@link #setSelectorList(String, String)}.
	 * @param selectionListCode
	 */
	public void setSelectorList(String selectionListCode) {
		
		if (!addSelector)
			return;
		
		this.catalogSelector.setList(selectionListCode);
	}
	
	/**
	 * Set an xml list for the combo box and get only a subset
	 * identified by the selectionId. The selection id identifies
	 * a sub node of the xml list and allows taking just the values
	 * under the matched node.
	 * @param selectionListCode
	 * @param selectionId
	 */
	public void setSelectorList(String selectionListCode, String selectionId) {
		
		if (!addSelector)
			return;
		
		this.catalogSelector.setList(selectionListCode, selectionId);
	}
	
	/**
	 * Listener called when the input of the table
	 * changes
	 * @param inputChangedListener
	 */
	public void setInputChangedListener(Listener inputChangedListener) {
		table.setInputChangedListener(inputChangedListener);
	}
	
	/**
	 * Listener called when the selection in the table changes
	 * @param listener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		this.table.addSelectionChangedListener(listener);
	}
	
	public TableView getTable() {
		return table;
	}
	
	public boolean areMandatoryFilled() {
		return table.areMandatoryFilled();
	}
	
	public TableRow getSelection() {
		return table.getSelection();
	}
	
	public ArrayList<TableRow> getTableElements() {
		return table.getTableElements();
	}
	
	public TableSchema getSchema() {
		return table.getSchema();
	}
	
	public String getHelpMessage() {
		return helpMessage;
	}
	
	public CatalogSelector getTypeSelector() {
		return catalogSelector;
	}
	
	public boolean isTableEmpty() {
		return this.table.isEmpty();
	}
	
	/**
	 * Listener called when the {@link #catalogSelector}
	 * changes selection
	 * @param listener
	 */
	public void addSelectionListener(CatalogChangedListener listener) {
		if (addSelector) {
			this.catalogSelector.addSelectionListener(listener);
		}
	}
	
	public HelpViewer getHelpViewer() {
		return helpViewer;
	}
}

package table_dialog;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;

import table_skeleton.TableRow;
import user_components.SelectorViewer;
import user_components.SelectorViewer.SelectorListener;
import xlsx_reader.TableSchema;

/**
 * {@link TableView} and {@link HelpViewer} packed together.
 * {@link SelectorViewer} can also be added by setting
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
	private SelectorViewer typeSelector;
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
			this.typeSelector = new SelectorViewer(composite);
			this.typeSelector.setEnabled(false);
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
			this.typeSelector.setEnabled(enabled);
	}
	
	public void setMenu(Menu menu) {
		table.setMenu(menu);
	}
	
	public void add(TableRow row) {
		table.add(row);
	}
	
	public void clearTable() {
		table.clear();
	}
	
	public void removeSelectedRow() {
		table.removeSelectedRow();
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
	
	public SelectorViewer getTypeSelector() {
		return typeSelector;
	}
	
	public boolean isTableEmpty() {
		return this.table.isEmpty();
	}
	
	/**
	 * Listener called when the {@link #typeSelector}
	 * changes selection
	 * @param listener
	 */
	public void addSelectionListener(SelectorListener listener) {
		if (addSelector) {
			this.typeSelector.addSelectionListener(listener);
		}
	}
	
	public HelpViewer getHelpViewer() {
		return helpViewer;
	}
}

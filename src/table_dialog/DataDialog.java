package table_dialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import database.TableDao;
import table_dialog.SelectorViewer.SelectorListener;
import table_skeleton.TableRow;
import xlsx_reader.TableSchema;
import xml_config_reader.Selection;

/**
 * Generic dialog for showing a table that follows a {@link TableSchema}.
 * In particular, it is possible to extend this class to show whatever
 * table that is described in the {@link AppPaths#APP_CONFIG_FILE} .xslx file.
 * Features:
 * 
 * - automatic creation of table with {@link TableColumn} which are visible
 * 
 * - automatic generation of {@link HelpViewer} to show help to the user
 * 
 * - automatic generation of {@link SelectorViewer} which allows creating
 * new rows in the table by selecting a parameter from a list. Need to be
 * specified by setting {@link #addSelector} to true. If the plus icon
 * is pressed, the {@link #createNewRow(TableSchema, Selection)} method
 * is called
 * 
 * - automatic generation of a {@link Button} to apply changes. Need to be
 * specified by setting {@link #addSaveBtn} to true. If the button is
 * pressed the {@link #apply(TableSchema, Collection, TableRow)} method
 * is called
 * 
 * - can specify if the table should be shown in a new dialog or in an
 * already existing shell by setting {@link #createPopUp} accordingly
 * @author avonva
 *
 */
public abstract class DataDialog {

	private Shell parent;
	private Shell dialog;
	private ReportTableWithHelp panel;
	private Button saveButton;
	
	private TableRow parentTable;
	private TableSchema schema;
	
	private String title;
	private String helpMessage;
	private boolean editable;
	private boolean addSelector;
	private boolean createPopUp;
	private boolean addSaveBtn;
	
	/**
	 * Create a dialog with a {@link HelpViewer}, a {@link ReportTable}
	 * and possibly a {@link SelectorViewer} if {@code addSelector} is set to true.
	 * @param parent the shell parent
	 * @param title the title of the pop up
	 * @param helpMessage the help message to be displayed in the {@link HelpViewer}
	 * @param editable if the table can be edited or not
	 * @param addSelector if the {@link SelectorViewer} should be added or not
	 * @param createPopUp true to create a new shell, false to use the parent shell
	 * @param addSaveBtn true to create a button below the table
	 */
	public DataDialog(Shell parent, String title, String helpMessage, boolean editable, 
			boolean addSelector, boolean createPopUp, boolean addSaveBtn) {

		this.parent = parent;
		this.title = title;
		this.helpMessage = helpMessage;
		this.editable = editable;
		this.addSelector = addSelector;
		this.createPopUp = createPopUp;
		this.addSaveBtn = addSaveBtn;
		
		try {
			this.schema = TableSchema.load(getSchemaSheetName());
			create();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public DataDialog(Shell parent, String title, String helpMessage, boolean editable, 
			boolean addSelector, boolean createPopUp) {
		this(parent, title, helpMessage, editable, addSelector, createPopUp, true);
	}
	
	public DataDialog(Shell parent, String title, String helpMessage, boolean editable, 
			boolean addSelector) {
		this(parent, title, helpMessage, editable, addSelector, true, true);
	}
	
	public DataDialog(Shell parent, String title, String helpMessage, boolean editable) {
		this(parent, title, helpMessage, editable, false, true, true);
	}
	
	public DataDialog(Shell parent, String title, String helpMessage) {
		this(parent, title, helpMessage, true, false, true, true);
	}
	
	public TableSchema getSchema() {
		return schema;
	}
	
	/**
	 * If the current table is in many to one relation
	 * with a parent table, then use this method to
	 * set the parent. This will be passed then to the
	 * {@link #loadContents(TableSchema)} methods
	 * in order to allow loading just the records
	 * related to the parent table.
	 * @param parentTable
	 */
	public void setParentTable(TableRow parentTable) {
		this.parentTable = parentTable;
		loadRows();
	}
	
	/**
	 * Clear all the table rows
	 * and the parent table object
	 */
	public void clear() {
		
		panel.clearTable();
		this.parentTable = null;
		
		// disable the panel
		if (addSelector)
			this.panel.setEnabled(false);
	}
	
	/**
	 * Load the rows which are defined in the {@link #getRows(TableSchema, TableRow)}
	 * method
	 */
	public void loadRows() {
		
		panel.clearTable();
		
		// load the rows
		Collection<TableRow> rows = getRows(panel.getSchema(), parentTable);

		// add them to the table
		for (TableRow row : rows) {
			panel.add(row);
		}
	}
	
	public TableRow getParentTable() {
		return parentTable;
	}
	
	/**
	 * Warn the user with an ERROR message box
	 * @param title
	 * @param message
	 * @param icon
	 */
	protected void warnUser(String title, String message, int icon) {
		MessageBox mb = new MessageBox(getDialog(), icon);
		mb.setText(title);
		mb.setMessage(message);
		mb.open();
	}
	
	/**
	 * Warn the user with a message box with custom icon
	 * @param title
	 * @param message
	 */
	protected void warnUser(String title, String message) {
		MessageBox mb = new MessageBox(getDialog(), SWT.ICON_ERROR);
		mb.setText(title);
		mb.setMessage(message);
		mb.open();
	}
	
	/**
	 * Open the dialog and wait that it is closed
	 */
	public void open() {
		
		this.dialog.open();
		
		// Event loop
		while ( !dialog.isDisposed() ) {
			if ( !dialog.getDisplay().readAndDispatch() )
				dialog.getDisplay().sleep();
		}
	}
	
	/**
	 * Set the text of the button
	 * @param text
	 */
	public void setButtonText(String text) {
		
		if (!addSaveBtn) {
			System.err.println("DataDialog-" + getSchemaSheetName() + ":Cannot set the button text to " 
					+ text + " since the button was not created. Please set addSaveBtn to true");
			return;
		}
		
		this.saveButton.setText(text);
		this.saveButton.pack();
	}
	
	/**
	 * Create the interface
	 */
	private void create() {

		// new shell if required
		if (createPopUp)
			this.dialog = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		else
			this.dialog = parent;
		
		this.dialog.setText(this.title);
		
		this.dialog.setLayout(new GridLayout(1,false));
		this.dialog.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		this.panel = new ReportTableWithHelp(dialog, getSchemaSheetName(), helpMessage, editable, addSelector);
		this.panel.setMenu(createMenu());
		
		// add selector functionalities if possible
		addSelectorFunctionalities();
		
		// load all the rows into the table
		loadRows();

		if (addSaveBtn) {
			// save button
			this.saveButton = new Button(dialog, SWT.PUSH);
			this.saveButton.setText("Save");
			this.saveButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
			this.saveButton.setEnabled(panel.areMandatoryFilled());
			
			// save options
			this.saveButton.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					
					boolean ok = apply(panel.getSchema(), panel.getTableElements(), panel.getSelection());
					
					if (ok)
						dialog.close();
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {}
			});
			
			
			panel.setInputChangedListener(new Listener() {

				@Override
				public void handleEvent(Event arg0) {
					saveButton.setEnabled(panel.areMandatoryFilled());
				}
			});
		}
		
		dialog.pack();
		
		// make dialog longer
		dialog.setSize(dialog.getSize().x, dialog.getSize().y + 50);
	}
	
	/**
	 * Add the selector functionalities to create a new row
	 * when the + is pressed
	 */
	private void addSelectorFunctionalities() {
		
		if (!addSelector)
			return;
		
		// add a selection listener to the selector
		this.addSelectionListener(new SelectorListener() {

			@Override
			public void selectionConfirmed(Selection selectedItem) {

				// create a new row and
				// put the first cell in the row
				try {
					
					TableRow row = createNewRow(getSchema(), selectedItem);
					
					// if a parent was set, then add also the foreign key to the
					// current new row
					if (parentTable != null) {
						String reportForeignKey = getSchema()
								.getRelationByParentTable(parentTable.getSchema().getSheetName()).getForeignKey();
						row.put(reportForeignKey, parentTable.get(reportForeignKey));
					}
					
					// insert the row and get the row id
					TableDao dao = new TableDao(getSchema());
					int id = dao.add(row);
					
					// set the id for the new row
					row.setId(id);
					
					// update the formulas
					row.updateFormulas();
					
					// update the row with the formulas solved
					dao.update(row);
					
					// add the row to the table
					add(row);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void selectionChanged(Selection selectedItem) {}
		});
		
	}
	
	/**
	 * Add listener to the selector if it was added (i.e. {@link #addSelector} true)
	 * @param listener
	 */
	public void addSelectionListener(SelectorListener listener) {
		if (addSelector) {
			this.panel.addSelectionListener(listener);
		}
	}
	
	public void addTableSelectionListener(ISelectionChangedListener listener) {
		this.panel.addSelectionChangedListener(listener);
	}
	
	/**
	 * Check if the table is empty or not
	 * @return
	 */
	public boolean isTableEmpty() {
		return this.panel.isTableEmpty();
	}
	
	/**
	 * Enable/disable the creation of new records
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		if (addSelector)
			this.panel.setEnabled(enabled);
	}
	
	public Shell getDialog() {
		return dialog;
	}
	
	/**
	 * Add a row to the table
	 * @param row
	 */
	public void add(TableRow row) {
		panel.add(row);
	}
	
	/**
	 * Remove from the table the selected row
	 */
	public void removeSelectedRow() {
		panel.removeSelectedRow();
	}
	
	/**
	 * Get the record of the current table which are 
	 * related to the parent table
	 * @return
	 */
	public Collection<TableRow> getParentRows() {
		
		Collection<TableRow> rows = new ArrayList<>();

		if (parentTable == null)
			return rows;
		
		this.setEnabled(true);
		
		// load parents rows
		TableDao dao = new TableDao(schema);
		rows = dao.getByParentId(parentTable.getSchema().getSheetName(), parentTable.getId());

		return rows;
	}
	
	/**
	 * Get the sheet which includes the schema for the table
	 * @return
	 */
	public abstract String getSchemaSheetName();
	
	/**
	 * Create a menu for the table
	 * @return
	 */
	public abstract Menu createMenu();
	
	/**
	 * Get the rows of the table when it is created
	 * @param schema the table schema
	 * @param parentTable the parent related to this table passed with the
	 * method {@link #setParentTable(TableRow)}
	 * @return
	 */
	public abstract Collection<TableRow> getRows(TableSchema schema, TableRow parentTable);
	
	/**
	 * Create a new row with default values and foreign keys when the selector
	 * is used. Note that {@link #addSelector} should be set to true, otherwise
	 * no selector will be available and this method will never be called
	 * @param element the element which is currently selected in the selector
	 * @return
	 * @throws IOException 
	 */
	public abstract TableRow createNewRow(TableSchema schema, Selection type) throws IOException;
	
	/**
	 * Apply the changes that were made when the {@link #saveButton} is
	 * pressed. Note that the {@link #addSaveBtn} should be set to true
	 * to show the button, otherwise this method will never be called.
	 * @param schema the table schema
	 * @param rows the table rows
	 * @param selectedRow the current selected row in the table (null if no selection)
	 */
	public abstract boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow);
}


package table_dialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import export.DatasetXmlCreator;
import html_viewer.Help;
import table_database.Relation;
import table_database.TableDao;
import table_dialog.RowCreatorViewer.CatalogChangedListener;
import table_dialog.TableViewWithHelp.RowCreationMode;
import table_skeleton.TableRow;
import tse_config.CustomPaths;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Generic dialog for showing a table that follows a {@link TableSchema}.
 * 
 * Each custom dialog that needs to show the content of a table of this type
 * should extend this class.
 * 
 * In particular, remember that only the tables identified in the 
 * {@link CustomPaths#APP_CONFIG_FILE} .xslx file are considered.
 * 
 * Features that are provided by this class:
 * <ul>
 * <li>automatic creation of table with {@link TableColumn} which are related
 * to a column which is visible.</li>
 * 
 * <li>automatic editor added to all the columns which are marked as editable
 * according to the column type.</li>
 * 
 * <li>automatic generation of {@link HelpViewer} to show help to the user
 * with .html files.</li>
 * 
 * <li>automatic generation of new rows by pressing a button:
 * 	<ul>
 * 		<li>It is possible to add a simple button by setting {@link #mode} to {@link RowCreationMode#STANDARD}
 * 		in the constructor of the class.
 * 		<li>If instead it is needed to first select an element from a catalogue to initialize
 * 		a new row, then set {@link #mode} to {@link RowCreationMode#SELECTOR}, in order
 * 		to have a {@link RowCreatorViewer} which allows creating
 * 		new rows in the table by selecting a parameter from a list. 
 * 		<li>In both cases if the plus icon is pressed, the {@link #createNewRow(TableSchema, Selection)} 
 * 		method is called. To disable the addition of new rows, simply put 
 * 		as {@link #mode} the value {@link RowCreationMode#NONE}.</li>
 * 	</ul>
 * 
 * <li>automatic generation of a {@link Button} to apply changes. Need to be
 * specified by setting {@link #addSaveBtn} to true. If the button is
 * pressed the {@link #apply(TableSchema, Collection, TableRow)} method
 * is called.</li>
 * 
 * <li>You can specify if the table should be shown in a new dialog or in an
 * already existing shell by setting {@link #createPopUp} accordingly.</li>
 * </ul>
 * @author avonva
 *
 */
public abstract class TableDialog {
	
	private Shell parent;
	private Shell dialog;
	private TableViewWithHelp panel;
	private Button saveButton;
	
	private TableRow parentFilter;              // if set, only the rows children of this parent are shown in the table
	private Collection<TableRow> parentTables;  // list of parents of the table in 1-n relations
	private TableSchema schema;                 // schema of the table that is shown
	
	private String title;
	private String helpMessage;
	private boolean editable;
	private RowCreationMode mode;
	private boolean createPopUp;
	private boolean addSaveBtn;
	
	/**
	 * Create a dialog with a {@link HelpViewer}, a {@link TableView}
	 * and possibly a {@link RowCreatorViewer} if {@code addSelector} is set to true.
	 * @param parent the shell parent
	 * @param title the title of the pop up (used only if {@code createPopUp} is true)
	 * @param helpMessage the help message to be displayed in the {@link HelpViewer}
	 * @param editable if the table can be edited or not
	 * @param mode see {@link TableDialog}
	 * @param createPopUp true to create a new shell, false to use the parent shell
	 * @param addSaveBtn true to create a button below the table
	 */
	public TableDialog(Shell parent, String title, String helpMessage, boolean editable, 
			RowCreationMode mode, boolean createPopUp, boolean addSaveBtn) {

		this.parent = parent;
		this.title = title;
		this.helpMessage = helpMessage;
		this.editable = editable;
		this.mode = mode;
		this.createPopUp = createPopUp;
		this.addSaveBtn = addSaveBtn;
		
		// list of parent tables
		this.parentTables = new ArrayList<>();
		
		try {
			this.schema = TableSchema.load(getSchemaSheetName());
			create();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * A table with save button.
	 * @param parent
	 * @param title
	 * @param helpMessage
	 */
	public TableDialog(Shell parent, String title, String helpMessage, boolean editable, 
			RowCreationMode mode, boolean createPopUp) {
		this(parent, title, helpMessage, editable, mode, createPopUp, true);
	}
	
	/**
	 * A table in a new pop up. Save button is also added.
	 * @param parent
	 * @param title
	 * @param helpMessage
	 */
	public TableDialog(Shell parent, String title, String helpMessage, boolean editable, 
			RowCreationMode mode) {
		this(parent, title, helpMessage, editable, mode, true, true);
	}
	
	/**
	 * A table in a new pop up with no row creation. Save button is also added.
	 * @param parent
	 * @param title
	 * @param helpMessage
	 */
	public TableDialog(Shell parent, String title, String helpMessage, boolean editable) {
		this(parent, title, helpMessage, editable, RowCreationMode.NONE, true, true);
	}
	
	/**
	 * Editable table in a new pop up with no row creation. Save button is also added.
	 * @param parent
	 * @param title
	 * @param helpMessage
	 */
	public TableDialog(Shell parent, String title, String helpMessage) {
		this(parent, title, helpMessage, true, RowCreationMode.NONE, true, true);
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
	public void setParentFilter(TableRow parentFilter) {

		// remove the old parent filter
		parentTables.remove(this.parentFilter);
		
		if (parentFilter == null)
			return;
		
		// add the new filter as parent table
		parentTables.add(parentFilter);
		
		// save the new filter
		this.parentFilter = parentFilter;
		
		this.panel.clearTable();
		this.panel.addAll(getRows());
	}
	
	/**
	 * Add a parent table to the current table
	 * @param parentTable
	 */
	public void addParentTable(TableRow parentTable) {
		
		if (parentTable == null)
			return;
		
		this.parentTables.add(parentTable);
	}
	
	/**
	 * Remove a parent table from the current table
	 * @param parentTable
	 */
	public void removeParentTable(TableRow parentTable) {
		
		if (parentTable == null)
			return;
		
		this.parentTables.remove(parentTable);
	}
	
	/**
	 * Clear all the table rows
	 * and the parent table object
	 */
	public void clear() {
		
		panel.clearTable();
		this.parentFilter = null;
		
		// disable the panel
		if (mode == RowCreationMode.SELECTOR)
			this.panel.setEnabled(false);
	}
	
	/**
	 * Load the rows which are defined in the {@link #loadInitialRows(TableSchema, TableRow)}
	 * method
	 */
	public void loadRows() {
		
		panel.clearTable();
		
		// load the rows
		Collection<TableRow> rows = loadInitialRows(panel.getSchema(), parentFilter);

		// skip if null parameter
		if (rows == null)
			return;
		
		// add them to the table
		for (TableRow row : rows) {
			panel.add(row);
		}
	}
	
	/**
	 * Get the parent table if it was set
	 * @return
	 */
	public TableRow getParentFilter() {
		return parentFilter;
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
		if (createPopUp) {
			
			this.dialog = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
			this.dialog.setText(this.title);
			
			// inherit also the icon
			Image image = parent.getImage();
			if (image != null)
				this.dialog.setImage(image);
		}
		else
			this.dialog = parent;
		
		this.dialog.setLayout(new GridLayout(1,false));
		this.dialog.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		this.panel = new TableViewWithHelp(dialog, getSchemaSheetName(), helpMessage, editable, mode);
		this.panel.setMenu(createMenu());
		
		// help listener
		this.panel.addHelpListener(new MouseListener() {
			
			@Override
			public void mouseUp(MouseEvent arg0) {
				showHelp();
			}
			
			@Override
			public void mouseDown(MouseEvent arg0) {}
			
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {}
		});


		// add a selection listener to the selector
		this.addSelectionListener(new CatalogChangedListener() {

			@Override
			public void catalogConfirmed(Selection selectedItem) {
				addNewRow(selectedItem);
			}

			@Override
			public void catalogChanged(Selection selectedItem) {}
		});


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
		
		// DEBUG for duplicates in the table
		/*Button duplicates = new Button(dialog, SWT.PUSH);
		duplicates.setText("Check duplicates");
		duplicates.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				for (Duplicate<Checkable> t : DuplicatesDetector.detect(panel.getTableElements())) {
					System.out.println("DUPLICATE " + ((TableRow)t.getFirst()).getId() + " => " + ((TableRow)t.getSecond()).getId());
				}
				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});*/
		
		Button export = new Button(dialog, SWT.PUSH);
		export.setText("Export report");
		export.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				DatasetXmlCreator creator = new DatasetXmlCreator();
				try {
					creator.export(getParentFilter());
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		dialog.pack();
		
		// make dialog longer
		dialog.setSize(dialog.getSize().x, dialog.getSize().y + 50);
	}
	
	/**
	 * Show the html help if possible
	 */
	private void showHelp() {
		
		Help help = Help.getHelp(getSchemaSheetName());
		
		// if no help found return
		if (help == null)
			return;
		
		// open the help viewer
		help.open();
	}
	
	
	private void addNewRow(Selection selectedItem) {

		// create a new row and
		// put the first cell in the row
		TableRow row = createNewRow(getSchema(), selectedItem);
		
		// inject all the parent tables ids into the row
		for (TableRow parent : parentTables) {
			Relation.injectParent(parent, row);
		}
		
		// initialize the row fields with default values
		row.initialize();

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
	}
	
	/**
	 * Set the selector label text
	 * @param text
	 */
	public void setRowCreatorLabel(String text) {
		
		if (mode == RowCreationMode.NONE)
			return;
		
		this.panel.setSelectorLabelText(text);
	}
	
	/**
	 * Set an xml list for the combo box. All the values in the
	 * list will be picked up. If a filter needs to be set, 
	 * please see {@link #setSelectorList(String, String)}.
	 * @param selectionListCode
	 */
	public void setSelectorList(String selectionListCode) {
		
		if (mode != RowCreationMode.SELECTOR)
			return;
		
		this.panel.setSelectorList(selectionListCode);
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
		
		if (mode != RowCreationMode.SELECTOR)
			return;
		
		this.panel.setSelectorList(selectionListCode, selectionId);
	}
	
	
	/**
	 * Add listener to the selector if it was added (i.e. {@link #addSelector} true)
	 * @param listener
	 */
	public void addSelectionListener(CatalogChangedListener listener) {
		if (mode != RowCreationMode.NONE) {
			this.panel.addSelectionListener(listener);
		}
	}
	
	public void addTableSelectionListener(ISelectionChangedListener listener) {
		this.panel.addSelectionChangedListener(listener);
	}
	
	public void addTableDoubleClickListener(IDoubleClickListener listener) {
		this.panel.addDoubleClickListener(listener);
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
	public void setRowCreationEnabled(boolean enabled) {
		if (mode != RowCreationMode.NONE)
			this.panel.setEnabled(enabled);
	}
	
	public Shell getDialog() {
		return dialog;
	}
	
	public void setSize(int width, int height) {
		this.dialog.setSize(width, height);
	}
	public void setDialogHeight(int height) {
		setSize(dialog.getSize().x, height);
	}
	
	/**
	 * Add height to the dialog 
	 * @param height
	 */
	public void addDialogHeight(int height) {
		setSize(dialog.getSize().x, dialog.getSize().y + height);
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
	 * Get the record of the current table. If no {@link #parentFilter}
	 * was set, then all the rows are returned, otherwise
	 * the filtered rows are returned.
	 * @return
	 */
	public Collection<TableRow> getRows() {
		
		Collection<TableRow> rows = new ArrayList<>();

		// load parents rows
		TableDao dao = new TableDao(schema);

		// if no filter get all
		if (parentFilter == null)
			return dao.getAll();

		// otherwise filter by id
		rows = dao.getByParentId(parentFilter.getSchema().getSheetName(), parentFilter.getId());
		
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
	 * Get the rows of the table when it is created. If there is no
	 * particular need, just return the {@link #getRows()} method to
	 * load all the table rows (possibly filtered by the {@link #parentFilter}).
	 * @param schema the table schema
	 * @param parentFilter the parent related to this table passed with the
	 * method {@link #setParentFilter(TableRow)}. Can be null if it was not set.
	 * @return
	 */
	public abstract Collection<TableRow> loadInitialRows(TableSchema schema, TableRow parentFilter);
	
	/**
	 * Create a new row with default values and foreign keys when the selector
	 * is used. Note that {@link #addSelector} should be set to true, otherwise
	 * no selector will be available and this method will never be called
	 * @param element the element which is currently selected in the selector
	 * @return
	 * @throws IOException 
	 */
	public abstract TableRow createNewRow(TableSchema schema, Selection type);
	
	/**
	 * Apply the changes that were made when the {@link #saveButton} is
	 * pressed. Note that the {@link #addSaveBtn} should be set to true
	 * to show the button, otherwise this method will never be called.
	 * @param schema the table schema
	 * @param rows the table rows
	 * @param selectedRow the current selected row in the table (null if no selection)
	 * @return true if the current dialog should be closed or false otherwise
	 */
	public abstract boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow);
}


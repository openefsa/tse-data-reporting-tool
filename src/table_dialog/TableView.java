package table_dialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;

import table_skeleton.TableColumn;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import xlsx_reader.TableSchema;

/**
 * This class contains a table which shows all the information
 * related to a summarized information table. In particular,
 * it allows also to change its contents dynamically.
 * @author avonva
 *
 */
public class TableView {

	private Composite parent;
	private TableViewer tableViewer;             // main table
	private TableSchema schema;                  // defines the table columns
	private ArrayList<TableRow> tableElements;   // cache of the table elements to do sorting by column
	private boolean editable;
	private Listener inputChangedListener;
	
	/**
	 * Create a report table using a predefined schema for the columns
	 * @param parent
	 * @param schema schema which specifies the columns 
	 */
	public TableView(Composite parent, String schemaSheetName, boolean editable) {
		
		this.parent = parent;
		this.editable = editable;
		try {
			this.schema = TableSchema.load(schemaSheetName);
			this.tableElements = new ArrayList<>();
			this.create();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public TableViewer getViewer() {
		return tableViewer;
	}
	
	public Table getTable() {
		return tableViewer.getTable();
	}
	
	public boolean isEditable() {
		return editable;
	}
	
	public void setInputChangedListener(Listener inputChangedListener) {
		this.inputChangedListener = inputChangedListener;
	}
	
	
	public TableSchema getSchema() {
		return schema;
	}
	
	public ArrayList<TableRow> getTableElements() {
		return tableElements;
	}
	
	/**
	 * Check if the whole table is correct
	 * @return
	 */
	public boolean areMandatoryFilled() {
		for (TableRow row : tableElements) {
			if (!row.areMandatoryFilled())
				return false;
		}
		return true;
	}

	/**
	 * Create the interface into the composite 
	 */
	private void create() {
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		this.tableViewer = new TableViewer(composite, SWT.BORDER | SWT.SINGLE
				| SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.NONE);
		
		this.tableViewer.getTable().setHeaderVisible(true);
		this.tableViewer.setContentProvider(new ContentProvider());
		this.tableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// create the columns based on the schema
		createColumns();
	}
	
	/**
	 * Set menu to the table
	 * @param menu
	 */
	public void setMenu(Menu menu) {
		this.tableViewer.getTable().setMenu(menu);
	}
	
	/**
	 * Add an element to the table viewer
	 * @param row
	 */
	public void add(TableRow row) {
		this.tableViewer.add(row);
		this.tableElements.add(row);
	}
	
	/**
	 * Add an element to the table viewer
	 * @param row
	 */
	public void addAll(Collection<TableRow> rows) {
		for (TableRow r : rows) {
			this.tableViewer.add(r);
		}
		this.tableElements.addAll(rows);
	}
	
	
	/**
	 * Clear all the elements of the table
	 */
	public void clear() {
		this.tableViewer.setInput(null);
		this.tableElements.clear();
	}
	
	/**
	 * Remove an element from the table viewer
	 * @param row
	 */
	public void delete(TableRow row) {
		this.tableViewer.remove(row);
		this.tableElements.remove(row);
		row.delete();
	}
	
	/**
	 * Remove the selected row
	 */
	public void removeSelectedRow() {
		TableRow row = getSelection();
		
		if(row == null)
			return;
		
		delete(row);
	}
	
	/**
	 * Set the input of the table
	 * @param elements
	 */
	public void setInput(Collection<TableRow> elements) {
		this.tableViewer.setInput(elements);
		this.tableElements = new ArrayList<>(elements);
		this.tableViewer.refresh();
	}
	
	/**
	 * Add a listener which is called when the table changes the highlighted element
	 * @param listener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		this.tableViewer.addSelectionChangedListener(listener);
	}
	
	/**
	 * Add a listener which is called when the table changes the highlighted element
	 * @param listener
	 */
	public void addDoubleClickListener(IDoubleClickListener listener) {
		this.tableViewer.addDoubleClickListener(listener);
	}
	
	/**
	 * Get the selected element if present
	 * @return
	 */
	public TableRow getSelection() {
		
		IStructuredSelection selection = (IStructuredSelection) this.tableViewer.getSelection();
		
		if (selection.isEmpty())
			return null;
		
		return (TableRow) selection.getFirstElement();
	}
	
	/**
	 * Get the number of elements contained in the table
	 * @return
	 */
	public int getItemCount() {
		return this.tableViewer.getTable().getItemCount();
	}
	
	/**
	 * Check if the table is empty or not
	 */
	public boolean isEmpty() {
		return getItemCount() == 0;
	}
	
	public void refresh(TableRow row) {
		
		this.tableViewer.refresh(row);
		
		// call listener
		if (inputChangedListener != null) {
			Event event = new Event();
			event.data = row;
			inputChangedListener.handleEvent(event);
		}
	}

	/**
	 * Create all the columns which are related to the schema
	 * Only visible columns are added
	 */
	private void createColumns() {
		
		// Add the validator column if editable table
		if (editable) {
			TableViewerColumn validator = new TableViewerColumn(this.tableViewer, SWT.NONE);
			validator.setLabelProvider(new ValidatorLabelProvider());
			validator.getColumn().setWidth(140);
			validator.getColumn().setText("Data check");
		}

		for (TableColumn col : schema) {

			// skip non visible columns
			if (!col.isVisible())
				continue;
			
			// Add the column to the parent table
			TableViewerColumn columnViewer = new TableViewerColumn(this.tableViewer, SWT.NONE);

			// set the label provider for column
			columnViewer.setLabelProvider(new LabelProvider(col.getId()));
			
			int size = 80;
			switch (col.getType()) {
			case INTEGER:
				size = 80;
				break;
			default:
				size = 80 + col.getLabel().length() * 4;
				break;
			}
			columnViewer.getColumn().setWidth(size);
			
			if (col.getLabel() != null)
				columnViewer.getColumn().setText(col.getLabel());
			
			if(col.getTip() != null)
				columnViewer.getColumn().setToolTipText(col.getTip());
			
			// add editor if editable flag is true
			if (editable)
				columnViewer.setEditingSupport(new TableEditor(this, col));

			addColumnSorter(col, columnViewer);
		}
	}

	/**
	 * Add a click to the column header that sorts
	 * the table by the clicked field
	 * @param columnViewer
	 */
	private void addColumnSorter(TableColumn column, TableViewerColumn columnViewer) {
		
		// set default sort direction (false will do an ascending sorting)
		columnViewer.getColumn().setData(false);

		// when a column is pressed order by the selected variable
		columnViewer.getColumn().addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				// get old direction
				boolean oldSortDirection = (boolean) columnViewer.getColumn().getData();

				// save the new direction
				columnViewer.getColumn().setData(!oldSortDirection);

				// invert the direction in the table
				orderRowsBy(column, !oldSortDirection);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
	}

	/**
	 * Sort the table elements by a variable identified by the columnKey
	 * @param columnKey
	 * @param ascendant if true ascendant order, otherwise descendant
	 */
	private void orderRowsBy(TableColumn column, boolean ascendant) {
		
		// sort elements
		Collections.sort(tableElements, new Comparator<TableRow>() {
			public int compare(TableRow row1, TableRow row2) {
				
				String value1 = null;
				String value2 = null;
				
				TableColumnValue sel1 = row1.get(column.getId());
				TableColumnValue sel2 = row2.get(column.getId());
				
				// get values
				if (sel1 != null)
					value1 = sel1.getLabel();
				
				if (sel2 != null)
					value2 = sel2.getLabel();
				
				int compare = 0;
				
				switch(column.getType()) {
				case INTEGER:
					
					int intValue1;
					int intValue2;
					
					// set default values if no value is retrieved
					if (value1 == null)
						intValue1 = ascendant ? Integer.MAX_VALUE : Integer.MIN_VALUE;
					else
						intValue1 = Integer.valueOf(value1);
					
					if (value2 == null)
						intValue2 = ascendant ? Integer.MAX_VALUE : Integer.MIN_VALUE;
					else
						intValue2 = Integer.valueOf(value2);

					// check if equal
					if ( intValue1 == intValue2)
						compare = 0;
					else {
						
						// if not equal check greater/less than
						boolean result = ascendant ? intValue1 > intValue2 : intValue2 > intValue1;
						
						// convert boolean to integer
						compare = result ? 1 : -1;
					}

					break;
				default:
					
					// check null values
					if (value1 == null)
						value1 = "";
					
					if (value2 == null)
						value2 = "";
					
					compare = ascendant ? value1.compareTo(value2) : value2.compareTo(value1);
					break;
				}
				
				return compare;
			};
		});
		
		// reset input with ordered elements
		this.tableViewer.setInput(tableElements);
	}
	
	private class ContentProvider implements IStructuredContentProvider {

		@Override
		public void dispose() {}

		@Override
		public void inputChanged(Viewer arg0, Object oldInput, Object newInput) {}

		@SuppressWarnings("unchecked")
		@Override
		public Object[] getElements(Object arg0) {
			return ((Collection<TableRow>) arg0).toArray();
		}
	}
	
	private class LabelProvider extends ColumnLabelProvider {

		private String key;
		public LabelProvider(String key) {
			this.key = key;
		}
		
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

			TableRow row = (TableRow) arg0;
			TableColumnValue cell = row.get(key);

			if (cell == null || cell.getLabel() == null)
				return null;
			
			TableColumn col = row.getSchema().getById(key);
			
			if (col.isPassword()) {
				// show as password with dots
				String ECHARSTR = Character.toString((char)9679);
				return cell.getLabel().replaceAll(".", ECHARSTR);
			}
			else
				return cell.getLabel();
		}
	}
	
	private class ValidatorLabelProvider extends ColumnLabelProvider {
		
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
		public String getText(Object element) {

			TableRow row = (TableRow) element;
			
			String text = "";

			switch (row.getStatus()) {
			case OK:
				text = "Validated";
				break;
			case POSITIVE_MISSING:
				text = "Positive cases report incomplete";
				break;
			case INCONCLUSIVE_MISSING:
				text = "";
				break;
			case MANDATORY_MISSING:
				text = "Missing mandatory fields";
				break;
			case ERROR:
				text = "Check case report";
				break;
			}

			return text;
		}
		
		@Override
		public Color getForeground(Object element) {
			TableRow row = (TableRow) element;
			return getRowColor(row);
		}
		
		/**
		 * Row color based on row status
		 * @param row
		 * @return
		 */
		private Color getRowColor(TableRow row) {
			
			Display display = parent.getDisplay();
			
			Color red = display.getSystemColor(SWT.COLOR_RED);
		    Color green = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		    Color yellow = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
		    
		    Color rowColor = green;
		    
		    switch (row.getStatus()) {
		    case OK:
		    	rowColor = green;
		    	break;
		    	
		    case POSITIVE_MISSING:
		    case INCONCLUSIVE_MISSING:
		    	rowColor = yellow;
		    	break;
		    	
		    case ERROR:
		    case MANDATORY_MISSING:
		    	rowColor = red;
		    	break;
		    }
		    
		    return rowColor;
		}
	}
}

package table_dialog;

import java.io.IOException;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;

import database.TableDao;
import table_dialog.MonitoringSelector.MonitoringListener;
import table_skeleton.TableRow;
import xlsx_reader.TableSchema;
import xml_config_reader.Selection;

/**
 * Generic class that provides an interface to create {@link TableRow} objects
 * through their schema.
 * @author avonva
 *
 */
public abstract class ReportViewer {
	
	private Composite parent;
	private String title;
	private String sheetName;
	
	private MonitoringSelector typeSelector;
	private ReportViewerHelp helpViewer;
	private ReportTable reportTable;
	
	private TableRow parentTable;
	
	/**
	 * Create the selector and report table
	 * @param parent
	 */
	public ReportViewer(Composite parent, String title, String sheetName) {
		this.parent = parent;
		this.title = title;
		this.sheetName = sheetName;
		create();
	}
	
	/**
	 * Enable/disable the creation of new records
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		this.typeSelector.setEnabled(enabled);
	}
	
	/**
	 * Create the panel
	 */
	private void create() {
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1,false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		this.helpViewer = new ReportViewerHelp(composite, title);
		this.helpViewer.setListener(new MouseListener() {
			
			@Override
			public void mouseUp(MouseEvent arg0) {
				System.out.println("Open help");
			}
			
			@Override
			public void mouseDown(MouseEvent arg0) {}
			
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {}
		});
		
		this.typeSelector = new MonitoringSelector(composite);
		this.typeSelector.setEnabled(false);

		this.reportTable = new ReportTable(composite, sheetName, true);

		// add the menu to the table
		addTableMenu();

		this.typeSelector.addSelectionListener(new MonitoringListener() {

			@Override
			public void selectionConfirmed(Selection selectedItem) {

				// create a new row and
				// put the first cell in the row
				try {
					
					TableRow row = createNewRow(reportTable.getSchema(), selectedItem);
					
					// if a parent was set, then add also the foreign key to the
					// current new row
					if (parentTable != null) {
						String reportForeignKey = reportTable.getSchema()
								.getRelationByParentTable(parentTable.getSchema().getSheetName()).getForeignKey();
						row.put(reportForeignKey, parentTable.get(reportForeignKey));
					}
					
					// insert the row and get the row id
					TableDao dao = new TableDao(reportTable.getSchema());
					int id = dao.add(row);
					
					// set the id for the new row
					row.setId(id);
					
					// update the formulas
					row.updateFormulas();
					
					// update the row with the formulas solved
					dao.update(row);
					
					// add the row to the table
					reportTable.add(row);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void selectionChanged(Selection selectedItem) {}
		});
	}
	
	/**
	 * If the current table is in many to one relation
	 * with a parent table, then use this method to
	 * load all the records related to the chosen parent
	 * in the table.
	 * @param parentTable
	 */
	public void loadParentRecords(TableRow parentTable) {
		
		this.parentTable = parentTable;
		
		this.setEnabled(true);
		
		// load parents rows
		TableDao dao = new TableDao(this.reportTable.getSchema());
		this.reportTable.setInput(dao.getByParentId(parentTable.getSchema().getSheetName(), parentTable.getId()));
	}
	
	/**
	 * Add a menu to the table
	 */
	private void addTableMenu() {
		
		Menu menu = new Menu(parent);
		
		// remove an item
		final MenuItem remove = new MenuItem(menu, SWT.PUSH);
		remove.setText("Delete record");
		remove.setEnabled(false);
		
		reportTable.setMenu(menu);
		
		reportTable.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				remove.setEnabled(!reportTable.isEmpty());
			}
		});
		
		remove.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				MessageBox mb = new MessageBox(parent.getShell(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
				mb.setText("Warning!");
				mb.setMessage("The selected record and all the related data will be permanently deleted. Continue?");
				
				int val = mb.open();
				
				if (val == SWT.YES)
					reportTable.removeSelectedRow();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		// add all the other menu items
		addMenuItems(menu);
	}
	
	
	/**
	 * Create a new row with default values and foreign keys
	 * @param element a type if it was selected with the combo box filter
	 * @return
	 * @throws IOException 
	 */
	public abstract TableRow createNewRow(TableSchema schema, Selection type) throws IOException;
	
	/**
	 * Add menu items to the menu of the table if necessary
	 * @param menu
	 */
	public abstract void addMenuItems(Menu menu);
}

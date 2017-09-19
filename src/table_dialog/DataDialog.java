package table_dialog;

import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
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

import table_skeleton.TableRow;
import xlsx_reader.TableSchema;

/**
 * Generic dialog for showing a dialog 
 * with an help panel and a table that uses an excel schema.
 * It is also possible to load the first row of the table during
 * its creation; this should be used just for settings/preferences
 * in which we have just a row
 * @author avonva
 *
 */
public abstract class DataDialog {

	private Shell parent;
	private Shell dialog;
	private ReportTable table;
	private ReportViewerHelp helpViewer;
	private Button saveButton;
	
	private String title;
	private String message;
	private boolean editable;
	
	public DataDialog(Shell parent, String title, String message, boolean editable) {
		this.parent = parent;
		this.title = title;
		this.message = message;
		this.editable = editable;
		
		create();
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
		this.saveButton.setText(text);
		this.saveButton.pack();
	}
	
	/**
	 * Create the interface
	 */
	private void create() {
		this.dialog = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		this.dialog.setText(this.title);
		
		this.dialog.setLayout(new GridLayout(1,false));
		this.dialog.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		this.helpViewer = new ReportViewerHelp(dialog, message);

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
		
		this.table = new ReportTable(dialog, getSchemaSheetName(), editable);
		this.table.setMenu(createMenu());
		
		// load the rows
		Collection<TableRow> rows = loadContents(table.getSchema());

		// add them to the table
		for (TableRow row : rows) {
			this.table.add(row);
		}

		// save button
		this.saveButton = new Button(dialog, SWT.PUSH);
		this.saveButton.setText("Save");
		this.saveButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		this.saveButton.setEnabled(table.areMandatoryFilled());
		
		// save options
		this.saveButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				boolean ok = apply(table.getSchema(), table.getTableElements(), table.getSelection());
				
				if (ok)
					dialog.close();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		// disable button if errors
		this.table.setInputChangedListener(new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				saveButton.setEnabled(table.areMandatoryFilled());
			}
		});
		
		dialog.pack();
		
		// make dialog longer
		dialog.setSize(dialog.getSize().x, dialog.getSize().y + 50);
	}
	
	public Shell getDialog() {
		return dialog;
	}
	
	/**
	 * get the sheet which includes the schema for the table
	 * @return
	 */
	public abstract String getSchemaSheetName();
	
	/**
	 * Create a menu for the table
	 * @return
	 */
	public abstract Menu createMenu();
	
	/**
	 * Load rows into the table
	 * @return
	 */
	public abstract Collection<TableRow> loadContents(TableSchema schema);
	
	/**
	 * Apply the changes that were made
	 * @param row
	 */
	public abstract boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow);
}


package tse_components;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import app_config.DebugConfig;
import global_utils.Warnings;
import table_dialog.HelpViewer;
import table_dialog.RowCreatorViewer;
import table_dialog.TableDialog;
import table_dialog.TableView;
import table_skeleton.TableColumn;
import table_skeleton.TableRow;

/**
 * Generic class that provides an interface to create {@link TableRow} objects
 * through their schema.
 * @author avonva
 *
 */
public abstract class TableDialogWithMenu extends TableDialog {
	
	/**
	 * Create a dialog with a {@link HelpViewer}, a {@link TableView}
	 * and possibly a {@link RowCreatorViewer} if {@code addSelector} is set to true.
	 * It also allows adding and removing rows from the table
	 * @param parent the shell parent
	 * @param title the title of the pop up (used only if {@code createPopUp} is true)
	 * @param message the help message
	 * @param editable if the table can be edited or not
	 * @param addSelector if the {@link RowCreatorViewer} should be added or not
	 */
	public TableDialogWithMenu(Shell parent, String title, boolean createPopUp, boolean addSaveBtn) {
		super(parent, title, createPopUp, addSaveBtn);
	}
	
	@Override
	public Menu createMenu() {

		Menu menu = new Menu(getDialog());
		
		// remove an item
		MenuItem remove = new MenuItem(menu, SWT.PUSH);
		remove.setText("Delete records");
		remove.setEnabled(false);
		
		if (DebugConfig.debug) {
			MenuItem button = new MenuItem(menu, SWT.PUSH);
			button.setText("Print row");
			button.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					
					TableRow row = getSelection();
					
					if (row == null)
						return;
					
					System.out.println("ROW==================");
					System.out.println(row);
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {}
			});
			
			MenuItem button2 = new MenuItem(menu, SWT.PUSH);
			button2.setText("Check mandatory fields");
			button2.addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent arg0) {
					
					TableRow row = getSelection();
					
					if (row == null)
						return;
					
					System.out.println("ROW PROPERTIES==================");
					System.out.println("are mandatory filled = " + row.areMandatoryFilled());
					
					for (TableColumn col : getSchema()) {
						System.out.println(col.getId() + "; mandatory= " + col.isMandatory(row) 
							+ " with formula " + col.getMandatoryFormula());
					}
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent arg0) {}
			});
		}
		
		addTableSelectionListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				remove.setEnabled(!isTableEmpty() && isEditable());
			}
		});
		
		remove.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				int val = Warnings.warnUser(getDialog(), 
						"Warning",
						"CONF906: The selected records and all the related data will be permanently deleted. Continue?", 
						SWT.ICON_WARNING | SWT.YES | SWT.NO);
				
				if (val == SWT.YES) {
					getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
					removeSelectedRow();
					getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		return menu;
	}
}

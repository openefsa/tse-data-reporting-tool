package tse_components;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import table_dialog.RowCreatorViewer;
import table_dialog.HelpViewer;
import table_dialog.TableDialog;
import table_dialog.TableView;
import table_dialog.TableViewWithHelp.RowCreationMode;
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
	public TableDialogWithMenu(Shell parent, String title, String message, 
			boolean editable, RowCreationMode mode, boolean createPopUp, boolean addSaveBtn) {
		super(parent, title, message, editable, mode, createPopUp, addSaveBtn);
	}
	
	@Override
	public Menu createMenu() {

		Menu menu = new Menu(getDialog());
		
		// remove an item
		MenuItem remove = new MenuItem(menu, SWT.PUSH);
		remove.setText("Delete record");
		remove.setEnabled(false);

		
		addTableSelectionListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				remove.setEnabled(!isTableEmpty());
			}
		});
		
		remove.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				MessageBox mb = new MessageBox(getDialog(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
				mb.setText("Warning!");
				mb.setMessage("The selected record and all the related data will be permanently deleted. Continue?");
				
				int val = mb.open();
				
				if (val == SWT.YES)
					removeSelectedRow();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		return menu;
	}
}

package tse_options;

import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import session_manager.TSERestoreableWindowDao;
import table_database.TableDao;
import table_dialog.TableDialog;
import table_relations.Relation;
import table_skeleton.TableRow;
import window_restorer.RestoreableWindow;
import xlsx_reader.TableSchema;

public abstract class OptionsDialog extends TableDialog {
	
	private RestoreableWindow window;
	private OptionsChangedListener listener;
	private int status; 
	
	public OptionsDialog(Shell parent, String title, String windowCode) {
		super(parent, title, true, true, false);
		
		// create the parent structure
		Shell dialog = super.create();

		this.window = new RestoreableWindow(dialog, windowCode);
		window.restore(TSERestoreableWindowDao.class);
		window.saveOnClosure(TSERestoreableWindowDao.class);
		
		this.status = SWT.CANCEL;
	}
	
	/**
	 * Set a listener which is called when the options are set
	 * and saved
	 * @param listener
	 */
	public void setListener(OptionsChangedListener listener) {
		this.listener = listener;
	}
	
	/**
	 * Get the current status of the dialog, if it was closed
	 * with a saving operation or with a cancel
	 * @return
	 */
	public int getStatus() {
		return status;
	}

	@Override
	public Collection<TableRow> loadInitialRows(TableSchema schema, TableRow parentTable) {
		
		TableDao dao = new TableDao();
		Collection<TableRow> objs = dao.getAll(schema);
		
		// if no option was set, add an empty row
		// to the db
		if (objs.isEmpty()) {
			
			// add a new row
			TableRow row = new TableRow(schema);
			row.initialize();
			int id = dao.add(row);
			row.setId(id);
			
			objs.add(row);
		}
		
		return objs;
	}

	@Override
	public boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {
		
		if (rows.isEmpty())
			return true;
		
		TableRow row = rows.iterator().next();
		
		// save the values in the db
		this.saveRows(row);
		
		// update preferences
		/*TableDao dao = new TableDao(schema);
		dao.update(row);*/
		
		// update the cache of the relations
		Relation.updateCache(row);
		
		if (listener != null)
			listener.optionChanged(row);
		
		status = SWT.OK;
		
		return true;
	}

}

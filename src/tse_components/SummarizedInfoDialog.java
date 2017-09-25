package tse_components;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import table_dialog.TableViewWithHelp.RowCreationMode;
import table_relations.Relation;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import tse_config.CustomPaths;
import tse_config.CatalogLists;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Class which allows adding and editing a summarized information report.
 * @author avonva
 *
 */
public class SummarizedInfoDialog extends TableDialogWithMenu {
	
	public SummarizedInfoDialog(Shell parent) {
		
		super(parent, "", "TSEs monitoring data (aggregated level)", 
				true, RowCreationMode.SELECTOR, false, false);
		
		// add 300 px in height
		addDialogHeight(300);
		
		// specify title and list of the selector
		setRowCreatorLabel("Add data related to monitoring of:");
		setSelectorList(CatalogLists.TSE_LIST);
		
		// add the parents of preferences and settings
		try {
			addParentTable(Relation.getGlobalParent(CustomPaths.PREFERENCES_SHEET));
			addParentTable(Relation.getGlobalParent(CustomPaths.SETTINGS_SHEET));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// if double clicked an element of the table
		// open the cases
		addTableDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {

				final IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				if (selection == null || selection.isEmpty())
					return;

				final TableRow summInfo = (TableRow) selection.getFirstElement();

				// create a case passing also the report information
				CaseReportDialog dialog = new CaseReportDialog(parent, getParentFilter());
				
				// filter the records by the clicked summarized information
				dialog.setParentFilter(summInfo);
				
				// add as parent also the report of the summarized information
				// which is the parent filter since we have chosen a summarized
				// information from a single report (the summ info were filtered
				// by the report)
				dialog.addParentTable(getParentFilter());
				
				dialog.open();
			}
		});
	}
	
	@Override
	public void setParentFilter(TableRow parentFilter) {
		// enable/disable the selector when a report is opened/closed
		setRowCreationEnabled(parentFilter != null);
		super.setParentFilter(parentFilter);
	}

	/**
	 * Create a new row with default values
	 * @param element
	 * @return
	 * @throws IOException 
	 */
	@Override
	public TableRow createNewRow(TableSchema schema, Selection element) {

		TableColumnValue value = new TableColumnValue(element);
		
		// create a new row with the type column already set
		TableRow row = new TableRow(schema, SummarizedInformationSchema.TYPE, value);

		return row;
	}

	@Override
	public String getSchemaSheetName() {
		return CustomPaths.SUMMARIZED_INFO_SHEET;
	}

	@Override
	public boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {
		return true;
	}

	@Override
	public Collection<TableRow> loadInitialRows(TableSchema schema, TableRow parentFilter) {
		return null;
	}

	@Override
	public void processNewRow(TableRow row) {}
}

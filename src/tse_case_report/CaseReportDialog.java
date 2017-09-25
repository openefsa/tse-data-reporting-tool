package tse_case_report;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import table_database.TableDao;
import table_dialog.RowValidatorLabelProvider;
import table_dialog.TableViewWithHelp.RowCreationMode;
import table_relations.Relation;
import table_skeleton.TableRow;
import tse_analytical_result.ResultDialog;
import tse_components.TableDialogWithMenu;
import tse_config.CustomPaths;
import tse_validator.SimpleRowValidatorLabelProvider;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;
import xml_catalog_reader.Selection;

/**
 * Class which allows adding and editing a summarized information report.
 * @author avonva
 *
 */
public class CaseReportDialog extends TableDialogWithMenu {
	
	private TableRow report;
	private TableRow summInfo;
	
	public CaseReportDialog(Shell parent, TableRow report) {
		
		super(parent, "Case report", "TSEs monitoring data (case level)", 
				true, RowCreationMode.STANDARD, true, false);
		
		this.report = report;
		
		// add 300 px in height
		addDialogHeight(300);
		
		// specify title and list of the selector
		setRowCreatorLabel("Add data:");
		
		// set the report also as parent of the case
		addParentTable(report);
		
		// when element is double clicked
		addTableDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				
				final IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				if (selection == null || selection.isEmpty())
					return;

				final TableRow caseReport = (TableRow) selection.getFirstElement();
				
				// initialize result passing also the 
				// report data and the summarized information data
				ResultDialog dialog = new ResultDialog(parent, report, summInfo);
				dialog.setParentFilter(caseReport); // set the case as filter (and parent)
				dialog.open();
			}
		});
	}
	
	@Override
	public void setParentFilter(TableRow parentFilter) {
		setRowCreationEnabled(parentFilter != null);
		this.summInfo = parentFilter;
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

		// return the new row
		TableRow caseRow = new TableRow(schema);
		
		return caseRow;
	}

	@Override
	public String getSchemaSheetName() {
		return CustomPaths.CASE_INFO_SHEET;
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
	public void processNewRow(TableRow caseRow) {

		// create two default rows for the analytical results
		// related to this case
		// we do this after the new row was added to the database
		// in order to be able to get its id
		try {
			
			TableSchema resultSchema = TableSchemaList.getByName(CustomPaths.RESULT_SHEET);
			
			TableRow resultRow = new TableRow(resultSchema);
			resultRow.initialize();
			
			// inject the case parent to the result
			Relation.injectParent(report, resultRow);
			Relation.injectParent(summInfo, resultRow);
			Relation.injectParent(caseRow, resultRow);
			
			// add two default rows
			TableDao dao = new TableDao(resultSchema);
			dao.add(resultRow);
			dao.add(resultRow);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public RowValidatorLabelProvider getValidator() {
		return new SimpleRowValidatorLabelProvider();
	}
}

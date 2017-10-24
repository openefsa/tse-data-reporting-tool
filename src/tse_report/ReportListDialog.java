package tse_report;

import java.util.Collection;

import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import dataset.DatasetList;
import table_database.TableDao;
import table_dialog.DialogBuilder;
import table_dialog.RowValidatorLabelProvider;
import table_dialog.TableDialog;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

public class ReportListDialog extends TableDialog {
	
	private TseReport selectedReport;
	
	public ReportListDialog(Shell parent, String title) {
		super(parent, title, true, true);
		
		// create the parent structure
		super.create();
	}

	@Override
	public String getSchemaSheetName() {
		return CustomStrings.REPORT_SHEET;
	}

	@Override
	public Collection<TableRow> loadInitialRows(TableSchema schema, TableRow parentTable) {
		
		TableDao dao = new TableDao(schema);
		Collection<TableRow> reports = dao.getAll();
		
		// convert to dataset list
		DatasetList<TseReport> tseReports = new DatasetList<>();
		for (TableRow r : reports) {
			TseReport report = new TseReport(r);
			tseReports.add(report);
		}
		
		// get only last versions
		DatasetList<TseReport> lastVersions = tseReports.filterOldVersions();

		// sort the list
		lastVersions.sort();
		
		reports.clear();
		
		// convert back to table row
		for (TseReport tseReport : lastVersions) {
			reports.add(new TableRow(tseReport));
		}
		
		return reports;
	}

	@Override
	public boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {
		
		if (selectedRow == null) {
			warnUser("Error", "A report was not selected!");
			return false;
		}
		
		this.selectedReport = new TseReport(selectedRow);
		
		return true;
	}

	public TseReport getSelectedReport() {
		return selectedReport;
	}

	@Override
	public Menu createMenu() {
		return null;
	}

	@Override
	public TableRow createNewRow(TableSchema schema, Selection type) {
		return null;
	}

	@Override
	public void processNewRow(TableRow row) {}
	
	@Override
	public RowValidatorLabelProvider getValidator() {
		return null;
	}

	@Override
	public void addWidgets(DialogBuilder viewer) {
		viewer.addHelp(getDialog().getText(), false)
			.addTable(CustomStrings.REPORT_SHEET, false);
	}
}

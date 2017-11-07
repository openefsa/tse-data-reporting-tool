package tse_case_report;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import dataset.DatasetStatus;
import predefined_results_reader.PredefinedResult;
import report.Report;
import table_dialog.DialogBuilder;
import table_dialog.RowValidatorLabelProvider;
import table_relations.Relation;
import table_skeleton.TableRow;
import tse_analytical_result.ResultDialog;
import tse_components.TableDialogWithMenu;
import tse_config.CustomStrings;
import tse_summarized_information.SummarizedInfo;
import tse_validator.CaseReportValidator;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Class which allows adding and editing a summarized information report.
 * @author avonva
 *
 */
public class CaseReportDialog extends TableDialogWithMenu {
	
	private Report report;
	private SummarizedInfo summInfo;
	
	public CaseReportDialog(Shell parent, Report report, SummarizedInfo summInfo) {
		
		super(parent, "Case report", true, false);
		
		this.report = report;
		this.summInfo = summInfo;
		
		// create the parent structure
		super.create();
		
		// add 300 px in height
		addHeight(300);
		
		// update the ui
		updateUI();
	}
	
	@Override
	public Menu createMenu() {
		
		Menu menu = super.createMenu();
		
		MenuItem addResult = new MenuItem(menu, SWT.PUSH);
		addResult.setText("Open analytical results form");
		addResult.setEnabled(false);
		
		addTableSelectionListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				addResult.setEnabled(!isTableEmpty());
			}
		});
		
		addResult.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {

				TableRow row = getSelection();
				
				if (row == null)
					return;
				
				CaseReport caseReport = new CaseReport(row);
				
				if (!caseReport.areMandatoryFilled()) {
					warnUser("Error", "ERR000: Cannot add analytical results. Mandatory data are missing!");
					return;
				}
				
				try {
					
					// create default if no results are present
					if (!caseReport.hasResults()) {
						PredefinedResult.createDefaultResults(report, summInfo, caseReport);
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// initialize result passing also the 
				// report data and the summarized information data
				ResultDialog dialog = new ResultDialog(getParent(), report, summInfo, caseReport);
				dialog.setParentFilter(caseReport); // set the case as filter (and parent)
				dialog.open();
				
				caseReport.updateChildrenErrors();
				
				replace(caseReport);
			}
		});
		
		addRemoveMenuItem(menu);
			
		return menu;
	}
	
	@Override
	public void setParentFilter(TableRow parentFilter) {
		this.summInfo = new SummarizedInfo(parentFilter);
		super.setParentFilter(parentFilter);
	}

	private void updateUI() {
		DialogBuilder panel = getPanelBuilder();
		String status = report.getLabel(AppPaths.REPORT_STATUS);
		DatasetStatus datasetStatus = DatasetStatus.fromString(status);
		boolean editableReport = datasetStatus.isEditable();
		panel.setTableEditable(editableReport);
		panel.setRowCreatorEnabled(editableReport);
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
		
		// add parents
		Relation.injectParent(report, caseRow);
		Relation.injectParent(summInfo, caseRow);
		
		return caseRow;
	}

	@Override
	public String getSchemaSheetName() {
		return CustomStrings.CASE_INFO_SHEET;
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
	public void processNewRow(TableRow caseRow) {}

	@Override
	public RowValidatorLabelProvider getValidator() {
		return new CaseReportValidator();
	}

	@Override
	public void addWidgets(DialogBuilder viewer) {
		
		String reportMonth = report.getLabel(AppPaths.REPORT_MONTH);
		String reportYear = report.getLabel(AppPaths.REPORT_YEAR);
		String source = summInfo.getLabel(CustomStrings.SUMMARIZED_INFO_SOURCE);
		String prod = summInfo.getLabel(CustomStrings.SUMMARIZED_INFO_PROD);
		String age = summInfo.getLabel(CustomStrings.SUMMARIZED_INFO_AGE);
		String target = summInfo.getLabel(CustomStrings.SUMMARIZED_INFO_TARGET_GROUP);
		String status = summInfo.getLabel(CustomStrings.SUMMARIZED_INFO_STATUS);
		
		StringBuilder reportRow = new StringBuilder();
		reportRow.append("Monthly report: ")
			.append(reportMonth)
			.append(" ")
			.append(reportYear);
		
		StringBuilder animalRow = new StringBuilder();
		animalRow.append("Animal: ")
			.append(source)
			.append(" ")
			.append(age)
			.append(" ")
			.append(prod);
		
		StringBuilder targetRow = new StringBuilder();
		targetRow.append("Target group: ")
			.append(target);
		
		StringBuilder statusRow = new StringBuilder();
		statusRow.append("Status of the herd/flock: ")
			.append(status);
		
		viewer.addHelp("TSEs monitoring data (case level)")
			.addLabel("reportLabel", reportRow.toString())
			.addLabel("animalLabel", animalRow.toString())
			.addLabel("targetLabel", targetRow.toString())
			.addLabel("statusLabel", statusRow.toString())
			.addRowCreator("Add data:")
			.addTable(CustomStrings.CASE_INFO_SHEET, true);
	}
}

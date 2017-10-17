package tse_case_report;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import dataset.DatasetStatus;
import table_database.TableDao;
import table_dialog.DialogBuilder;
import table_dialog.RowValidatorLabelProvider;
import table_relations.Relation;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import tse_analytical_result.ResultDialog;
import tse_components.TableDialogWithMenu;
import tse_config.CatalogLists;
import tse_config.CustomStrings;
import tse_validator.CaseReportValidator;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;
import xml_catalog_reader.Selection;
import xml_catalog_reader.XmlContents;
import xml_catalog_reader.XmlLoader;

/**
 * Class which allows adding and editing a summarized information report.
 * @author avonva
 *
 */
public class CaseReportDialog extends TableDialogWithMenu {
	
	private TableRow report;
	private TableRow summInfo;
	
	public CaseReportDialog(Shell parent, TableRow report, TableRow summInfo) {
		
		super(parent, "Case report", true, false);
		
		this.report = report;
		this.summInfo = summInfo;
		
		// create the parent structure
		super.create();
		
		// add 300 px in height
		addHeight(300);
		
		// update the ui
		updateUI();
		
		// when element is double clicked
		addTableDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				
				final IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				if (selection == null || selection.isEmpty())
					return;

				final TableRow caseReport = (TableRow) selection.getFirstElement();
				
				if (!caseReport.areMandatoryFilled()) {
					warnUser("Error", "ERR000: Cannot add analytical results. Mandatory data are missing!");
					return;
				}
				
				try {
					
					TableDao dao = new TableDao(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));

					boolean hasResults = !dao.getByParentId(caseReport.getSchema().getSheetName(), caseReport.getId()).isEmpty();
					
					// create default if no results are present
					if (!hasResults)
						createDefaultResults(caseReport);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// initialize result passing also the 
				// report data and the summarized information data
				ResultDialog dialog = new ResultDialog(parent, report, summInfo, caseReport);
				dialog.setParentFilter(caseReport); // set the case as filter (and parent)
				dialog.open();
				
				// refresh cases when results are closed
				refresh();
			}
		});
	}
	
	public void createDefaultResults(TableRow caseReport) throws IOException {
		
		TableSchema resultSchema = TableSchemaList.getByName(CustomStrings.RESULT_SHEET);
		
		TableRow resultRow = new TableRow(resultSchema);
		
		// inject the case parent to the result
		Relation.injectParent(report, resultRow);
		Relation.injectParent(summInfo, resultRow);
		Relation.injectParent(caseReport, resultRow);

		// add two default rows
		TableDao dao = new TableDao(resultSchema);
		
		resultRow.initialize();
		
		// check if we have a confirmatory test
		boolean isConfirmatoryTest = summInfo.getCode(CustomStrings.SUMMARIZED_INFO_TEST_TYPE)
				.equals(CustomStrings.SUMMARIZED_INFO_CONFIRMATORY_TEST);
		
		boolean isScrapie = summInfo.getCode(CustomStrings.SUMMARIZED_INFO_TYPE)
				.equals(CustomStrings.SUMMARIZED_INFO_SCRAPIE_TYPE);
		
		// get all the tests lists
		XmlContents xml = XmlLoader.getByPicklistKey(CatalogLists.TEST_LIST);
		
		// get the list with all the tests
		Collection<Selection> tests = xml.getListElements(CatalogLists.ALL_TEST_ID);
		
		// for each test create a row with that test
		// do not create screening row if we have confirmatory test
		for (Selection test : tests) {
		
			// skip
			boolean testScreening = test.getCode().equals(CustomStrings.SUMMARIZED_INFO_SCREENING_TEST);
			boolean testMolecular = test.getCode().equals(CustomStrings.SUMMARIZED_INFO_MOLECULAR_TEST);
			
			if (isConfirmatoryTest && testScreening)
				continue;

			// molecular test only for scrapie
			if (!isScrapie && testMolecular)
				continue;
			
			// add get the id and update the fields
			int id = dao.add(resultRow);
			resultRow.setId(id);

			// initialize values
			resultRow.initialize();
			
			// set test accordingly
			TableColumnValue value = new TableColumnValue(test);
			resultRow.put(CustomStrings.RESULT_TEST_TYPE, value);
			
			// save in the db
			dao.update(resultRow);
		}
	}
	
	@Override
	public void setParentFilter(TableRow parentFilter) {
		this.summInfo = parentFilter;
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
		String part = summInfo.getLabel(CustomStrings.SUMMARIZED_INFO_PART);
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
		
		StringBuilder sampleRow = new StringBuilder();
		sampleRow.append("Sample: ")
			.append(part);
		
		StringBuilder targetRow = new StringBuilder();
		targetRow.append("Target group: ")
			.append(target);
		
		StringBuilder statusRow = new StringBuilder();
		statusRow.append("Status of the herd/flock: ")
			.append(status);
		
		viewer.addHelp("TSEs monitoring data (case level)")
			.addLabel("reportLabel", reportRow.toString())
			.addLabel("animalLabel", animalRow.toString())
			.addLabel("sampLabel", sampleRow.toString())
			.addLabel("targetLabel", targetRow.toString())
			.addLabel("statusLabel", statusRow.toString())
			.addRowCreator("Add data:")
			.addTable(CustomStrings.CASE_INFO_SHEET, true);
	}
}

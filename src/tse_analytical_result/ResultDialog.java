package tse_analytical_result;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.swt.widgets.Shell;

import dataset.DatasetStatus;
import table_dialog.DialogBuilder;
import table_dialog.RowValidatorLabelProvider;
import table_skeleton.TableRow;
import tse_components.TableDialogWithMenu;
import tse_config.CustomStrings;
import tse_validator.ResultValidator;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Class which allows adding and editing a summarized information report.
 * @author avonva
 *
 */
public class ResultDialog extends TableDialogWithMenu {
	
	private TableRow report;
	private TableRow summInfo;
	private TableRow caseInfo;
	
	public ResultDialog(Shell parent, TableRow report, TableRow summInfo, TableRow caseInfo) {
		
		super(parent, "Analytical results", true, false);
		
		this.report = report;
		this.summInfo = summInfo;
		this.caseInfo = caseInfo;
		
		// create the dialog
		super.create();
		
		// add 300 px in height
		addHeight(300);
		
		// add the parents
		addParentTable(report);
		addParentTable(summInfo);
		
		updateUI();
	}
	
	/**
	 * make table non editable if needed
	 */
	private void updateUI() {
		DialogBuilder panel = getPanelBuilder();
		String status = report.getLabel(CustomStrings.REPORT_STATUS);
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
		
		TableRow row = new TableRow(schema);
		return row;
	}

	@Override
	public String getSchemaSheetName() {
		return CustomStrings.RESULT_SHEET;
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
	
	@Override
	public RowValidatorLabelProvider getValidator() {
		return new ResultValidator();
	}

	@Override
	public void addWidgets(DialogBuilder viewer) {
		
		String reportMonth = report.getLabel(CustomStrings.REPORT_MONTH);
		String reportYear = report.getLabel(CustomStrings.REPORT_YEAR);
		String sampleId = caseInfo.getLabel(CustomStrings.CASE_INFO_SAMPLE_ID);
		String animalId = caseInfo.getLabel(CustomStrings.CASE_INFO_ANIMAL_ID);
		String caseId = caseInfo.getLabel(CustomStrings.CASE_INFO_CASE_ID);
		
		StringBuilder reportRow = new StringBuilder();
		reportRow.append("Monthly report: ")
			.append(reportMonth)
			.append(" ")
			.append(reportYear);
		
		StringBuilder sampleIdRow = new StringBuilder();
		sampleIdRow.append("Sample id: ")
			.append(sampleId);
		
		StringBuilder animalIdRow = new StringBuilder();
		animalIdRow.append("Animal id: ")
			.append(animalId);
		
		StringBuilder caseIdRow = new StringBuilder();
		caseIdRow.append("Case id: ")
			.append(caseId);
		
		viewer.addHelp("Analytical results")
			.addRowCreator("Add result:")
			.addLabel("reportLabel", reportRow.toString())
			.addLabel("sampLabel", sampleIdRow.toString())
			.addLabel("animalLabel", animalIdRow.toString())
			.addLabel("caseIdLabel", caseIdRow.toString())
			.addTable(CustomStrings.RESULT_SHEET, true, summInfo);  // add parent to be able to solve isVisible field
	}
}

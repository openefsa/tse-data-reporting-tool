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
import global_utils.Warnings;
import report.Report;
import table_database.TableDao;
import table_dialog.DialogBuilder;
import table_dialog.EditorListener;
import table_dialog.RowValidatorLabelProvider;
import table_relations.Relation;
import table_skeleton.TableColumn;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import tse_analytical_result.ResultDialog;
import tse_components.TableDialogWithMenu;
import tse_config.CustomStrings;
import tse_summarized_information.SummarizedInfo;
import tse_validator.CaseReportValidator;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;
import xml_catalog_reader.Selection;

/**
 * Dialog which shows cases report related to a summarized information parent
 * @author avonva
 *
 */
public class CaseReportDialog extends TableDialogWithMenu {
	
	private Report report;
	private SummarizedInfo summInfo;
	
	public CaseReportDialog(Shell parent, Report report, SummarizedInfo summInfo) {
		
		super(parent, "Cases/samples", true, false);
		
		this.report = report;
		this.summInfo = summInfo;
		
		// create the parent structure
		super.create();
		
		// add 300 px in height
		addHeight(300);
		
		// update the ui
		updateUI();
		
		// ask for default values
		askForDefault();
		
		setEditorListener(new EditorListener() {
			
			@Override
			public void editStarted() {}
			
			@Override
			public void editEnded(TableRow row, TableColumn field, boolean changed) {
				if (changed && field.equals(CustomStrings.CASE_INFO_STATUS)) {
					row.remove(CustomStrings.CASE_INDEX_CASE);
				}
			}
		});
	}
	
	public void askForDefault() {
		
		// create default cases if no cases
		// and cases were set in the aggregated data
		if (isEditable() && !summInfo.hasCases() 
				&& getNumberOfExpectedCases(summInfo) > 0) {
			
			Warnings.warnUser(getDialog(), "Warning", 
					"The tool will initialize the table of cases/samples for the selected context, based on the number of inconclusive and positive cases.", 
					SWT.ICON_INFORMATION);
			
			try {
				createDefaultCases(summInfo);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Once a summ info is clicked, create the default cases according to 
	 * number of positive/inconclusive cases
	 * @param summInfo
	 * @param positive
	 * @param inconclusive
	 * @throws IOException
	 */
	private void createDefaultCases(TableRow summInfo) throws IOException {
		
		// check cases number
		int positive = summInfo.getNumLabel(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES);
		int inconclusive = summInfo.getNumLabel(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES);
		
		TableSchema resultSchema = TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET);
		
		TableRow resultRow = new TableRow(resultSchema);
		
		// inject the case parent to the result
		Relation.injectParent(report, resultRow);
		Relation.injectParent(summInfo, resultRow);

		// add two default rows
		TableDao dao = new TableDao(resultSchema);
		
		resultRow.initialize();
		
		boolean isCervid = summInfo.getCode(CustomStrings.SUMMARIZED_INFO_TYPE)
			.equals(CustomStrings.SUMMARIZED_INFO_CWD_TYPE);
		
		// for cervids we need double rows
		int repeats = isCervid ? 2 : 1;

		// for each inconclusive
		for (int i = 0; i < inconclusive; ++i) {

			for (int j = 0; j < repeats; ++j) {

				// add get the id and update the fields
				int id = dao.add(resultRow);
				resultRow.setId(id);

				resultRow.initialize();

				// set assessment as inconclusive
				TableColumnValue value = new TableColumnValue();
				value.setCode(CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE);
				value.setLabel(CustomStrings.DEFAULT_ASSESS_INC_CASE_LABEL);
				resultRow.put(CustomStrings.CASE_INFO_ASSESS, value);

				if (isCervid) {
					if (j==0) {
						resultRow.put(CustomStrings.SUMMARIZED_INFO_PART, CustomStrings.OBEX_CODE);
					}
					else if (j==1) {
						resultRow.put(CustomStrings.SUMMARIZED_INFO_PART, CustomStrings.LYMPH_CODE);
					}
				}

				dao.update(resultRow);
			}
		}

		// for each positive
		for (int i = 0; i < positive; ++i) {

			for (int j = 0; j < repeats; ++j) {
				
				// add get the id and update the fields
				int id = dao.add(resultRow);
				resultRow.setId(id);
				resultRow.initialize();

				if (isCervid) {
					if (j==0) {
						resultRow.put(CustomStrings.SUMMARIZED_INFO_PART, CustomStrings.OBEX_CODE);
					}
					else if (j==1) {
						resultRow.put(CustomStrings.SUMMARIZED_INFO_PART, CustomStrings.LYMPH_CODE);
					}
				}

				dao.update(resultRow);
			}
		}
	}
	
	/**
	 * get the declared number of cases in the current row
	 * @param summInfo
	 * @return
	 */
	private int getNumberOfExpectedCases(TableRow summInfo) {
		
		int positive = summInfo.getNumLabel(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES);
		int inconclusive = summInfo.getNumLabel(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES);
		int total = positive + inconclusive;
		
		return total;
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
				
				Relation.emptyCache();
				
				CaseReport caseReport = new CaseReport(row);
				
				if (!caseReport.areMandatoryFilled()) {
					warnUser("Error", "ERR000: Cannot add analytical results. Mandatory data are missing!");
					return;
				}

				// initialize result passing also the 
				// report data and the summarized information data
				ResultDialog dialog = new ResultDialog(getParent(), report, summInfo, caseReport);
				dialog.setParentFilter(caseReport); // set the case as filter (and parent)
				dialog.open();
				
				// update children errors
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
		String progId = summInfo.getLabel(CustomStrings.SUMMARIZED_INFO_PROG_ID);
		
		StringBuilder yearRow = new StringBuilder();
		yearRow.append("Sampling year: ")
			.append(reportYear);
		
		StringBuilder monthRow = new StringBuilder();
		monthRow.append("Sampling month: ")
			.append(reportMonth);
		
		StringBuilder sourceRow = new StringBuilder();
		sourceRow.append("Animal species: ")
			.append(source);
		
		StringBuilder prodRow = new StringBuilder();
		prodRow.append("Type of production: ")
			.append(prod);

		StringBuilder ageRow = new StringBuilder();
		ageRow.append("Age class: ")
			.append(age);
		
		StringBuilder targetRow = new StringBuilder();
		targetRow.append("Target group: ")
			.append(target);
		
		StringBuilder progIdRow = new StringBuilder();
		progIdRow.append("Context ID: ")
			.append(progId);
		
		viewer.addHelp("Cases/samples")
			.addLabel("yearLabel", yearRow.toString())
			.addLabel("monthLabel", monthRow.toString())
			.addLabel("sourceLabel", sourceRow.toString())
			.addLabel("prodLabel", prodRow.toString())
			.addLabel("ageLabel", ageRow.toString())
			.addLabel("targetLabel", targetRow.toString())
			.addLabel("progIdLabel", progIdRow.toString())
			.addRowCreator("Add case/sample:")
			.addTable(CustomStrings.CASE_INFO_SHEET, true);
	}
}

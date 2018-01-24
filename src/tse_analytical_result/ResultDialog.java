package tse_analytical_result;

import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import dataset.RCLDatasetStatus;
import global_utils.Warnings;
import i18n_messages.TSEMessages;
import predefined_results_reader.PredefinedResult;
import predefined_results_reader.PredefinedResultHeader;
import report.Report;
import session_manager.TSERestoreableWindowDao;
import table_dialog.DialogBuilder;
import table_dialog.EditorListener;
import table_dialog.RowValidatorLabelProvider;
import table_relations.Relation;
import table_skeleton.TableColumn;
import table_skeleton.TableRow;
import tse_case_report.CaseReport;
import tse_components.TableDialogWithMenu;
import tse_config.CustomStrings;
import tse_summarized_information.SummarizedInfo;
import tse_validator.ResultValidator;
import window_restorer.RestoreableWindow;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Class which allows adding and editing a summarized information report.
 * @author avonva
 *
 */
public class ResultDialog extends TableDialogWithMenu {
	
	private static final Logger LOGGER = LogManager.getLogger(ResultDialog.class);
	
	private RestoreableWindow window;
	private static final String WINDOW_CODE = "AnalyticalResult";
	
	private Report report;
	private SummarizedInfo summInfo;
	private CaseReport caseInfo;
	
	public ResultDialog(Shell parent, Report report, SummarizedInfo summInfo, CaseReport caseInfo) {
		
		super(parent, TSEMessages.get("result.title"), true, false);
		
		this.report = report;
		this.summInfo = summInfo;
		this.caseInfo = caseInfo;
		
		// create the dialog
		super.create();
		
		this.window = new RestoreableWindow(getDialog(), WINDOW_CODE);
		boolean restored = window.restore(TSERestoreableWindowDao.class);
		window.saveOnClosure(TSERestoreableWindowDao.class);
		
		// add 300 px in height
		if (!restored)
			addHeight(300);
		
		setEditorListener(new EditorListener() {
			
			@Override
			public void editStarted() {}
			
			@Override
			public void editEnded(TableRow row, TableColumn field, boolean changed) {

				// update the base term and the result value if
				// the test aim was changed
				if (changed && (field.getId().equals(CustomStrings.RESULT_TEST_AIM) 
						|| field.getId().equals(CustomStrings.AN_METH_CODE))) {

					// if genotyping set base term
					if (row.getCode(CustomStrings.AN_METH_CODE).equals(CustomStrings.AN_METH_CODE_GENOTYPING)) {

						try {
							PredefinedResult predRes = PredefinedResult.getPredefinedResult(report, summInfo, caseInfo);

							row.put(CustomStrings.PARAM_CODE_BASE_TERM_COL, 
									predRes.get(PredefinedResultHeader.GENOTYPING_BASE_TERM));
							
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					else
						PredefinedResult.addParamAndResult(row, row.getCode(field.getId()));
				}
				
				// reset the aim of the test if the test type is changed
				if (changed && field.equals(CustomStrings.RESULT_TEST_TYPE)) {
					row.remove(CustomStrings.RESULT_TEST_AIM);
					row.remove(CustomStrings.AN_METH_CODE);
				}
			}
		});
		
		updateUI();
		
		askForDefault();
	}
	
	public void askForDefault() {
			
		// create default if no results are present
		if (!caseInfo.hasResults() && isEditable()) {

			int val = Warnings.warnUser(getDialog(), TSEMessages.get("warning.title"), 
					TSEMessages.get("result.confirm.default"),
					SWT.YES | SWT.NO | SWT.ICON_QUESTION);
			
			LOGGER.info("Add default results to the list? " + (val == SWT.YES));
			
			if (val == SWT.NO)
				return;
			
			try {
				PredefinedResult.createDefaultResults(report, summInfo, caseInfo);
				
				warnUser(TSEMessages.get("warning.title"), 
						TSEMessages.get("result.check.default"),
						SWT.ICON_WARNING);

				LOGGER.info("Default results created");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * make table non editable if needed
	 */
	private void updateUI() {
		
		LOGGER.info("Updating GUI");
		
		DialogBuilder panel = getPanelBuilder();
		String status = report.getLabel(AppPaths.REPORT_STATUS);
		RCLDatasetStatus datasetStatus = RCLDatasetStatus.fromString(status);
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
		
		LOGGER.info("Creating a new result");
		
		TableRow row = new TableRow(schema);
		
		Relation.injectParent(report, row);
		Relation.injectParent(summInfo, row);
		Relation.injectParent(caseInfo, row);
		
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
	public Menu createMenu() {
		Menu menu = super.createMenu();
		addRemoveMenuItem(menu);
		return menu;
	}

	@Override
	public void addWidgets(DialogBuilder viewer) {
		
		String sampleId = caseInfo.getLabel(CustomStrings.CASE_INFO_SAMPLE_ID);
		String animalId = caseInfo.getLabel(CustomStrings.CASE_INFO_ANIMAL_ID);
		String caseId = caseInfo.getLabel(CustomStrings.CASE_INFO_CASE_ID);
		
		String sampleIdRow = TSEMessages.get("result.sample.id", sampleId);
		String animalIdRow = TSEMessages.get("result.animal.id", animalId);
		String caseIdRow = TSEMessages.get("result.case.id", caseId);
		
		viewer.addHelp(TSEMessages.get("result.help.title"))
			.addRowCreator(TSEMessages.get("result.add.record"))
			.addLabel("sampLabel", sampleIdRow.toString())
			.addLabel("animalLabel", animalIdRow.toString())
			.addLabel("caseIdLabel", caseIdRow.toString())
			.addTable(CustomStrings.RESULT_SHEET, true, report, summInfo, caseInfo);  // add parent to be able to solve isVisible field
	}
}

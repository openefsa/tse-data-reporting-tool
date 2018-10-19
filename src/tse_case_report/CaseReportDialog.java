package tse_case_report;

import java.io.IOException;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import dataset.RCLDatasetStatus;
import global_utils.Warnings;
import i18n_messages.TSEMessages;
import providers.IFormulaService;
import providers.ITableDaoService;
import providers.TseReportService;
import report.Report;
import session_manager.TSERestoreableWindowDao;
import table_dialog.DialogBuilder;
import table_dialog.EditorListener;
import table_dialog.RowValidatorLabelProvider;
import table_relations.Relation;
import table_skeleton.TableColumn;
import table_skeleton.TableRow;
import tse_analytical_result.ResultDialog;
import tse_components.TableDialogWithMenu;
import tse_config.CustomStrings;
import tse_summarized_information.SummarizedInfo;
import tse_validator.CaseReportValidator;
import window_restorer.RestoreableWindow;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;
import xml_catalog_reader.Selection;

/**
 * Dialog which shows cases report related to a summarized information parent
 * @author avonva
 *
 */
public class CaseReportDialog extends TableDialogWithMenu {
	
	private static final Logger LOGGER = LogManager.getLogger(CaseReportDialog.class);
	
	private RestoreableWindow window;
	private static final String WINDOW_CODE = "CaseReport";
	
	private Report report;
	private SummarizedInfo summInfo;
	
	private TseReportService reportService;
	private ITableDaoService daoService;
	private IFormulaService formulaService;
	
	public CaseReportDialog(Shell parent, Report report, SummarizedInfo summInfo, 
			TseReportService reportService, ITableDaoService daoService, IFormulaService formulaService) {
		
		super(parent, TSEMessages.get("case.title"), true, false);
		LOGGER.info("Opening case report dialog");
		
		this.report = report;
		this.summInfo = summInfo;
		this.reportService = reportService;
		this.daoService = daoService;
		this.formulaService = formulaService;
		
		LOGGER.info("Creating dialog structure and contents");
		// create the parent structure
		super.create();
		
		LOGGER.info("Saving window preferences");
		this.window = new RestoreableWindow(getDialog(), WINDOW_CODE);
		boolean restored = window.restore(TSERestoreableWindowDao.class);
		window.saveOnClosure(TSERestoreableWindowDao.class);
		
		// add 300 px in height
		if (!restored)
			addHeight(300);
		
		LOGGER.info("Updating UI");
		// update the ui
		updateUI();
		
		LOGGER.info("Ask for default values");
		// ask for default values
		askForDefault();
		
		setEditorListener(new EditorListener() {
			
			@Override
			public void editStarted() {}
			
			@SuppressWarnings("unlikely-arg-type")
			@Override
			public void editEnded(TableRow row, TableColumn field, boolean changed) {
				if (changed && field.equals(CustomStrings.STATUS_HERD_COL)) {
					row.remove(CustomStrings.INDEX_CASE_COL);
				}
			}
		});
	}
	
	public void askForDefault() {
		
		boolean isRGT = summInfo.isRGT();
		boolean hasExpectedCases = !isRGT // cannot compute expected cases for RGT
				&& getNumberOfExpectedCases(summInfo) > 0;

		boolean canAsk = isEditable() && !summInfo.isBSEOS() 
				&& !reportService.hasChildren(summInfo, TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET))
				&& (hasExpectedCases || isRGT);
		
		LOGGER.debug("Can ask=" + canAsk);
				
		// create default cases if no cases
		// and cases were set in the aggregated data
		if (canAsk) {
			
			if (!isRGT) {
				LOGGER.debug("Warn user");
				
				Warnings.warnUser(getDialog(), TSEMessages.get("warning.title"), 
						TSEMessages.get("case.check.default"), 
						SWT.ICON_INFORMATION);
				
				LOGGER.debug("End warn user");
			}
			
			if (hasExpectedCases) {
				try {
					reportService.createDefaultCases(report, summInfo);
				} catch (IOException e) {
					e.printStackTrace();
					LOGGER.error("Cannot create default cases in summarized info with progId=" 
							+ summInfo.getProgId(), e);
				}
			}
			else if (isRGT) {
				reportService.createDefaultRGTCase(report, summInfo);
			}
		}
	}
	
	/**
	 * get the declared number of cases in the current row
	 * @param summInfo
	 * @return
	 */
	private int getNumberOfExpectedCases(TableRow summInfo) {
		
		int positive = summInfo.getNumLabel(CustomStrings.TOT_SAMPLE_POSITIVE_COL);
		int inconclusive = summInfo.getNumLabel(CustomStrings.TOT_SAMPLE_INCONCLUSIVE_COL);
		int total = positive + inconclusive;
		
		return total;
	}

	
	@Override
	public Menu createMenu() {
		
		Menu menu = super.createMenu();
		
		MenuItem openResults = new MenuItem(menu, SWT.PUSH);
		openResults.setText(TSEMessages.get("case.open.results"));
		openResults.setEnabled(false);
		
		addTableSelectionListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				openResults.setEnabled(!isTableEmpty());
			}
		});
		
		openResults.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {

				TableRow row = getSelection();
				
				if (row == null)
					return;
				
				Relation.emptyCache();
				
				CaseReport caseReport = new CaseReport(row);
				
				if (!caseReport.areMandatoryFilled() && report.isEditable()) {
					warnUser(TSEMessages.get("error.title"), TSEMessages.get("case.open.results.error"));
					return;
				}

				LOGGER.info("Opening result dialog");
				
				// initialize result passing also the 
				// report data and the summarized information data
				ResultDialog dialog = new ResultDialog(getParent(), report, summInfo, 
						caseReport, reportService, daoService, formulaService);
				dialog.setParentFilter(caseReport); // set the case as filter (and parent)
				dialog.askForDefault();
				dialog.open();
				
				// update children errors
				reportService.updateChildrenErrors(caseReport);
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
		return new CaseReportValidator(daoService);
	}

	@Override
	public void addWidgets(DialogBuilder viewer) {
		
		String reportMonth = report.getLabel(AppPaths.REPORT_MONTH_COL);
		String reportYear = report.getLabel(AppPaths.REPORT_YEAR_COL);
		String source = summInfo.getLabel(CustomStrings.SOURCE_COL);
		String prod = summInfo.getLabel(CustomStrings.PROD_COL);
		String age = summInfo.getLabel(CustomStrings.ANIMAGE_COL);
		String target = summInfo.getLabel(CustomStrings.TARGET_GROUP_COL);
		String progId = summInfo.getLabel(CustomStrings.PROG_ID_COL);
		
		String sex = summInfo.getLabel(CustomStrings.SEX_COL);
		
		String yearRow = TSEMessages.get("case.samp.year", reportYear);
		String monthRow = TSEMessages.get("case.samp.month", reportMonth);
		String sourceRow = TSEMessages.get("case.animal.species", source);
		String prodRow = TSEMessages.get("case.production.type", prod);
		String ageRow = TSEMessages.get("case.age.class", age);
		String targetRow = TSEMessages.get("case.target.group", target);
		String progIdRow = TSEMessages.get("case.prog.id", progId);
		String sexRow = TSEMessages.get("case.sex.id", sex);
		
		viewer.addHelp(TSEMessages.get("case.help.title"))
			.addComposite("labelsComp", new GridLayout(1, false), null)
			.addLabelToComposite("yearLabel", yearRow, "labelsComp")
			.addLabelToComposite("monthLabel", monthRow, "labelsComp");
		
		if (!source.isEmpty())
			viewer.addLabelToComposite("sourceLabel", sourceRow, "labelsComp");
		
		if (!prod.isEmpty())
			viewer.addLabelToComposite("prodLabel", prodRow, "labelsComp");
		
		if (!age.isEmpty())
			viewer.addLabelToComposite("ageLabel", ageRow, "labelsComp");
		
		if (!target.isEmpty())
			viewer.addLabelToComposite("targetLabel", targetRow, "labelsComp");
		
		viewer.addLabelToComposite("progIdLabel", progIdRow, "labelsComp");
			
		if (summInfo.getType().equals(CustomStrings.SUMMARIZED_INFO_CWD_TYPE))
			viewer.addLabelToComposite("sexLabel", sexRow, "labelsComp");
			
		viewer.addRowCreator(TSEMessages.get("case.add.record"))
			.addTable(CustomStrings.CASE_INFO_SHEET, true, report, summInfo);
	}
}

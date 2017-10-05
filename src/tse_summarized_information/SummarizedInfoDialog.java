package tse_summarized_information;

import java.io.IOException;
import java.util.Collection;

import javax.xml.soap.SOAPException;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Shell;

import acknowledge.Ack;
import dataset.DatasetStatus;
import table_database.TableDao;
import table_dialog.PanelBuilder;
import table_dialog.RowValidatorLabelProvider;
import table_relations.Relation;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import tse_case_report.CaseReportDialog;
import tse_components.TableDialogWithMenu;
import tse_config.CatalogLists;
import tse_config.CustomStrings;
import tse_report.Report;
import tse_validator.SummarizedInfoValidator;
import webservice.GetAck;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;
import xml_catalog_reader.Selection;

/**
 * Class which allows adding and editing a summarized information report.
 * @author avonva
 *
 */
public class SummarizedInfoDialog extends TableDialogWithMenu {

	private Report report;
	
	public SummarizedInfoDialog(Shell parent) {
		
		super(parent, "", false, false);
		
		// create the parent structure
		super.create();
		
		// default disabled
		setRowCreationEnabled(false);
		
		// add 300 px in height
		addHeight(300);
		
		// add the parents of preferences and settings
		try {
			addParentTable(Relation.getGlobalParent(CustomStrings.PREFERENCES_SHEET));
			addParentTable(Relation.getGlobalParent(CustomStrings.SETTINGS_SHEET));
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

				TableRow summInfo = (TableRow) selection.getFirstElement();

				// first validate the content of the row
				if (!validate(summInfo))
					return;
				
				// create default cases if no cases
				// and cases were set in the aggregated data
				if (!hasCases(summInfo) && getNumberOfExpectedCases(summInfo) > 0) {
					try {
						createDefaultCases(summInfo);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				// open cases dialog
				openCases(summInfo);
			}
		});
	}
	
	/**
	 * Validate the row
	 * @param summInfo
	 * @return
	 */
	private boolean validate(TableRow summInfo) {
		
		if (!summInfo.areMandatoryFilled()) {
			warnUser("Error", "Cannot add cases. Mandatory data are missing!");
			return false;
		}

		boolean hasCases = hasCases(summInfo);
		int expected = getNumberOfExpectedCases(summInfo);

		if (expected == 0 && !hasCases) {
			warnUser("Error", 
					"No positive or inconclusive cases can be detailed, please check fields Positive and Inconclusive in this table.");
			return false;
		}

		return true;
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
	
	/**
	 * Check if the summ info has cases or not
	 * @param summInfo
	 * @return
	 */
	private boolean hasCases(TableRow summInfo) {

		TableSchema schema = TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET);
		
		if (schema == null)
			return false;
		
		TableDao dao = new TableDao(schema);

		boolean hasCases = !dao.getByParentId(summInfo.getSchema().getSheetName(), summInfo.getId()).isEmpty();

		return hasCases;
	}

	/**
	 * Open the cases dialog of the summarized information
	 */
	private void openCases(TableRow summInfo) {
		
		// create a case passing also the report information
		CaseReportDialog dialog = new CaseReportDialog(getDialog(), getParentFilter(), summInfo);
		
		// filter the records by the clicked summarized information
		dialog.setParentFilter(summInfo);
		
		// add as parent also the report of the summarized information
		// which is the parent filter since we have chosen a summarized
		// information from a single report (the summ info were filtered
		// by the report)
		dialog.addParentTable(getParentFilter());
		
		dialog.open();
		
		// refresh the table when cases are changed
		refresh(summInfo);
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
		
		// for each inconclusive
		for (int i = 0; i < inconclusive; ++i) {
			
			// add get the id and update the fields
			int id = dao.add(resultRow);
			resultRow.setId(id);
			
			resultRow.initialize();
			
			// set assessment as inconclusive
			TableColumnValue value = new TableColumnValue();
			value.setCode(CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE);
			value.setLabel(CustomStrings.DEFAULT_ASSESS_INC_CASE_LABEL);
			resultRow.put(CustomStrings.CASE_INFO_ASSESS, value);
			
			dao.update(resultRow);
		}
		
		// for each positive
		for (int i = 0; i < positive; ++i) {
			
			// add get the id and update the fields
			int id = dao.add(resultRow);
			resultRow.setId(id);
			resultRow.initialize();
			dao.update(resultRow);
		}
	}
	
	/**
	 * Get the state of the current report
	 * @return
	 * @throws SOAPException
	 */
	private Ack getReportAck() throws SOAPException {
		
		if (report == null) {
			return null;
		}

		// make get ack request
		String messageId = report.getMessageId();
		
		// if no message id => the report was never sent
		if (messageId.isEmpty()) {
			return null;
		}
		
		GetAck req = new GetAck(messageId);
		
		// get state
		Ack ack = req.getAck();

		// if we have something in the ack
		if (ack.isCorrect()) {

			// save id
			String datasetId = ack.getLog().getDatasetId();
			report.setDatasetId(datasetId);
			
			// save status
			String datasetStatus = ack.getLog().getDatasetStatus().getStatus();
			report.setDatasetStatus(datasetStatus);
			
			// permanently save data
			report.update();
			
			System.out.println("Ack successful for message id " + messageId + ". Retrieved datasetId=" 
					+ datasetId + " with status=" + datasetStatus);
		}
		
		return ack;
	}
	
	@Override
	public void setParentFilter(TableRow parentFilter) {
		
		this.report = new Report(parentFilter);
		
		// enable/disable the selector when a report is opened/closed
		setRowCreationEnabled(parentFilter != null);
		
		// update ui with report data
		updateUI();
		
		super.setParentFilter(parentFilter);
	}
	
	/**
	 * Get the report that contains the summarized information
	 * @return
	 */
	public Report getReport() {
		return report;
	}
	
	@Override
	public void clear() {
		super.clear();
		initUI(); // report was closed => update ui
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
		
		// create a new summarized information
		return new SummarizedInfo(CustomStrings.SUMMARIZED_INFO_TYPE, value);
	}

	@Override
	public String getSchemaSheetName() {
		return CustomStrings.SUMMARIZED_INFO_SHEET;
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
		return new SummarizedInfoValidator();
	}

	@Override
	public void addWidgets(PanelBuilder viewer) {
		
		SelectionListener refreshStateListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				// if no report opened, stop
				if (getParentFilter() == null)
					return;
				
				Cursor wait = getDialog().getDisplay().getSystemCursor(SWT.CURSOR_WAIT);
				getDialog().setCursor(wait);

				try {
					
					Ack ack = getReportAck();
					
					if (ack == null)
						warnUser("Error", "The current report cannot be refreshed, since it was never sent to the dcf.");
					else {
						
						// update the ui accordingly
						updateUI();
						
						warnUser("Acknowledgment received", 
								"Current report state: " + ack.getState(), SWT.ICON_INFORMATION);
					}
					
				} catch (SOAPException e) {
					e.printStackTrace();
					warnUser("Error", "Check your credentials or your internet connection");
				}
				
				Cursor arrow = getDialog().getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
				getDialog().setCursor(arrow);
			}
		};
		
		viewer.addHelp("TSEs monitoring data (aggregated level)")
			.addLabel("reportLabel")
			.addLabel("statusLabel")
			.addLabel("messageIdLabel")
			.addLabel("datasetIdLabel")
			.addButton("refreshBtn", "Refresh status", refreshStateListener)
			.addRowCreator("Add data related to monitoring of:", CatalogLists.TSE_LIST)
			.addTable(CustomStrings.SUMMARIZED_INFO_SHEET, true);

		initUI();
	}
	
	/**
	 * Initialize the labels to their initial state
	 */
	private void initUI() {
		
		PanelBuilder panel = getPanelBuilder();
		
		// disable refresh until a report is opened
		panel.setEnabled("refreshBtn", false);
		
		panel.setLabelText("reportLabel", "Monthly report: no report is currently opened!");
		panel.setLabelText("statusLabel", "Status: -");
		panel.setLabelText("messageIdLabel", "DCF Message ID: -");
		panel.setLabelText("datasetIdLabel", "DCF Dataset ID: -");
	}
	
	/**
	 * Update the ui using the report information
	 * @param report
	 */
	public void updateUI() {
		
		String reportMonth = report.getMonth();
		String reportYear = report.getYear();
		String status = report.getDatasetStatus().getStatus();
		String messageId = report.getMessageId();
		String datasetId = report.getDatasetId();
		
		StringBuilder reportRow = new StringBuilder();
		reportRow.append("Monthly report: ")
			.append(reportMonth)
			.append(" ")
			.append(reportYear);
		
		StringBuilder statusRow = new StringBuilder("Status: ");
		statusRow.append(checkField(status, DatasetStatus.DRAFT.getStatus()));
		
		StringBuilder messageRow = new StringBuilder("DCF Message ID: ");
		messageRow.append(checkField(messageId, "not assigned yet"));

		StringBuilder datasetRow = new StringBuilder("DCF Dataset ID: ");
		datasetRow.append(checkField(datasetId, "not assigned yet"));
		
		PanelBuilder panel = getPanelBuilder();
		panel.setLabelText("reportLabel", reportRow.toString());
		panel.setLabelText("statusLabel", statusRow.toString());
		panel.setLabelText("messageIdLabel", messageRow.toString());
		panel.setLabelText("datasetIdLabel", datasetRow.toString());
		
		panel.setEnabled("refreshBtn", !messageId.isEmpty());
		
		// enable the table only if report status if correct
		DatasetStatus datasetStatus = DatasetStatus.fromString(status);
		boolean editableReport = datasetStatus.isEditable();
		panel.setTableEditable(editableReport);
		panel.setRowCreatorEnabled(editableReport);
	}
	
	/**
	 * Check if a field is null or empty. If so return the default value,
	 * otherwise return the field itself.
	 * @param field
	 * @param defaultValue
	 * @return
	 */
	private String checkField(String field, String defaultValue) {
		
		String out = null;
		
		if (field != null && !field.isEmpty())
			out = field;
		else
			out = defaultValue;
		
		return out;
	}
}

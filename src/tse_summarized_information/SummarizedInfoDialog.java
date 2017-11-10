package tse_summarized_information;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import app_config.AppPaths;
import dataset.DatasetStatus;
import global_utils.Warnings;
import report.Report;
import report.ReportAckManager;
import report.ReportActions;
import table_dialog.DialogBuilder;
import table_dialog.EditorListener;
import table_dialog.RowValidatorLabelProvider;
import table_relations.Relation;
import table_skeleton.TableColumn;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import table_skeleton.TableVersion;
import tse_case_report.CaseReportDialog;
import tse_components.TableDialogWithMenu;
import tse_config.CatalogLists;
import tse_config.CustomStrings;
import tse_config.DebugConfig;
import tse_report.TseReport;
import tse_validator.SummarizedInfoValidator;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Class which allows adding and editing a summarized information report.
 * @author avonva
 *
 */
public class SummarizedInfoDialog extends TableDialogWithMenu {

	private TseReport report;
	
	public SummarizedInfoDialog(Shell parent) {
		
		super(parent, "", false, false);
		
		// create the parent structure
		super.create();
		
		// default disabled
		setRowCreationEnabled(false);
		
		// add 300 px in height
		addHeight(300);
		
		setEditorListener(new EditorListener() {
			
			@Override
			public void editStarted() {}
			
			@Override
			public void editEnded(TableRow row, TableColumn field, boolean changed) {
				if (changed) {
					switch(field.getId()) {
					case CustomStrings.SUMMARIZED_INFO_TARGET_GROUP:
						row.remove(CustomStrings.SUMMARIZED_INFO_TEST_TYPE);
						row.remove(CustomStrings.AN_METH_CODE);
						break;
					case CustomStrings.SUMMARIZED_INFO_TEST_TYPE:
						row.remove(CustomStrings.AN_METH_CODE);
						break;
					}
				}
			}
		});
	}
	
	@Override
	public Menu createMenu() {
		
		Menu menu = super.createMenu();

		MenuItem addCase = new MenuItem(menu, SWT.PUSH);
		addCase.setText("Open case/sample form");
		addCase.setEnabled(false);
		
		addTableSelectionListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				addCase.setEnabled(!isTableEmpty());
			}
		});

		addCase.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				TableRow row = getSelection();
				
				if (row == null)
					return;
				
				SummarizedInfo summInfo = new SummarizedInfo(row);
				
				// first validate the content of the row
				if (!validate(summInfo) && isEditable())
					return;

				openCases(summInfo);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		addRemoveMenuItem(menu);
		
		return menu;
	}
	
	/**
	 * Validate the row
	 * @param summInfo
	 * @return
	 */
	private boolean validate(TableRow summInfo) {
		
		if (!summInfo.areMandatoryFilled()) {
			warnUser("Error", "ERR000: Cannot add cases. Mandatory data are missing!");
			return false;
		}

		return true;
	}

	/**
	 * Open the cases dialog of the summarized information
	 */
	private void openCases(SummarizedInfo summInfo) {
		
		Relation.emptyCache();
		
		// create a case passing also the report information
		CaseReportDialog dialog = new CaseReportDialog(getDialog(), this.report, summInfo);
		
		// filter the records by the clicked summarized information
		dialog.setParentFilter(summInfo);
		
		dialog.open();
		
		// set case errors if present
		summInfo.updateChildrenErrors();
		
		replace(summInfo);
	}
	
	@Override
	public void setParentFilter(TableRow parentFilter) {

		this.report = new TseReport(parentFilter);
		
		// update ui with report data
		updateUI();
		
		super.setParentFilter(parentFilter);
	}
	
	/**
	 * Get the report that contains the summarized information
	 * @return
	 */
	public TseReport getReport() {
		return report;
	}
	
	@Override
	public void clear() {
		super.clear();
		this.report = null;
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
		SummarizedInfo si = new SummarizedInfo(CustomStrings.SUMMARIZED_INFO_TYPE, value);
		
		try {
			Relation.injectGlobalParent(si, CustomStrings.SETTINGS_SHEET);
			Relation.injectGlobalParent(si, CustomStrings.PREFERENCES_SHEET);
			Relation.injectParent(report, si);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return si;
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
	public void addWidgets(DialogBuilder viewer) {
		
		SelectionListener refreshStateListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {

				// if no report opened, stop
				if (report == null)
					return;

				// refresh the status
				ReportAckManager ackManager = new ReportAckManager(getDialog(), report);
				ackManager.refreshStatus(new Listener() {
					
					@Override
					public void handleEvent(Event arg0) {
						updateUI();
					}
				});
			}
		};
		
		SelectionListener editListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				// if no report opened, stop
				if (report == null)
					return;
				
				int val = warnUser("Warning", 
						"CONF905: The report editing will be enabled, but be aware this will overwrite the current data.", 
						SWT.ICON_WARNING | SWT.YES | SWT.NO);
				
				if (val == SWT.NO)
					return;
				
				// yes, overwrite
				report.makeEditable();
				report.update();
				
				// update the ui accordingly
				updateUI();
			}
		};
		
		SelectionListener sendListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				if (report == null)
					return;
				
				if (report.isEmpty()) {
					Warnings.warnUser(getDialog(), "Error", "Cannot send an empty report!");
					return;
				}
				
				ReportActions actions = new ReportActions(getDialog(), report);
				actions.send(new Listener() {
					
					@Override
					public void handleEvent(Event arg0) {
						updateUI();
					}
				});
				
				getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
			}
		};
		
		SelectionListener rejectListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				if (report == null)
					return;
				
				ReportActions actions = new ReportActions(getDialog(), report);
				
				// reject the report and update the ui
				actions.reject(new Listener() {
					
					@Override
					public void handleEvent(Event arg0) {
						updateUI();
					}
				});
			}
		};
		
		SelectionListener submitListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				if (report == null)
					return;
				
				ReportActions actions = new ReportActions(getDialog(), report);
				
				// reject the report and update the ui
				actions.submit(new Listener() {
					
					@Override
					public void handleEvent(Event arg0) {
						updateUI();
					}
				});
			}
		};
		
		SelectionListener displayAckListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				if (report == null)
					return;
				
				// refresh the status
				ReportAckManager ackManager = new ReportAckManager(getDialog(), report);
				ackManager.displayAck();
			}
		};
		
		SelectionListener amendListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				if (report == null)
					return;
				
				ReportActions actions = new ReportActions(getDialog(), report);
				
				Report newVersion = actions.amend();
				
				if (newVersion == null)
					return;
				
				// open the new version in the tool
				setParentFilter(newVersion);
			}
		};
		
		viewer.addHelp("TSEs monitoring data (aggregated level)")
		
			.addComposite("labelsComp", new GridLayout(1, false), null)
			.addLabelToComposite("reportLabel", "labelsComp")
			.addLabelToComposite("statusLabel", "labelsComp")
			.addLabelToComposite("messageIdLabel", "labelsComp")
			.addLabelToComposite("datasetIdLabel", "labelsComp")
			
			.addComposite("panel", new GridLayout(2, false), null)
			
			.addGroupToComposite("rowCreatorComp", "panel", "Add record", new GridLayout(1, false), null)
			.addRowCreatorToComposite("rowCreatorComp", "Add data related to monitoring of:", CatalogLists.TSE_LIST)
			
			.addGroupToComposite("buttonsComp", "panel", "Toolbar", new GridLayout(7, false), null)
			
			.addButtonToComposite("editBtn", "buttonsComp", "Edit", editListener)
			.addButtonToComposite("sendBtn", "buttonsComp", "Send", sendListener)
			.addButtonToComposite("submitBtn", "buttonsComp", "Submit", submitListener)
			.addButtonToComposite("amendBtn", "buttonsComp", "Amend", amendListener)
			.addButtonToComposite("rejectBtn", "buttonsComp", "Reject", rejectListener)
			.addButtonToComposite("refreshBtn", "buttonsComp", "Refresh status", refreshStateListener)
			.addButtonToComposite("displayAckBtn", "buttonsComp", "Display ack", displayAckListener)
			
			.addTable(CustomStrings.SUMMARIZED_INFO_SHEET, true);

		initUI();
	}
	
	/**
	 * Initialize the labels to their initial state
	 */
	private void initUI() {
		
		DialogBuilder panel = getPanelBuilder();
		
		// disable refresh until a report is opened
		panel.setEnabled("refreshBtn", false);
		panel.setEnabled("editBtn", false);
		panel.setEnabled("sendBtn", false);
		panel.setEnabled("rejectBtn", false);
		panel.setEnabled("submitBtn", false);
		panel.setEnabled("amendBtn", false);
		panel.setEnabled("displayAckBtn", false);
		
		// add image to edit button
		Image editImage = new Image(Display.getCurrent(), this.getClass()
				.getClassLoader().getResourceAsStream("edit-icon.png"));
		panel.addButtonImage("editBtn", editImage);
		
		// add image to send button
		Image sendImage = new Image(Display.getCurrent(), this.getClass()
				.getClassLoader().getResourceAsStream("send-icon.png"));
		panel.addButtonImage("sendBtn", sendImage);
		
		// add image to refresh button
		Image submitImage = new Image(Display.getCurrent(), this.getClass()
				.getClassLoader().getResourceAsStream("submit-icon.png"));
		panel.addButtonImage("submitBtn", submitImage);
		
		// add image to send button
		Image amendImage = new Image(Display.getCurrent(), this.getClass()
				.getClassLoader().getResourceAsStream("amend-icon.png"));
		panel.addButtonImage("amendBtn", amendImage);
		
		// add image to send button
		Image rejectImage = new Image(Display.getCurrent(), this.getClass()
				.getClassLoader().getResourceAsStream("reject-icon.png"));
		panel.addButtonImage("rejectBtn", rejectImage);
		
		// add image to send button
		Image displayAckImage = new Image(Display.getCurrent(), this.getClass()
				.getClassLoader().getResourceAsStream("displayAck-icon.png"));
		panel.addButtonImage("displayAckBtn", displayAckImage);
		
		// add image to refresh button
		Image refreshImage = new Image(Display.getCurrent(), this.getClass()
				.getClassLoader().getResourceAsStream("refresh-icon.png"));
		panel.addButtonImage("refreshBtn", refreshImage);
		
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
		
		String reportMonth = report.getLabel(AppPaths.REPORT_MONTH);
		String reportYear = report.getYear();
		String status = report.getStatus().getLabel();
		String messageId = report.getMessageId();
		String datasetId = report.getDatasetId();
		
		StringBuilder reportRow = new StringBuilder();
		reportRow.append("Monthly report: ")
			.append(reportMonth)
			.append(" ")
			.append(reportYear);
		
		// add version if possible
		if (!TableVersion.isFirstVersion(report.getVersion())) {
			reportRow.append(" revision ")
				.append(Integer.valueOf(report.getVersion())); // remove 0 from 01..
		}
		
		StringBuilder statusRow = new StringBuilder("Status: ");
		statusRow.append(checkField(status, DatasetStatus.DRAFT.getLabel()));
		
		StringBuilder messageRow = new StringBuilder("DCF Message ID: ");
		messageRow.append(checkField(messageId, "Not assigned yet"));

		StringBuilder datasetRow = new StringBuilder("DCF Dataset ID: ");
		datasetRow.append(checkField(datasetId, "Not assigned yet"));
		
		DialogBuilder panel = getPanelBuilder();
		panel.setLabelText("reportLabel", reportRow.toString());
		panel.setLabelText("statusLabel", statusRow.toString());
		panel.setLabelText("messageIdLabel", messageRow.toString());
		panel.setLabelText("datasetIdLabel", datasetRow.toString());
		
		// enable the table only if report status if correct
		DatasetStatus datasetStatus = DatasetStatus.fromString(status);
		boolean editableReport = datasetStatus.isEditable();
		panel.setTableEditable(editableReport);
		panel.setRowCreatorEnabled(editableReport);
		
		panel.setEnabled("editBtn", !DebugConfig.disableMainPanel && datasetStatus.canBeMadeEditable());
		panel.setEnabled("sendBtn", !DebugConfig.disableMainPanel && datasetStatus.canBeSent());
		panel.setEnabled("amendBtn", !DebugConfig.disableMainPanel && datasetStatus.canBeAmended());
		panel.setEnabled("submitBtn", !DebugConfig.disableMainPanel && datasetStatus.canBeSubmitted());
		panel.setEnabled("rejectBtn", !DebugConfig.disableMainPanel && datasetStatus.canBeRejected());
		panel.setEnabled("displayAckBtn", !DebugConfig.disableMainPanel && datasetStatus.canDisplayAck());
		panel.setEnabled("refreshBtn", !DebugConfig.disableMainPanel && datasetStatus.canBeRefreshed());
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

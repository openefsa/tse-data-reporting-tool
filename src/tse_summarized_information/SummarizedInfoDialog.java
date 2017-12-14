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
import i18n_messages.TSEMessages;
import report.Report;
import report.ReportAckManager;
import report.ReportActions;
import report_validator.ReportError;
import table_dialog.DialogBuilder;
import table_dialog.EditorListener;
import table_dialog.RowValidatorLabelProvider;
import table_relations.Relation;
import table_skeleton.TableColumn;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import table_skeleton.TableVersion;
import test_case.EnumPicker;
import test_case.NumberInputDialog;
import tse_case_report.CaseReportDialog;
import tse_components.TableDialogWithMenu;
import tse_config.CatalogLists;
import tse_config.CustomStrings;
import tse_config.DebugConfig;
import tse_report.TseReport;
import tse_validator.SummarizedInfoValidator;
import tse_validator.TseReportValidator;
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
		addCase.setText(TSEMessages.get("si.open.cases"));
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
			warnUser(TSEMessages.get("error.title"), 
					TSEMessages.get("si.open.cases.error"));
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
				
				int val = warnUser(TSEMessages.get("warning.title"), 
						TSEMessages.get("edit.confirm"), 
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
					Warnings.warnUser(getDialog(), TSEMessages.get("error.title"), 
							TSEMessages.get("send.empty.report"));
					return;
				}
				
				ReportActions actions = new TseReportActions(getDialog(), report);
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
				
				ReportActions actions = new TseReportActions(getDialog(), report);
				
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
				
				ReportActions actions = new TseReportActions(getDialog(), report);
				
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
				
				ReportActions actions = new TseReportActions(getDialog(), report);
				
				Report newVersion = actions.amend();
				
				if (newVersion == null)
					return;
				
				// open the new version in the tool
				setParentFilter(newVersion);
			}
		};
		
		SelectionListener validateListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				if (report == null)
					return;

				// validate and show the errors in the browser
				TseReportValidator validator = new TseReportValidator(report);
				try {
					
					getDialog().setCursor(getDialog().getDisplay()
							.getSystemCursor(SWT.CURSOR_WAIT));
					
					// validate the report
					Collection<ReportError> errors = validator.validate();
					
					getDialog().setCursor(getDialog().getDisplay()
							.getSystemCursor(SWT.CURSOR_ARROW));
					
					// if no errors update report status
					if (errors.isEmpty()) {
						report.setStatus(DatasetStatus.LOCALLY_VALIDATED);
						report.update();
						updateUI();
						warnUser(TSEMessages.get("success.title"), TSEMessages.get("check.success"),
								SWT.ICON_INFORMATION);
					}
					else { // otherwise show them to the user
						validator.show(errors);
						warnUser(TSEMessages.get("error.title"), 
								TSEMessages.get("check.report.failed"));
					}
					
				} catch (IOException e) {
					getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
					e.printStackTrace();
					warnUser(TSEMessages.get("error.title"), 
							TSEMessages.get("check.report.error", 
									Warnings.getStackTrace(e)));
				}
			}
		};
		
		SelectionListener changeStatusListener = new SelectionAdapter() {
		
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				EnumPicker<DatasetStatus> dialog = new EnumPicker<>(getDialog(), DatasetStatus.class);
				dialog.setDefaultValue(report.getStatus());
				dialog.open();
				
				DatasetStatus status = (DatasetStatus) dialog.getSelection();
				
				if (status == null)
					return;
				
				// update the report status and UI
				report.setStatus(status);
				report.update();
				updateUI();
			}
		};
		
		SelectionListener changeMessageIdListener = new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				NumberInputDialog dialog = new NumberInputDialog(getDialog());
				dialog.setDefaultValue(report.getMessageId());
				Integer messageIdNumeric = dialog.open();
				
				String messageId = "";
				if (messageIdNumeric != null) {
					messageId = String.valueOf(messageIdNumeric);
				}
				
				if (!dialog.wasCancelled()) {

					report.setMessageId(messageId);
					report.update();
					updateUI();
				}
			}
		};	
		
		SelectionListener changeDatasetIdListener = new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				NumberInputDialog dialog = new NumberInputDialog(getDialog());
				dialog.setDefaultValue(report.getDatasetId());
				Integer dataIdNumeric = dialog.open();
				
				String datasetId = "";
				if (dataIdNumeric != null) {
					datasetId = String.valueOf(dataIdNumeric);
				}
				
				if (!dialog.wasCancelled()) {
					report.setDatasetId(datasetId);
					report.update();
					updateUI();
				}
			}
		};
		
		viewer
			.addHelp(TSEMessages.get("si.help.title"))
		
			.addComposite("labelsComp", new GridLayout(1, false), null)
			.addLabelToComposite("reportLabel", "labelsComp")
			.addLabelToComposite("statusLabel", "labelsComp")
			.addLabelToComposite("messageIdLabel", "labelsComp")
			.addLabelToComposite("datasetIdLabel", "labelsComp");

			// if debug add change status button
			if (DebugConfig.debug) {
				viewer
					.addGroup("debugPanel", TSEMessages.get("si.debug.panel"), new GridLayout(3, false), null)
					.addButtonToComposite("changeStatusBtn", "debugPanel", 
						TSEMessages.get("si.debug.change.status"), changeStatusListener)
					.addButtonToComposite("changeMessageIdBtn", "debugPanel", 
						TSEMessages.get("si.debug.change.mexid"), changeMessageIdListener)
					.addButtonToComposite("changeDatasetIdBtn", "debugPanel", 
						TSEMessages.get("si.debug.change.dataid"), changeDatasetIdListener);
			}
		
		viewer	
			.addComposite("panel", new GridLayout(1, false), null)
			
			.addGroupToComposite("buttonsComp", "panel", TSEMessages.get("si.toolbar.title"), new GridLayout(8, false), null)
			
			.addButtonToComposite("editBtn", "buttonsComp", TSEMessages.get("si.toolbar.edit"), editListener)
			.addButtonToComposite("validateBtn", "buttonsComp", TSEMessages.get("si.toolbar.check"), validateListener)
			.addButtonToComposite("sendBtn", "buttonsComp", TSEMessages.get("si.toolbar.send"), sendListener)
			.addButtonToComposite("submitBtn", "buttonsComp", TSEMessages.get("si.toolbar.submit"), submitListener)
			.addButtonToComposite("amendBtn", "buttonsComp", TSEMessages.get("si.toolbar.amend"), amendListener)
			.addButtonToComposite("rejectBtn", "buttonsComp", TSEMessages.get("si.toolbar.reject"), rejectListener)
			.addButtonToComposite("refreshBtn", "buttonsComp", TSEMessages.get("si.toolbar.refresh.status"), refreshStateListener)
			.addButtonToComposite("displayAckBtn", "buttonsComp", TSEMessages.get("si.toolbar.display.ack"), displayAckListener)
			
			.addGroupToComposite("rowCreatorComp", "panel", TSEMessages.get("si.add.record"), new GridLayout(1, false), null)
			.addRowCreatorToComposite("rowCreatorComp", TSEMessages.get("si.add.record.label"), CatalogLists.TSE_LIST)
			
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
		panel.setEnabled("validateBtn", false);
		panel.setEnabled("editBtn", false);
		panel.setEnabled("sendBtn", false);
		panel.setEnabled("rejectBtn", false);
		panel.setEnabled("submitBtn", false);
		panel.setEnabled("amendBtn", false);
		panel.setEnabled("displayAckBtn", false);
		panel.setEnabled("changeStatusBtn", false);
		panel.setEnabled("changeMessageIdBtn", false);
		panel.setEnabled("changeDatasetIdBtn", false);
		
		// add image to edit button
		Image editImage = new Image(Display.getCurrent(), this.getClass()
				.getClassLoader().getResourceAsStream("edit-icon.png"));
		panel.addButtonImage("editBtn", editImage);
		
		// add image to send button
		Image validateImage = new Image(Display.getCurrent(), this.getClass()
				.getClassLoader().getResourceAsStream("checkReport-icon.png"));
		panel.addButtonImage("validateBtn", validateImage);
		
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
		
		panel.setLabelText("reportLabel", TSEMessages.get("si.report.void"));
		panel.setLabelText("statusLabel", TSEMessages.get("si.dataset.status", TSEMessages.get("si.no.data")));
		
		panel.setLabelText("messageIdLabel", TSEMessages.get("si.message.id", 
				TSEMessages.get("si.no.data")));
		
		panel.setLabelText("datasetIdLabel", TSEMessages.get("si.dataset.id", 
				TSEMessages.get("si.no.data")));
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
		
		String reportRow;
		
		// add version if possible
		if (!TableVersion.isFirstVersion(report.getVersion())) {
			int revisionInt = Integer.valueOf(report.getVersion()); // remove 0 from 01..
			String revision = String.valueOf(revisionInt);
			reportRow = TSEMessages.get("si.report.opened.revision", reportYear, reportMonth, revision);
		}
		else {
			reportRow = TSEMessages.get("si.report.opened", reportYear, reportMonth);
		}

		String statusRow = TSEMessages.get("si.dataset.status", 
				checkField(status, DatasetStatus.DRAFT.getLabel()));
		
		String messageRow = TSEMessages.get("si.message.id", 
				checkField(messageId, TSEMessages.get("si.missing.data")));

		String datasetRow = TSEMessages.get("si.dataset.id", 
				checkField(datasetId, TSEMessages.get("si.missing.data")));
		
		DialogBuilder panel = getPanelBuilder();
		panel.setLabelText("reportLabel", reportRow);
		panel.setLabelText("statusLabel", statusRow);
		panel.setLabelText("messageIdLabel", messageRow);
		panel.setLabelText("datasetIdLabel", datasetRow);
		
		// enable the table only if report status if correct
		DatasetStatus datasetStatus = DatasetStatus.fromString(status);
		boolean editableReport = datasetStatus.isEditable();
		panel.setTableEditable(editableReport);
		panel.setRowCreatorEnabled(editableReport);
		
		panel.setEnabled("editBtn", !DebugConfig.disableMainPanel && datasetStatus.canBeMadeEditable());
		panel.setEnabled("validateBtn", !DebugConfig.disableMainPanel && datasetStatus.canBeChecked());
		panel.setEnabled("sendBtn", !DebugConfig.disableMainPanel && datasetStatus.canBeSent());
		panel.setEnabled("amendBtn", !DebugConfig.disableMainPanel && datasetStatus.canBeAmended());
		panel.setEnabled("submitBtn", !DebugConfig.disableMainPanel && datasetStatus.canBeSubmitted());
		panel.setEnabled("rejectBtn", !DebugConfig.disableMainPanel && datasetStatus.canBeRejected());
		panel.setEnabled("displayAckBtn", !DebugConfig.disableMainPanel && datasetStatus.canDisplayAck());
		panel.setEnabled("refreshBtn", !DebugConfig.disableMainPanel && datasetStatus.canBeRefreshed());
		panel.setEnabled("changeStatusBtn", true);
		panel.setEnabled("changeMessageIdBtn", true);
		panel.setEnabled("changeDatasetIdBtn", true);
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

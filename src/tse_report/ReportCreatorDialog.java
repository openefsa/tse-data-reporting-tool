package tse_report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import app_config.PropertiesReader;
import dataset.IDataset;
import dataset.RCLDatasetStatus;
import global_utils.Message;
import global_utils.Warnings;
import i18n_messages.TSEMessages;
import providers.IReportService;
import providers.RCLError;
import session_manager.TSERestoreableWindowDao;
import soap.DetailedSOAPException;
import table_dialog.DialogBuilder;
import table_dialog.RowValidatorLabelProvider;
import table_dialog.TableDialog;
import table_relations.Relation;
import table_skeleton.TableRow;
import table_skeleton.TableVersion;
import tse_config.CustomStrings;
import tse_validator.SimpleRowValidatorLabelProvider;
import window_restorer.RestoreableWindow;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Form to create a new report
 * @author avonva
 *
 */
public class ReportCreatorDialog extends TableDialog {
	
	private static final Logger LOGGER = LogManager.getLogger(ReportCreatorDialog.class);
	
	private IReportService reportService;
	private RestoreableWindow window;
	private static final String WINDOW_CODE = "ReportCreator";
	
	public ReportCreatorDialog(Shell parent, IReportService reportService) {
		super(parent, TSEMessages.get("new.report.title"), true, true);
		
		// create the parent structure
		super.create();
		
		this.reportService = reportService;
		
		this.window = new RestoreableWindow(getDialog(), WINDOW_CODE);
		window.restore(TSERestoreableWindowDao.class);
		window.saveOnClosure(TSERestoreableWindowDao.class);
	}
	
	@Override
	public String getSchemaSheetName() {
		return CustomStrings.REPORT_SHEET;
	}

	@Override
	public Collection<TableRow> loadInitialRows(TableSchema schema, TableRow parentTable) {
		
		Collection<TableRow> rows = new ArrayList<>();
		TseReport row = new TseReport();
		
		// add preferences to the report
		try {
			Relation.injectGlobalParent(row, CustomStrings.PREFERENCES_SHEET);
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Cannot inject global parent=" + CustomStrings.PREFERENCES_SHEET, e);
		}
		
		row.initialize();
		
		// set as default the first version
		row.setVersion(TableVersion.getFirstVersion());
		
		// by default the report status is draft for new reports
		row.setStatus(RCLDatasetStatus.DRAFT.getStatus());
		
		// update the formulas of the report
		// to compute the sender id
		row.updateFormulas();

		rows.add(row);
		return rows;
	}	
	
	@Override
	public boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {
		
		TseReport report = (TseReport) rows.iterator().next();

		Message msg = null;
		
		getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
		
		try {
			RCLError error = reportService.create(report);
			
			if (error != null)
				msg = getErrorMessage(error, report);
			
		} catch (DetailedSOAPException e) {
			e.printStackTrace();
			
			LOGGER.error("Cannot create report", e);
			msg = Warnings.createSOAPWarning(e);
		}
		finally {
			getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
		}
		
		boolean errorOccurred = msg != null;
		
		if (msg == null) {
			msg = Warnings.create(TSEMessages.get("success.title"), 
					TSEMessages.get("new.report.success"), 
					SWT.ICON_INFORMATION);
		}
		
		msg.open(getDialog());
		
		return !errorOccurred;
	}
	
	private Message getErrorMessage(RCLError error, TseReport report) {
		
		IDataset oldReport = (IDataset) error.getData();

		String message = null;
		boolean fatal = false;
		
		switch(error.getCode()) {
		case "WARN304":
			message = TSEMessages.get("new.report.already.present.locally");
			break;
		case "WARN301":
			message = TSEMessages.get("new.report.acc.dwh", oldReport.getId());
			break;
		case "WARN302":
			message = TSEMessages.get("new.report.submitted", oldReport.getId());
			break;
		case "WARN303":
			message = TSEMessages.get("new.report.other", oldReport.getId(), 
					oldReport.getRCLStatus().getLabel());
			break;
		case "WARN300":
			message = TSEMessages.get("new.report.processing", oldReport.getId());
			break;
		case "ERR300":
			fatal = true;
			message = TSEMessages.get("new.report.failed", PropertiesReader.getSupportEmail());
			break;
		}
		
		Message msg = fatal ? Warnings.createFatal(message, report, oldReport) : Warnings.create(message);
		
		return message != null ? msg : null;
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
		return new SimpleRowValidatorLabelProvider();
	}

	@Override
	public void addWidgets(DialogBuilder viewer) {

		viewer.addHelp(TSEMessages.get("new.report.help.title"))
			.addTable(CustomStrings.REPORT_SHEET, true);
	}
}

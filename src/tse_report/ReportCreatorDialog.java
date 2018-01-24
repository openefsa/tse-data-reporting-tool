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
import dataset.Dataset;
import dataset.RCLDatasetStatus;
import global_utils.Warnings;
import i18n_messages.TSEMessages;
import report.ReportException;
import session_manager.TSERestoreableWindowDao;
import soap.MySOAPException;
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
	
	private RestoreableWindow window;
	private static final String WINDOW_CODE = "ReportCreator";
	
	public ReportCreatorDialog(Shell parent) {
		super(parent, TSEMessages.get("new.report.title"), true, true);
		
		// create the parent structure
		super.create();
		
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
		
		// if the report is already present
		// show error message
		if (report.isLocallyPresent()) {
			warnUser(TSEMessages.get("error.title"), TSEMessages.get("new.report.fail"));
			return false;
		}

		// change the cursor to wait
		getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
		
		String title = null;
		String message = null;
		Dataset oldReport = null;
		
		try {
			
			oldReport = report.getLatestDataset();
			
		} catch (MySOAPException e) {
			
			e.printStackTrace();
			
			LOGGER.error("Cannot create report", e);
			
			String[] warnings = Warnings.getSOAPWarning(e);
			title = warnings[0];
			message = warnings[1];
			
		} catch (ReportException e) {
			e.printStackTrace();
			
			LOGGER.error("Cannot create report", e);
			
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("new.report.failed.no.senderId", 
					PropertiesReader.getSupportEmail(), e.getMessage());
		}
		finally {
			// change the cursor to old cursor
			getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
		}
		
		// if we had an exception warn user and return
		if (message != null) {
			warnUser(title, message);
			return false;
		}
		
		// if the report already exists
		// with the selected sender dataset id
		if (oldReport != null) {
			
			// check if there are errors
			String errorMessage = getErrorMessage(oldReport);

			// if there are errors
			if (errorMessage != null) {
				warnUser(TSEMessages.get("error.title"), errorMessage);
				return false;
			}

			// if no errors, then we are able to create the report
			
			switch (oldReport.getRCLStatus()) {
			case DELETED:
				// we ignore deleted datasets
				report.save();
				break;
			case REJECTED:
				// we mantain the same dataset id
				// of the rejected dataset, but actually
				// we create a new report with that
				report.setId(oldReport.getId());
				report.save();
				break;
				
			default:
				break;
			}
		}

		// if no conflicts create the new report
		report.save();
		
		warnUser(TSEMessages.get("success.title"), TSEMessages.get("new.report.success"), SWT.ICON_INFORMATION);
		
		return true;
	}
	
	/**
	 * get the error message that needs to be displayed if
	 * an old report already exists
	 * @param oldReport
	 * @return
	 */
	private String getErrorMessage(Dataset oldReport) {
		
		String message = null;
		
		switch(oldReport.getRCLStatus()) {
		case ACCEPTED_DWH:
			message = TSEMessages.get("new.report.acc.dwh", oldReport.getId());
			break;
		case SUBMITTED:
			message = TSEMessages.get("new.report.submitted", oldReport.getId());
			break;
		case VALID:
		case VALID_WITH_WARNINGS:
		case REJECTED_EDITABLE:
			message = TSEMessages.get("new.report.other", oldReport.getId(), 
					oldReport.getRCLStatus().getLabel());
			break;
		case PROCESSING:
			message = TSEMessages.get("new.report.processing", oldReport.getId());
			break;
		case DELETED:
		case REJECTED:
			break;
		default:
			message = TSEMessages.get("new.report.failed", PropertiesReader.getSupportEmail());
			break;
		}
		
		return message;
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

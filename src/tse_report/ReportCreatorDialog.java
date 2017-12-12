package tse_report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import app_config.PropertiesReader;
import dataset.Dataset;
import dataset.DatasetStatus;
import global_utils.Warnings;
import i18n_messages.TSEMessages;
import report.ReportException;
import table_dialog.DialogBuilder;
import table_dialog.RowValidatorLabelProvider;
import table_dialog.TableDialog;
import table_relations.Relation;
import table_skeleton.TableRow;
import table_skeleton.TableVersion;
import tse_config.CustomStrings;
import tse_validator.SimpleRowValidatorLabelProvider;
import webservice.MySOAPException;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Form to create a new report
 * @author avonva
 *
 */
public class ReportCreatorDialog extends TableDialog {
	
	public ReportCreatorDialog(Shell parent) {
		super(parent, TSEMessages.get("new.report.title"), true, true);
		
		// create the parent structure
		super.create();
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
		}
		
		row.initialize();
		
		// set as default the first version
		row.setVersion(TableVersion.getFirstVersion());
		
		// by default the report status is draft for new reports
		row.setStatus(DatasetStatus.DRAFT.getStatus());
		
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
			
			oldReport = report.getDataset();
			
		} catch (MySOAPException e) {
			
			e.printStackTrace();
			
			String[] warnings = Warnings.getSOAPWarning(e);
			title = warnings[0];
			message = warnings[1];
			
		} catch (ReportException e) {
			e.printStackTrace();
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
			
			switch (oldReport.getStatus()) {
			case DELETED:
				// we ignore deleted datasets
				report.save();
				break;
			case REJECTED:
				// we mantain the same dataset id
				// of the rejected dataset, but actually
				// we create a new report with that
				report.setDatasetId(oldReport.getId());
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
		
		switch(oldReport.getStatus()) {
		case ACCEPTED_DWH:
			message = TSEMessages.get("new.report.acc.dwh", oldReport.getId());
			break;
		case SUBMITTED:
			message = TSEMessages.get("new.report.submitted", oldReport.getId());
			break;
		case VALID:
		case VALID_WITH_WARNINGS:
		case REJECTED_EDITABLE:
			message = TSEMessages.get("new.report.other", oldReport.getId(), oldReport.getStatus().getLabel());
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

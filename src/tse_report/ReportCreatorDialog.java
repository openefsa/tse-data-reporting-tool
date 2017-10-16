package tse_report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import dataset.Dataset;
import dataset.DatasetStatus;
import global_utils.Warnings;
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
		super(parent, "New report", true, true);
		
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
			warnUser("Error", "WARN304: The report already exists. Please open it.");
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
			
			String[] warnings = Warnings.getSOAPWarning(e.getError());
			title = warnings[0];
			message = warnings[1];
			
		} catch (ReportException e) {
			e.printStackTrace();
			title = "General error";
			message = "ERR700: It was not possible to retrieve the current report sender id. Please call technical assistance.";
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
				warnUser("Error", errorMessage);
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
			message = "ERR301: An existing report in DCF with dataset id "
					+ oldReport.getId()
					+ " was found in status ACCEPTED_DWH. To amend it please download and open it.";
			break;
		case SUBMITTED:
			message = "ERR302: An existing report in DCF with dataset id "
					+ oldReport.getId()
					+ " was found in status SUBMITTED. Please reject it in the validation report if changes are needed.";
			break;
		case VALID:
		case VALID_WITH_WARNINGS:
		case REJECTED_EDITABLE:
			message = "ERR303: An existing report in DCF with dataset id "
					+ oldReport.getId()
					+ " was found in status " 
					+ oldReport.getStatus() 
					+ ". To apply changes please download and open it.";
			break;
		case PROCESSING:
			message = "ERR300: An existing report in DCF with dataset id "
					+ oldReport.getId()
					+ " was found in status PROCESSING. Please wait the completion of the validation.";
			break;
		case DELETED:
		case REJECTED:
			break;
		default:
			message = "ERR300: An error occurred due to a conflicting dataset in DCF. Please contact zoonoses_support@efsa.europa.eu.";
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

		viewer.addHelp("Creation of a new report")
			.addTable(CustomStrings.REPORT_SHEET, true);
	}
}

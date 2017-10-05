package tse_report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.soap.SOAPException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import app_config.PropertiesReader;
import dataset.Dataset;
import dataset.DatasetList;
import dataset.DatasetStatus;
import table_database.TableDao;
import table_dialog.PanelBuilder;
import table_dialog.RowValidatorLabelProvider;
import table_dialog.TableDialog;
import table_relations.Relation;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_validator.SimpleRowValidatorLabelProvider;
import webservice.GetDatasetList;
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
		Report row = new Report();
		
		// add preferences to the report
		try {
			Relation.injectGlobalParent(row, CustomStrings.PREFERENCES_SHEET);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		row.initialize();
		
		// by default the report status is draft for new reports
		row.setDatasetStatus(DatasetStatus.DRAFT.getStatus());
		
		// update the formulas of the report
		// to compute the sender id
		row.updateFormulas();

		rows.add(row);
		return rows;
	}

	/**
	 * Check if the current report is already present in the database
	 * @param schema
	 * @param currentReport
	 * @return
	 */
	private boolean isLocallyPresent(Report currentReport) {
		
		String year = currentReport.getYear();
		String month = currentReport.getMonth();
		String country = currentReport.getCountry();
		
		// check if the report is already in the db
		TableDao dao = new TableDao(currentReport.getSchema());
		for (TableRow row : dao.getAll()) {
			
			Report report = new Report(row);
			
			String year2 = report.getYear();
			String month2 = report.getMonth();
			String country2 = report.getCountry();
			
			// if same month and year and country we have the same
			if (year.equals(year2) && month.equals(month2) && country.equals(country2))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Check if the current report is already present in the DCF
	 * by using the sender id attribute. If present the method
	 * returns the already present dataset
	 * @param report
	 * @return
	 * @throws SOAPException
	 * @throws ReportException 
	 */
	private DatasetList isRemotelyPresent(Report report, String dcCode) throws MySOAPException, ReportException {
		
		// check if the Report is in the DCF
		GetDatasetList request = new GetDatasetList(dcCode);

		DatasetList datasets = request.getList();
		
		String senderId = report.getSenderId();
		
		if (senderId == null) {
			throw new ReportException("Cannot retrieve the report sender id for " + report);
		}
		
		return datasets.filterBySenderId(senderId);
	}

	
	@Override
	public boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {
		
		Report report = (Report) rows.iterator().next();
		
		// if the report is already present
		// show error message
		if (isLocallyPresent(report)) {
			warnUser("Error", "The report already exists. Please open it.");
			return false;
		}

		// change the cursor to wait
		getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
		
		String title = null;
		String message = null;
		DatasetList oldReports = new DatasetList();
		
		try {
			
			oldReports = isRemotelyPresent(report, PropertiesReader.getDataCollectionCode());
			
		} catch (MySOAPException e) {
			
			e.printStackTrace();
			
			switch(e.getError()) {
			case NO_CONNECTION:
				title = "Connection error";
				message = "It was not possible to connect to the DCF, please check your internet connection.";
				break;
			case UNAUTHORIZED:
			case FORBIDDEN:
				title = "Wrong credentials";
				message = "Your credentials are incorrect. Please check them in the Settings.";
				break;
			}
		} catch (ReportException e) {
			e.printStackTrace();
			title = "General error";
			message = "It was not possible to retrieve the current report sender id. Please call technical assistance.";
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
		
		// if at least one report already exists
		// with the selected sender dataset id
		if (!oldReports.isEmpty()) {

			// NOTE: we consider just the first report (the most recent)
			Dataset oldReport = oldReports.get(0);
			
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
			message = "An existing report in DCF with dataset id "
					+ oldReport.getId()
					+ " was found in status ACCEPTED_DWH. To amend it please download and open it.";
			break;
		case SUBMITTED:
			message = "An existing report in DCF with dataset id "
					+ oldReport.getId()
					+ " was found in status SUBMITTED. Please reject it in the validation report if changes are needed.";
			break;
		case VALID:
		case VALID_WITH_WARNINGS:
		case REJECTED_EDITABLE:
			message = "An existing report in DCF with dataset id "
					+ oldReport.getId()
					+ " was found in status " 
					+ oldReport.getStatus() 
					+ ". To apply changes please download and open it.";
			break;
		case PROCESSING:
			message = "An existing report in DCF with dataset id "
					+ oldReport.getId()
					+ " was found in status PROCESSING. Please wait the completion of the validation.";
			break;
		case DELETED:
		case REJECTED:
			break;
		default:
			message = "An error occurred due to a conflicting dataset in DCF. Please contact zoonoses_support@efsa.europa.eu.";
			break;
		}
		
		return message;
	}
	
	public class ReportException extends Exception {
		private static final long serialVersionUID = 1L;
		public ReportException(String text) {
			super(text);
		}
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
	public void addWidgets(PanelBuilder viewer) {

		viewer.addHelp("Creation of a new report")
			.addTable(CustomStrings.REPORT_SHEET, true);
	}
}

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
import table_database.TableDao;
import table_dialog.RowValidatorLabelProvider;
import table_dialog.TableDialog;
import table_dialog.TableViewWithHelp.RowCreationMode;
import table_relations.Relation;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import tse_validator.SimpleRowValidatorLabelProvider;
import webservice.GetDatasetList;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Form to create a new report
 * @author avonva
 *
 */
public class ReportCreatorDialog extends TableDialog {
	
	public ReportCreatorDialog(Shell parent) {
		super(parent, "New report", "Creation of a new report", true, RowCreationMode.NONE, true);
	}
	
	@Override
	public String getSchemaSheetName() {
		return CustomStrings.REPORT_SHEET;
	}

	@Override
	public Collection<TableRow> loadInitialRows(TableSchema schema, TableRow parentTable) {
		
		Collection<TableRow> rows = new ArrayList<>();
		TableRow row = new TableRow(schema);
		

		// add preferences to the report
		try {
			Relation.injectGlobalParent(row, CustomStrings.PREFERENCES_SHEET);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		row.initialize();
		
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
	private boolean isLocallyPresent(TableRow currentReport) {
		
		String year = currentReport.get(CustomStrings.REPORT_YEAR).getCode();
		String month = currentReport.get(CustomStrings.REPORT_MONTH).getCode();
		String country = currentReport.get(CustomStrings.REPORT_COUNTRY).getCode();
		
		// check if the report is already in the db
		TableDao dao = new TableDao(currentReport.getSchema());
		for (TableRow report : dao.getAll()) {
			
			String year2 = report.get(CustomStrings.REPORT_YEAR).getCode();
			String month2 = report.get(CustomStrings.REPORT_MONTH).getCode();
			String country2 = report.get(CustomStrings.REPORT_COUNTRY).getCode();
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
	private Dataset isRemotelyPresent(TableRow report, String dcCode) throws SOAPException, ReportException {
		
		// check if the Report is in the DCF
		GetDatasetList request = new GetDatasetList(dcCode);

		DatasetList datasets = request.getlist();

		TableColumnValue value = report.get(CustomStrings.REPORT_SENDER_ID);
		
		if (value == null) {
			throw new ReportException("Cannot retrieve the report sender id for " + report);
		}
		
		// get the sender id label
		String senderId = value.getLabel();

		return datasets.getBySenderId(senderId);
	}

	
	@Override
	public boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {
		
		TableRow report = rows.iterator().next();
		
		// if the report is already present
		// show error message
		if (isLocallyPresent(report)) {
			
			// TODO allow possibly creating a new version
			warnUser("Error", "The chosen report was already created! Please open it and make there the required changes.");
			return false;
		}

		try {
			
			// TODO ask for download or for overriding the found report
			
			// change the cursor to wait
			getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
			
			Dataset oldReport = isRemotelyPresent(report, PropertiesReader.getDataCollectionCode());

			// change the cursor to old cursor
			getDialog().setCursor(getDialog().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
			
			// if no report found skip
			if (oldReport != null) {
				
				// if the report is not editable, then it is a useless version of it
				// then allow creating the report
				if (!oldReport.isEditable()) {
					warnUser("Warning", "The chosen report is already present in the DCF, "
							+ "but its status is not valid. A new report was created to replace it.", SWT.ICON_WARNING);
					//TODO mark the report as replacement
				}
				else {
					//TODO ask download or replace
					warnUser("Error", "The chosen report is already present in the DCF!");
					return false;
				}
			}
			
		} catch (SOAPException e) {
			warnUser("Connection error", "It was not possible to connect to the DCF, please check your credentials and/or your internet connection.");
			return false;
		} catch (ReportException e) {
			e.printStackTrace();
			warnUser("General error", "It was not possible to retrieve the current report sender id. Please call technical assistance.");
			return false;
		}

		// add the report to the database
		for (TableRow row : rows) {
			TableDao dao = new TableDao(schema);
			dao.add(row);
		}
		
		return true;
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
}

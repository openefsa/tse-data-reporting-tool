package tse_report;

import java.io.IOException;

import dataset.Dataset;
import dataset.DatasetStatus;
import table_relations.Relation;
import table_skeleton.TableRow;
import tse_config.CatalogLists;
import tse_config.CustomStrings;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class Report extends TableRow {  // TODO need to be integrated

	public Report() {
		super(getReportSchema());
	}
	
	public Report(TableRow row) {
		super(row);
	}
	
	/**
	 * Get the schema of a report
	 * @return
	 */
	public static TableSchema getReportSchema() {
		return TableSchemaList.getByName(CustomStrings.REPORT_SHEET);
	}
	
	/**
	 * Create a report from a dataset
	 * @param dataset
	 * @return
	 */
	public static Report fromDataset(Dataset dataset) {
		
		Report report = new Report();
		
		String senderDatasetId = dataset.getOperation().getSenderDatasetId();
		
		report.setDatasetId(dataset.getOperation().getDatasetId());
		report.setSenderId(senderDatasetId);
		
		if (dataset.getStatus() != null)
			report.setDatasetStatus(dataset.getStatus().getStatus());
		else
			report.setDatasetStatus(DatasetStatus.DRAFT.getStatus());
		
		// split FR1705... into country year and month
		if (senderDatasetId.length() != 6) {
			System.out.println("Report#fromDataset Cannot parse sender dataset id, expected 6 characters, found " + senderDatasetId);
			report.setCountry("");
			report.setYear("");
			report.setMonth("");
		}
		else {
			String countryCode = senderDatasetId.substring(0, 2);
			String year = "20" + senderDatasetId.substring(2, 4);
			String month = senderDatasetId.substring(4, 6);
			
			// remove the padding
			if (month.substring(0, 1).equals("0"))
				month = month.substring(1, 2);
			
			report.setCountry(countryCode);
			report.setYear(year);
			report.setMonth(month);
		}
		
		report.setVersion("1.0.0");
		report.setMessageId("");
		
		// add the preferences
		try {
			Relation.injectGlobalParent(report, CustomStrings.PREFERENCES_SHEET);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return report;
	}

	public String getYear() {
		return this.getCode(CustomStrings.REPORT_YEAR);
	}
	
	public void setYear(String year) {
		this.put(CustomStrings.REPORT_YEAR, 
				getTableColumnValue(year, CatalogLists.YEARS_LIST));
	}
	
	public String getMonth() {
		return this.getCode(CustomStrings.REPORT_MONTH);
	}
	
	public void setMonth(String month) {
		this.put(CustomStrings.REPORT_MONTH, 
				getTableColumnValue(month, CatalogLists.MONTHS_LIST));
	}
	
	public void setCountry(String country) {
		this.put(CustomStrings.REPORT_COUNTRY, 
				getTableColumnValue(country, CatalogLists.COUNTRY_LIST));
	}
	
	public String getCountry() {
		return this.getCode(CustomStrings.REPORT_COUNTRY);
	}
	
	public String getMessageId() {
		return this.getCode(CustomStrings.REPORT_MESSAGE_ID);
	}
	
	public void setMessageId(String id) {
		this.put(CustomStrings.REPORT_MESSAGE_ID, id);
	}
	
	public String getDatasetId() {
		return this.getCode(CustomStrings.REPORT_DATASET_ID);
	}
	
	public void setDatasetId(String id) {
		this.put(CustomStrings.REPORT_DATASET_ID, id);
	}
	
	public String getSenderId() {
		return this.getCode(CustomStrings.REPORT_SENDER_ID);
	}
	
	public void setSenderId(String id) {
		this.put(CustomStrings.REPORT_SENDER_ID, id);
	}
	
	public void setVersion(String version) {
		this.put(CustomStrings.REPORT_VERSION, version);
	}
	
	/**
	 * Get the status of the dataset attached to the report
	 * @return
	 */
	public DatasetStatus getDatasetStatus() {
		String status = getCode(CustomStrings.REPORT_STATUS);
		return DatasetStatus.fromString(status);
	}
	
	public void setDatasetStatus(String status) {
		this.put(CustomStrings.REPORT_STATUS, status);
	}
	
	/**
	 * Check if the dataset can be edited or not
	 * @return
	 */
	public boolean isEditable() {
		return getDatasetStatus().isEditable();
	}
}

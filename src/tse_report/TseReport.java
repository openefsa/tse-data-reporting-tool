package tse_report;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

import org.xml.sax.SAXException;

import app_config.AppPaths;
import dataset.Dataset;
import dataset.DcfDatasetStatus;
import dataset.RCLDatasetStatus;
import message.MessageConfigBuilder;
import message.SendMessageException;
import message_creator.OperationType;
import report.EFSAReport;
import report.Report;
import report.ReportException;
import report.ReportList;
import soap.MySOAPException;
import table_database.TableDao;
import table_relations.Relation;
import table_skeleton.TableRow;
import table_skeleton.TableVersion;
import tse_analytical_result.AnalyticalResult;
import tse_case_report.CaseReport;
import tse_config.CatalogLists;
import tse_config.CustomStrings;
import tse_summarized_information.SummarizedInfo;
import tse_validator.SummarizedInfoValidator;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

/**
 * A TSE report
 * @author avonva
 *
 */
public class TseReport extends Report implements TseTableRow {

	public TseReport() {
		super(getReportSchema());
	}
	
	public TseReport(TableRow row) {
		super(row);
	}
	
	public static TableSchema getReportSchema() {
		return TableSchemaList.getByName(CustomStrings.REPORT_SHEET);
	}
	
	public static TseReport createDefault() throws IOException {
		
		TseReport report = new TseReport();
		report.setCountry("Default");
		report.setSenderId("Default");
		report.setStatus(RCLDatasetStatus.DRAFT);
		report.setMonth("");
		report.setYear("");
		report.setVersion(TableVersion.getFirstVersion());
		report.setMessageId("");
		report.setId("");
		
		Relation.injectGlobalParent(report, CustomStrings.PREFERENCES_SHEET);
		
		return report;
	}
	
	@Override
	public Collection<TableRow> getRecords() {

		// children schemas
		TableSchema[] schemas = new TableSchema[] {
				TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET),
				TableSchemaList.getByName(CustomStrings.RESULT_SHEET)
		};

		Collection<TableRow> records = getRecords(schemas);
		
		// remove random genotyping from the summarized information
		List<TableRow> filteredRecords = records.stream().filter(new Predicate<TableRow>() {
			@Override
			public boolean test(TableRow arg0) {
				
				if (SummarizedInfo.isSummarizedInfo(arg0)) {
					
					String type = arg0.getCode(CustomStrings.SUMMARIZED_INFO_TYPE);
					
					if (type.equals(CustomStrings.SUMMARIZED_INFO_RGT_TYPE))
						return false;
				}
				
				return true;
			}
		}).collect(Collectors.toList());
		
		return filteredRecords;
	}
	
	/**
	 * Get all the elements of the report (summ info, case, analytical results)
	 * @return
	 */
	public Collection<TableRow> getAllRecords() {

		// children schemas
		TableSchema[] schemas = new TableSchema[] {
				TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET),
				TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET),
				TableSchemaList.getByName(CustomStrings.RESULT_SHEET)
		};

		return getRecords(schemas);
	}
	
	public Collection<TableRow> getRecords(TableSchema schema) {
		TableSchema[] schemas = new TableSchema[] {schema};
		return getRecords(schemas);
	}
	
	public Collection<TableRow> getRecords(TableSchema[] schemas) {
		
		Collection<TableRow> records = new ArrayList<>();
		
		// for each child schema get the rows related to the report
		for (TableSchema schema : schemas) {
			
			TableDao dao = new TableDao(schema);
			
			Collection<TableRow> children = dao.getByParentId(CustomStrings.REPORT_SHEET, this.getDatabaseId(), "desc");
			
			records.addAll(children);
		}

		return records;
	}
	
	/**
	 * Get the report summarized information
	 * @return
	 */
	public Collection<TseTableRow> getChildren() {
		
		Collection<TseTableRow> output = new ArrayList<>();
		
		TableSchema summInfoSchema = TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET);
		
		TableDao dao = new TableDao(summInfoSchema);
		Collection<TableRow> children = dao.getByParentId(CustomStrings.REPORT_SHEET, 
				this.getDatabaseId(), "desc");
		
		// create it as summarized info array
		for (TableRow child : children) {
			output.add(new SummarizedInfo(child));
		}
		
		return output;
	}
	
	/**
	 * Check if the report is empty or not (if has summarized info)
	 * @return
	 */
	public boolean isEmpty() {
		return getChildren().isEmpty();
	}
	
	/**
	 * Create a new version of the report and save it into the database.
	 * The version is automatically increased
	 * @return
	 */
	public EFSAReport amend() {
		
		Stack<TseTableRow> elements = new Stack<>();
		elements.add(this);

		SummarizedInfo summInfo = null;
		CaseReport caseReport = null;
		AnalyticalResult result = null;
		while (!elements.isEmpty()) {
			
			TseTableRow currentElement = elements.pop();
			
			// get the element children (before changing its id)
			Collection<TseTableRow> children = currentElement.getChildren();
			
			if (currentElement instanceof TseReport) {
				
				// get current version
				String currentVersion = this.getVersion();
				
				// increase version starting from the current
				String newVersion = TableVersion.createNewVersion(currentVersion);

				this.setVersion(newVersion);
				
				// new version is in draft
				this.setStatus(RCLDatasetStatus.DRAFT);
				
				this.setId("");
				this.setMessageId("");
			}
			else if (currentElement instanceof SummarizedInfo) {
				summInfo = (SummarizedInfo) currentElement;
				Relation.injectParent(this, summInfo);
			}
			else if (currentElement instanceof CaseReport) {
				caseReport = (CaseReport) currentElement;
				Relation.injectParent(this, caseReport);
				Relation.injectParent(summInfo, caseReport);
			}
			else if (currentElement instanceof AnalyticalResult) {
				result = (AnalyticalResult) currentElement;
				Relation.injectParent(this, result);
				Relation.injectParent(summInfo, result);
				Relation.injectParent(caseReport, result);
			}
			
			// save the current element (it changes the id)
			currentElement.save();
			
			// add the children
			elements.addAll(children);
		}
		
		return this;
	}
	
	/**
	 * Get all the report versions that are stored locally
	 */
	public ReportList getAllVersions() {

		ReportList allVersions = new ReportList();
		
		TableDao dao = new TableDao(this.getSchema());
		
		// get all the versions of the report by the dataset sender id
		Collection<TableRow> reports = dao.getByStringField(AppPaths.REPORT_SENDER_ID, 
				this.getSenderId());
		
		for (TableRow report : reports) {
			allVersions.add(new TseReport(report));
		}
		
		return allVersions;
	}

	@Override
	public EFSAReport getPreviousVersion() {
		
		String currentVersion = this.getVersion();
		
		ReportList allVersions = getAllVersions();

		// sort starting from the newest
		allVersions.sort();
		
		// search the previous version if present
		EFSAReport previousVersion = null;
		
		boolean isNext = false;
		for (EFSAReport report : allVersions) {

			if (isNext) {
				previousVersion = report;
				break;
			}
			
			if (report.getVersion().equals(currentVersion))
				isNext = true;
		}
		
		return previousVersion;
	}


	/**
	 * Check if the current report is already present in the database
	 * by using the senderDatasetId field
	 * @param schema
	 * @param currentReport
	 * @return
	 */
	public boolean isLocallyPresent() {
		return isLocallyPresent(this.getSenderId());
	}
	
	/**
	 * Check if the a report with the chosen senderDatasetId 
	 * is already present in the database
	 * @param senderDatasetId
	 * @return
	 */
	public static boolean isLocallyPresent(String senderDatasetId) {
		
		if (senderDatasetId == null)
			return false;
		
		// check if the report is already in the db
		TableDao dao = new TableDao(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		
		for (TableRow row : dao.getAll()) {

			TseReport report = new TseReport(row);
			String otherSenderDatasetId = report.getSenderId();
			
			// if same sender dataset id then return true
			if (otherSenderDatasetId != null 
					&& otherSenderDatasetId.equals(senderDatasetId))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Check if the entire report is valid
	 * @return
	 */
	public boolean isValid() {
		
		TableSchema childSchema = TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET);
		Collection<TableRow> children = this.getChildren(childSchema);
		
		// since the errors of inner children are propagated to the parents
		// it is sufficient to check if the first level children are correct
		SummarizedInfoValidator validator = new SummarizedInfoValidator();
		for (TableRow child : children) {
			
			int errorLevel = validator.getOverallWarningLevel(child);

			// if we have errors => not valid
			if (errorLevel > 0) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Get the message configuration for the current report
	 * the report will be exported in a temporary file
	 * @param opType
	 * @return
	 */
	public MessageConfigBuilder getDefaultExportConfiguration(OperationType opType) {
		return this.getDefaultExportConfiguration(opType, null);
	}
	
	/**
	 * get the message configuration for the current report
	 * the report will be exported in the specified {@code out} file
	 * @param opType
	 * @param out
	 * @return
	 */
	public MessageConfigBuilder getDefaultExportConfiguration(OperationType opType, File out) {
		
		Collection<TableRow> messageParents = new ArrayList<>();

		// add the report data
		messageParents.add(this);

		// add the settings data
		try {
			messageParents.add(Relation.getGlobalParent(CustomStrings.SETTINGS_SHEET));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		MessageConfigBuilder builder;
		if (out == null) {
			builder = new MessageConfigBuilder(messageParents, opType);
		}
		else {
			builder = new MessageConfigBuilder(messageParents, opType, out);
		}
		
		return builder;
	}
	
	/**
	 * Create the dataset (in DCF) as insert
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws SendMessageException
	 * @throws ReportException 
	 * @throws SOAPException
	 */
	public void create() throws IOException, 
		ParserConfigurationException, SAXException, SendMessageException, 
		MySOAPException, ReportException {
		
		OperationType op = OperationType.INSERT;
		this.exportAndSend(op);
	}
	
	/**
	 * Replace the dataset (in DCF)
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws SendMessageException
	 * @throws ReportException 
	 * @throws SOAPException
	 */
	public void replace() throws IOException, 
		ParserConfigurationException, SAXException, SendMessageException, 
		MySOAPException, ReportException {
		
		OperationType op = OperationType.REPLACE;
		this.exportAndSend(op);
	}
	
	/**
	 * Reject the dataset (in DCF)
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws SendMessageException
	 * @throws ReportException 
	 * @throws SOAPException
	 */
	public void reject() throws IOException, 
		ParserConfigurationException, SAXException, SendMessageException, 
		MySOAPException, ReportException {
		
		OperationType op = OperationType.REJECT;
		this.exportAndSend(op);
	}
	
	/**
	 * Submit the dataset
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws SendMessageException
	 * @throws MySOAPException
	 */
	public void submit() throws IOException, 
		ParserConfigurationException, SAXException, SendMessageException, 
		MySOAPException, ReportException {
		
		OperationType op = OperationType.SUBMIT;
		this.exportAndSend(op);
	}
	
	/**
	 * Create a report from a dataset
	 * @param dataset
	 * @return
	 */
	public static TseReport fromDataset(Dataset dataset) {
		
		TseReport report = new TseReport();
		
		String senderDatasetId = dataset.getOperation().getSenderDatasetId();
		
		report.setId(dataset.getId());
		
		String[] split = Dataset.splitSenderId(senderDatasetId);
		
		String senderId = senderDatasetId;
		String version = null;
		if (split != null && split.length > 1) {
			senderId = split[0];
			version = split[1];
			report.setVersion(version);
		}
		else {
			report.setVersion(TableVersion.getFirstVersion());
		}
		
		report.setSenderId(senderId);
		
		if (dataset.getRCLStatus() != null)
			report.setStatus(dataset.getRCLStatus());
		else
			report.setStatus(RCLDatasetStatus.DRAFT);
		
		// split FR1705... into country year and month
		if (senderId.length() < 6) {
			System.err.println("Report#fromDataset Cannot parse sender dataset id, expected at least 6 characters, found " 
					+ senderId);
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

		report.setMessageId("");
		
		// add the preferences
		try {
			Relation.injectGlobalParent(report, CustomStrings.PREFERENCES_SHEET);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return report;
	}
	
	public void setCountry(String country) {
		this.put(CustomStrings.REPORT_COUNTRY, 
				getTableColumnValue(country, CatalogLists.COUNTRY_LIST));
	}
	
	public String getCountry() {
		return this.getCode(CustomStrings.REPORT_COUNTRY);
	}
	
	@Override
	public String getRowIdFieldName() {
		return CustomStrings.RES_ID_COLUMN;
	}
	
	/**
	 * Check if the dataset can be edited or not
	 * @return
	 */
	public boolean isEditable() {
		return getRCLStatus().isEditable();
	}

	@Override
	public String getDecomposedSenderId() {
		return this.getSenderId();  // for report is already decomposed
	}

	@Override
	public void setStatus(DcfDatasetStatus status) {
		try {
			throw new UnsupportedOperationException("Cannot use this method");
		}
		catch(UnsupportedOperationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public DcfDatasetStatus getStatus() {
		
		try {
			throw new UnsupportedOperationException("Cannot use this method");
		}
		catch(UnsupportedOperationException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}

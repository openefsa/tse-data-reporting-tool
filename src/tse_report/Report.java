package tse_report;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;

import org.xml.sax.SAXException;

import acknowledge.Ack;
import acknowledge.AckDatasetStatus;
import app_config.AppPaths;
import app_config.DebugConfig;
import app_config.PropertiesReader;
import dataset.Dataset;
import dataset.DatasetList;
import dataset.DatasetStatus;
import global_utils.TimeUtils;
import message.MessageResponse;
import message.SendMessageException;
import message_creator.MessageCreator;
import message_creator.OperationType;
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
import webservice.GetAck;
import webservice.GetDatasetList;
import webservice.MySOAPException;
import webservice.SendMessage;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class Report extends TableRow implements TseTableRow {

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
			System.err.println("Report#fromDataset Cannot parse sender dataset id, expected 6 characters, found " 
					+ senderDatasetId);
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
	
	/**
	 * Get the version contained in the sender id
	 * @return
	 */
	public String getVersion() {
		
		return this.getCode(CustomStrings.REPORT_VERSION);
		/*String[] split = Dataset.splitSenderId(this.getSenderId());
		
		if (split == null || split.length < 2)
			return null;
		
		String version = split[1];
		
		return version;*/
	}
	
	/**
	 * Create a new version of the report and save it into the database
	 * the version is automatically increased
	 * @return
	 */
	public Report createNewVersion() {
		
		Stack<TseTableRow> elements = new Stack<>();
		elements.add(this);

		SummarizedInfo summInfo = null;
		CaseReport caseReport = null;
		AnalyticalResult result = null;
		while (!elements.isEmpty()) {
			
			TseTableRow currentElement = elements.pop();
			
			// get the element children (before changing its id)
			Collection<TseTableRow> children = currentElement.getChildren();
			
			if (currentElement instanceof Report) {
				
				// get current version
				String currentVersion = this.getVersion();
				
				// increase version starting from the current
				String newVersion = TableVersion.createNewVersion(currentVersion);
				
				// set sender id accordingly for the new version
				String newSenderId = TableVersion.mergeNameAndVersion(this.getDecomposedSenderId(), newVersion);
				this.setSenderId(newSenderId);
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
	 * Get the report summarized information
	 * @return
	 */
	public Collection<TseTableRow> getChildren() {
		
		Collection<TseTableRow> output = new ArrayList<>();
		
		TableSchema summInfoSchema = TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET);
		
		TableDao dao = new TableDao(summInfoSchema);
		Collection<TableRow> children = dao.getByParentId(CustomStrings.REPORT_SHEET, 
				this.getId(), "desc");
		
		// create it as summarized info array
		for (TableRow child : children) {
			output.add(new SummarizedInfo(child));
		}
		
		return output;
	}
	
	/**
	 * Get all the versions of the report
	 * @return
	 */
	public Collection<TableRow> getAllVersions() {
		
		TableDao dao = new TableDao(this.getSchema());
		
		Collection<TableRow> versions = dao.getByStringField(CustomStrings.REPORT_SENDER_ID, 
				this.getSenderId());
		
		return versions;
	}
	
	/**
	 * Delete all the versions of the report from the database
	 * @return
	 */
	public boolean deleteAllVersions() {
		
		TableDao dao = new TableDao(this.getSchema());
		
		return dao.deleteByStringField(CustomStrings.REPORT_SENDER_ID, 
				this.getSenderId());
	}

	/**
	 * Check if the current report is already present in the database
	 * by using the senderDatasetId field
	 * @param schema
	 * @param currentReport
	 * @return
	 */
	public boolean isLocallyPresent() {
		return isLocallyPresent(this.getDecomposedSenderId());
	}
	
	/**
	 * get the decomposed sender id
	 * @return
	 */
	private String getDecomposedSenderId() {
		
		String[] split = Dataset.splitSenderId(this.getSenderId());
		
		if (split == null || split.length < 1)
			return this.getSenderId();
		
		return split[0];
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
			
			Report report = new Report(row);
			
			String otherSenderDatasetId = report.getDecomposedSenderId();
			
			// if same sender dataset id then return true
			if (otherSenderDatasetId != null 
					&& otherSenderDatasetId.equals(senderDatasetId))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Get all the datasets in the DCF that have the as senderDatasetId
	 * the one given in input.
	 * @param report
	 * @return
	 * @throws SOAPException
	 * @throws ReportException 
	 */
	public DatasetList getDatasets() throws MySOAPException, ReportException {
		
		// check if the Report is in the DCF
		GetDatasetList request = new GetDatasetList(PropertiesReader.getDataCollectionCode());

		String senderDatasetId = this.getSenderId();
		
		if (senderDatasetId == null) {
			throw new ReportException("Cannot retrieve the report sender id for " + this);
		}
		
		DatasetList datasets = request.getList();
		return datasets.filterBySenderId(senderDatasetId);
	}
	
	/**
	 * Get the dataset related to this report (only metadata!). Note that only the newer one will
	 * be returned. If you need all the datasets related to this report use
	 * {@link #getDatasets()}.
	 * @return
	 * @throws MySOAPException
	 * @throws ReportException
	 */
	public Dataset getDataset() throws MySOAPException, ReportException {

		DatasetList datasets = getDatasets();
		if(datasets.isEmpty())
			return null;
		
		return datasets.get(0);
	}
	
	/**
	 * Get an acknowledgement of the dataset related to the report
	 * @return
	 * @throws SOAPException
	 */
	public Ack getAck() throws MySOAPException {

		// make get ack request
		String messageId = this.getMessageId();
		
		// if no message id => the report was never sent
		if (messageId.isEmpty()) {
			return null;
		}
		
		GetAck req = new GetAck(messageId);
		
		// get state
		Ack ack = req.getAck();

		// if we have something in the ack
		if (ack.isReady()) {

			// save id
			String datasetId = ack.getLog().getDatasetId();
			this.setDatasetId(datasetId);
			
			// save status
			AckDatasetStatus status = ack.getLog().getDatasetStatus();
			this.setDatasetStatus(status);
			
			// permanently save data
			this.update();
			
			System.out.println("Ack successful for message id " + messageId + ". Retrieved datasetId=" 
					+ datasetId + " with status=" + this.getDatasetStatus());
		}
		
		return ack;
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
	 * Export the report and send it to the DCF
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws SendMessageException
	 * @throws SOAPException
	 */
	public void exportAndSend(OperationType opType) throws IOException, ParserConfigurationException, 
		SAXException, SendMessageException, MySOAPException {
		
		// set report filename
		String filename = "report-" + TimeUtils.getTodayTimestamp() + ".xml";
		
		File tempFile = new File(AppPaths.TEMP_FOLDER + filename);

		// export the report and get an handle to the exported file
		File file = this.export(tempFile, opType);

		try {
			
			this.send(file, opType);
			
			// delete file also if exception occurs
			if (!DebugConfig.debug)
				file.delete();
		}
		catch (SOAPException e) {

			// delete file also if exception occurs
			if (!DebugConfig.debug)
				file.delete();

			throw new MySOAPException(e);
		}
	}
	
	/**
	 * Export a report into a file
	 * @param report
	 * @param filename
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public File export(File file, OperationType opType)
			throws IOException, ParserConfigurationException, SAXException {

		// instantiate xml creator and inject the required parents
		// of the configuration table (in order to be able to
		// retrieve the data for the message header)
		MessageCreator creator = new MessageCreator(file) {

			@Override
			public Collection<TableRow> getConfigMessageParents() {

				Collection<TableRow> parents = new ArrayList<>();

				// add the report data
				parents.add(Report.this);

				// add the settings data
				try {
					parents.add(Relation.getGlobalParent(CustomStrings.SETTINGS_SHEET));
				} catch (IOException e) {
					e.printStackTrace();
				}

				return parents;
			}
		};

		// set the correct operation type required
		creator.setOpType(opType);
		
		// export the report
		if (opType.needEmptyDataset())
			creator.exportEmpty();
		else
			creator.export(this);
		
		return file;
	}
	
	
	/**
	 * Reject the dataset (in DCF)
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws SendMessageException
	 * @throws SOAPException
	 */
	public void reject() throws IOException, 
		ParserConfigurationException, SAXException, SendMessageException, MySOAPException {
		this.exportAndSend(OperationType.REJECT);
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
		ParserConfigurationException, SAXException, SendMessageException, MySOAPException {
		this.exportAndSend(OperationType.SUBMIT);
}
	
	/**
	 * Send the report contained in the file
	 * and update the report status accordingly.
	 * NOTE only for expert users. Otherwise use
	 * {@link #exportAndSend()} to send the report
	 * with an atomic operation.
	 * @param file
	 * @throws SOAPException
	 * @throws SendMessageException
	 */
	private void send(File file, OperationType opType) throws SOAPException, SendMessageException {

		// send the report and get the response to the message
		SendMessage req = new SendMessage(file);
		MessageResponse response = req.send();

		// if correct response then save the message id
		// into the report
		if (response.isCorrect()) {

			// save the message id
			this.setMessageId(response.getMessageId());
			
			// update report status based on the request operation type
			DatasetStatus newStatus;
			switch(opType) {
			case INSERT:
			case REPLACE:
				newStatus = DatasetStatus.UPLOADED;
				break;
			case REJECT:
				newStatus = DatasetStatus.REJECTION_SENT;
				break;
			case SUBMIT:
				newStatus = DatasetStatus.SUBMISSION_SENT;
				break;
			default:
				newStatus = null;
				break;
			}
			
			if (newStatus != null) {
				this.setDatasetStatus(newStatus);
				this.update();
			}
		}
		else {

			// set upload failed status if message is not valid
			this.setDatasetStatus(DatasetStatus.UPLOAD_FAILED);
			this.update();

			throw new SendMessageException(response);
		}
	}
	
	
	/**
	 * Given a report and its state, get the operation
	 * that is correct for sending it to the dcf.
	 * For example, if the report was never sent then the operation
	 * will be {@link OperationType#INSERT}.
	 * @param report
	 * @return
	 * @throws ReportException 
	 * @throws MySOAPException 
	 */
	public ReportSendOperation getSendOperation() throws MySOAPException, ReportException {
		
		OperationType opType = OperationType.NOT_SUPPORTED;
		
		Dataset dataset = this.getDataset();
		
		// if no dataset is present => we do an insert
		if (dataset == null)
			return new ReportSendOperation(null, OperationType.INSERT);
		
		// otherwise we check the dataset status
		DatasetStatus status = dataset.getStatus();
		
		switch (status) {
		case REJECTED_EDITABLE:
		case VALID:
		case VALID_WITH_WARNINGS:
			opType = OperationType.REPLACE;
			break;
		case DELETED:
			opType = OperationType.INSERT;
			break;
		default:
			opType = OperationType.NOT_SUPPORTED;
			break;
		}
		
		ReportSendOperation operation = new ReportSendOperation(dataset, opType);
		
		return operation;
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
	
	public void setDatasetStatus(DatasetStatus status) {
		this.put(CustomStrings.REPORT_STATUS, status.getStatus());
	}
	
	/**
	 * Mapping between dataset status and ack dataset status
	 * @param status
	 */
	public void setDatasetStatus(AckDatasetStatus status) {
		
		DatasetStatus dataStatus = DatasetStatus.OTHER;
		switch(status) {
		case ACCEPTED_DWH:
			dataStatus = DatasetStatus.ACCEPTED_DWH; break;
		case DELETED:
			dataStatus = DatasetStatus.DELETED; break;
		case PROCESSING:
			dataStatus = DatasetStatus.PROCESSING; break;
		case REJECTED:
			dataStatus = DatasetStatus.REJECTED; break;
		case REJECTED_EDITABLE:
			dataStatus = DatasetStatus.REJECTED_EDITABLE; break;
		case SUBMITTED:
			dataStatus = DatasetStatus.SUBMITTED; break;
		case VALID:
			dataStatus = DatasetStatus.VALID; break;
		case VALID_WITH_WARNINGS:
			dataStatus = DatasetStatus.VALID_WITH_WARNINGS; break;
		case OTHER:
			dataStatus = DatasetStatus.OTHER; break;
		}
		
		this.put(CustomStrings.REPORT_STATUS, dataStatus.getStatus());
	}
	
	/**
	 * Force the report to be editable
	 */
	public void makeEditable() {
		this.put(CustomStrings.REPORT_STATUS, DatasetStatus.DRAFT.getStatus());
	}
	
	/**
	 * Check if the dataset can be edited or not
	 * @return
	 */
	public boolean isEditable() {
		return getDatasetStatus().isEditable();
	}
}

package report_service_test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import ack.DcfAck;
import ack.FileState;
import ack.OkCode;
import app_config.AppPaths;
import dataset.Dataset;
import dataset.DatasetList;
import dataset.DcfDatasetStatus;
import dataset.Header;
import dataset.IDataset;
import dataset.Operation;
import dataset.RCLDatasetStatus;
import dcf_log.DcfAckLogMock;
import global_utils.Message;
import message.MessageConfigBuilder;
import message.MessageResponse;
import message.SendMessageException;
import message.TrxCode;
import message_creator.OperationType;
import mocks.RowCreatorMock;
import mocks.TableDaoMock;
import providers.FormulaService;
import providers.IFormulaService;
import providers.ITableDaoService;
import providers.RCLError;
import providers.TableDaoService;
import providers.TseReportService;
import report.Report;
import report.ReportException;
import report.ReportSendOperation;
import soap.DetailedSOAPException;
import soap_test.GetAckMock;
import soap_test.GetDatasetsListMock;
import soap_test.SendMessageMock;
import table_skeleton.TableRow;
import table_skeleton.TableRowList;
import table_skeleton.TableVersion;
import tse_analytical_result.AnalyticalResult;
import tse_case_report.CaseReport;
import tse_config.CustomStrings;
import tse_report.TseReport;
import tse_summarized_information.SummarizedInfo;
import xlsx_reader.TableSchemaList;

public class ReportServiceTest {

	private TseReportService reportService;
	private GetAckMock getAck;
	private GetDatasetsListMock<IDataset> getDatasetsList;
	private SendMessageMock sendMessage;
	private ITableDaoService daoService;
	private IFormulaService formulaService;
	private Report report;
	
	@Before
	public void init() {
		this.getAck = new GetAckMock();
		this.getDatasetsList = new GetDatasetsListMock<>();
		this.sendMessage = new SendMessageMock();
		this.daoService = new TableDaoService(new TableDaoMock());
		
		this.formulaService = new FormulaService(daoService);
		
		this.reportService = new TseReportService(getAck, getDatasetsList, sendMessage, 
				daoService, formulaService);
		
		report = new TseReport();
		report.setId("11234");
		report.setMessageId("25232");
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2017");
		report.setStatus(RCLDatasetStatus.DRAFT);
	}
	
	/**
	 * Perform a refresh status
	 * @param localStatus status of the local report
	 * @param dcfStatus status of the report in dcf
	 * @param ackStatus status of the retrieved ack
	 * @return
	 */
	private Message refreshStatusWithReadyAck(RCLDatasetStatus localStatus, 
			DcfDatasetStatus dcfStatus, DcfDatasetStatus ackStatus) {
		
		DcfAck ack = new DcfAck(FileState.READY, new DcfAckLogMock(OkCode.OK, ackStatus));
		getAck.setAck(ack);

		Dataset dcfReport = new Dataset();
		dcfReport.setHeader(new Header("", "", "", "", ""));
		dcfReport.setOperation(new Operation("", "11234", "AT0404", "TSE.TEST", "SSD2_CENTRAL", "EFSA", ""));
		dcfReport.setId("11234");
		dcfReport.setSenderId("AT0404");
		dcfReport.setStatus(dcfStatus);
		
		DatasetList list = new DatasetList();
		list.add(dcfReport);
		
		report.setStatus(localStatus);
		
		getDatasetsList.setList(list);
		
		Message m = reportService.refreshStatus(report);
		
		return m;
	}
	
	
	@Test
	public void getDatasets() throws DetailedSOAPException {
		
		Dataset r1 = new Dataset();
		r1.setId("11234");
		r1.setSenderId("AT0404.00");
		r1.setStatus(DcfDatasetStatus.REJECTED);
		
		Dataset r2 = new Dataset();
		r2.setId("49201");
		r2.setSenderId("FR0404.00");
		r2.setStatus(DcfDatasetStatus.VALID);
		
		Dataset r3 = new Dataset();
		r3.setId("42842");
		r3.setSenderId("AT0404.01");
		r3.setStatus(DcfDatasetStatus.VALID);
		
		DatasetList list = new DatasetList();
		list.add(r1);
		list.add(r2);
		list.add(r3);
		
		getDatasetsList.setList(list);
		
		DatasetList out = reportService.getDatasetsOf("AT0404", "2017");
		
		// two versions
		assertEquals(2, out.size());
	}
	
	@Test
	public void getDatasetById() throws DetailedSOAPException {
		
		Dataset r1 = new Dataset();
		r1.setId("11234");
		r1.setSenderId("AT0404.00");
		r1.setStatus(DcfDatasetStatus.REJECTED);
		
		Dataset r2 = new Dataset();
		r2.setId("49201");
		r2.setSenderId("FR0404.00");
		r2.setStatus(DcfDatasetStatus.VALID);
		
		Dataset r3 = new Dataset();
		r3.setId("42842");
		r3.setSenderId("AT0404.01");
		r3.setStatus(DcfDatasetStatus.VALID);
		
		DatasetList list = new DatasetList();
		list.add(r1);
		list.add(r2);
		list.add(r3);
		
		getDatasetsList.setList(list);
		
		Dataset data = reportService.getDatasetById("AT0404", "2017", "11234");
		
		assertEquals(r1.getId(), data.getId());
		assertEquals(r1.getSenderId(), data.getSenderId());
	}
	
	@Test
	public void getLatestDatasetByVersion() throws DetailedSOAPException {
		
		Dataset r1 = new Dataset();
		r1.setId("11234");
		r1.setSenderId("AT0404.00");
		r1.setStatus(DcfDatasetStatus.REJECTED);
		
		Dataset r2 = new Dataset();
		r2.setId("49201");
		r2.setSenderId("FR0404.00");
		r2.setStatus(DcfDatasetStatus.VALID);
		
		Dataset r3 = new Dataset();
		r3.setId("42842");
		r3.setSenderId("AT0404.01");
		r3.setStatus(DcfDatasetStatus.VALID);
		
		DatasetList list = new DatasetList();
		list.add(r1);
		list.add(r2);
		list.add(r3);
		
		getDatasetsList.setList(list);
		
		Dataset data = reportService.getLatestDataset("AT0404", "2017");
		
		assertEquals(r3.getId(), data.getId());
		assertEquals(r3.getSenderId(), data.getSenderId());
	}
	
	@Test
	public void getLatestDatasetUsingVersion() throws DetailedSOAPException {
		
		Dataset r1 = new Dataset();
		r1.setId("11234");
		r1.setSenderId("AT0404.00");
		r1.setStatus(DcfDatasetStatus.REJECTED);
		
		Dataset r2 = new Dataset();
		r2.setId("49201");
		r2.setSenderId("FR0404.00");
		r2.setStatus(DcfDatasetStatus.VALID);
		
		Dataset r3 = new Dataset();
		r3.setId("42842");
		r3.setSenderId("AT0404.01");
		r3.setStatus(DcfDatasetStatus.VALID);
		
		DatasetList list = new DatasetList();
		list.add(r1);
		list.add(r2);
		list.add(r3);

		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2017");
		
		getDatasetsList.setList(list);
		
		Dataset data = reportService.getLatestDataset(report);
		
		assertEquals(r3.getId(), data.getId());
		assertEquals(r3.getSenderId(), data.getSenderId());
	}
	
	@Test
	public void getLatestDatasetUsingId() throws DetailedSOAPException {
		
		Dataset r1 = new Dataset();
		r1.setId("11234");
		r1.setSenderId("AT0404.00");
		r1.setStatus(DcfDatasetStatus.REJECTED);
		
		Dataset r2 = new Dataset();
		r2.setId("49201");
		r2.setSenderId("FR0404.00");
		r2.setStatus(DcfDatasetStatus.VALID);
		
		Dataset r3 = new Dataset();
		r3.setId("42842");
		r3.setSenderId("AT0404.01");
		r3.setStatus(DcfDatasetStatus.VALID);
		
		DatasetList list = new DatasetList();
		list.add(r1);
		list.add(r2);
		list.add(r3);

		getDatasetsList.setList(list);
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setId("42842");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2017");
		
		Dataset data = reportService.getLatestDataset(report);
		
		assertEquals(r3.getId(), data.getId());
		assertEquals(r3.getSenderId(), data.getSenderId());
	}
	
	@Test
	public void getSendOperationFirstSend() throws DetailedSOAPException, ReportException {
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2017");
		
		Dataset dataset = reportService.getLatestDataset(report);
		ReportSendOperation op = reportService.getSendOperation(report, dataset);
		assertEquals(OperationType.INSERT, op.getOpType());
	}
	
	@Test
	public void getSendOperationSecondSend() throws DetailedSOAPException, ReportException {
		
		Dataset r1 = new Dataset();
		r1.setId("11234");
		r1.setSenderId("AT0404.00");
		r1.setStatus(DcfDatasetStatus.VALID);
		
		DatasetList list = new DatasetList();
		list.add(r1);

		getDatasetsList.setList(list);
		
		// it should replace the report
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setId("11234");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2017");
		
		Dataset dataset = reportService.getLatestDataset(report);
		ReportSendOperation op = reportService.getSendOperation(report, dataset);
		assertEquals(OperationType.REPLACE, op.getOpType());
	}
	
	@Test
	public void getSendOperationSendAmendmended() throws DetailedSOAPException, ReportException {
		
		Dataset r1 = new Dataset();
		r1.setId("11234");
		r1.setSenderId("AT0404.00");
		r1.setStatus(DcfDatasetStatus.ACCEPTED_DWH);
		
		DatasetList list = new DatasetList();
		list.add(r1);

		getDatasetsList.setList(list);
		
		// it should insert a new report
		// with the new version
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion("01");
		report.setYear("2017");
		
		Dataset dataset = reportService.getLatestDataset(report);
		ReportSendOperation op = reportService.getSendOperation(report, dataset);
		assertEquals(OperationType.INSERT, op.getOpType());
	}
	
	@Test
	public void getSendOperationSendOverRejected() throws DetailedSOAPException, ReportException {
		
		Dataset r1 = new Dataset();
		r1.setId("11234");
		r1.setSenderId("AT0404.00");
		r1.setStatus(DcfDatasetStatus.REJECTED);
		
		DatasetList list = new DatasetList();
		list.add(r1);

		getDatasetsList.setList(list);
		
		// it should insert a new report
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion("00");
		report.setYear("2017");
		
		Dataset dataset = reportService.getLatestDataset(report);
		ReportSendOperation op = reportService.getSendOperation(report, dataset);
		assertEquals(OperationType.REPLACE, op.getOpType());
	}
	
	@Test
	public void getSendOperationSendOverDeleted() throws DetailedSOAPException, ReportException {
		
		Dataset r1 = new Dataset();
		r1.setId("11234");
		r1.setSenderId("AT0404.00");
		r1.setStatus(DcfDatasetStatus.DELETED);
		
		DatasetList list = new DatasetList();
		list.add(r1);

		getDatasetsList.setList(list);
		
		// it should insert a new report
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion("00");
		report.setYear("2017");
		
		Dataset dataset = reportService.getLatestDataset(report);
		ReportSendOperation op = reportService.getSendOperation(report, dataset);
		assertEquals(OperationType.INSERT, op.getOpType());
	}
	
	@Test
	public void getSendOperationSendOverAcceptedDwh() throws DetailedSOAPException, ReportException {
		
		Dataset r1 = new Dataset();
		r1.setId("11234");
		r1.setSenderId("AT0404.00");
		r1.setStatus(DcfDatasetStatus.ACCEPTED_DWH);
		
		DatasetList list = new DatasetList();
		list.add(r1);

		getDatasetsList.setList(list);
		
		// it should insert a new report
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion("00");
		report.setYear("2017");
		
		Dataset dataset = reportService.getLatestDataset(report);
		ReportSendOperation op = reportService.getSendOperation(report, dataset);
		assertEquals(OperationType.NOT_SUPPORTED, op.getOpType());
	}
	
	@Test
	public void getSendOperationSendOverAcceptedDcf() throws DetailedSOAPException, ReportException {
		
		Dataset r1 = new Dataset();
		r1.setId("11234");
		r1.setSenderId("AT0404.00");
		r1.setStatus(DcfDatasetStatus.OTHER);
		
		DatasetList list = new DatasetList();
		list.add(r1);

		getDatasetsList.setList(list);
		
		// it should insert a new report
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion("00");
		report.setYear("2017");
		
		Dataset dataset = reportService.getLatestDataset(report);
		ReportSendOperation op = reportService.getSendOperation(report, dataset);
		assertEquals(OperationType.NOT_SUPPORTED, op.getOpType());
	}
	
	@Test
	public void getSendOperationSendOverRejEd() throws DetailedSOAPException, ReportException {
		
		Dataset r1 = new Dataset();
		r1.setId("11234");
		r1.setSenderId("AT0404.00");
		r1.setStatus(DcfDatasetStatus.REJECTED_EDITABLE);
		
		DatasetList list = new DatasetList();
		list.add(r1);

		getDatasetsList.setList(list);
		
		// it should insert a new report
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion("00");
		report.setYear("2017");
		
		Dataset dataset = reportService.getLatestDataset(report);
		ReportSendOperation op = reportService.getSendOperation(report, dataset);
		assertEquals(OperationType.REPLACE, op.getOpType());
	}
	
	@Test
	public void refreshStatusWithKOAck() {
		
		DcfAck ack = new DcfAck(FileState.READY, new DcfAckLogMock(OkCode.KO, null));
		getAck.setAck(ack);
		
		RCLDatasetStatus localStatus = RCLDatasetStatus.UPLOADED; // status which uses the ack
		
		report.setStatus(localStatus);

		Message m = reportService.refreshStatus(report);
		
		assertEquals(localStatus, report.getRCLStatus()); // not changed
		assertEquals("ERR806", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsUploadedAckFail() {
		DcfAck ack = new DcfAck(FileState.FAIL, null);
		getAck.setAck(ack);
		
		report.setStatus(RCLDatasetStatus.UPLOADED);
		Message m = reportService.refreshStatus(report);
		
		// processing message
		assertEquals("WARN500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsUploadedAckIsInProcessing() {
		DcfAck ack = new DcfAck(FileState.WAIT, null);
		getAck.setAck(ack);
		
		report.setStatus(RCLDatasetStatus.UPLOADED);
		Message m = reportService.refreshStatus(report);
		
		// processing message
		assertEquals("WARN500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsRejectionSentAckIsInProcessing() {
		DcfAck ack = new DcfAck(FileState.WAIT, null);
		getAck.setAck(ack);
		
		report.setStatus(RCLDatasetStatus.REJECTION_SENT);
		Message m = reportService.refreshStatus(report);
		
		// processing message
		assertEquals("WARN500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsSubmissionSentAckIsInProcessing() {
		DcfAck ack = new DcfAck(FileState.WAIT, null);
		getAck.setAck(ack);
		
		report.setStatus(RCLDatasetStatus.SUBMISSION_SENT);
		Message m = reportService.refreshStatus(report);
		
		// processing message
		assertEquals("WARN500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsUploadedAckValidListValid() {
		
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.UPLOADED, 
				DcfDatasetStatus.VALID, DcfDatasetStatus.VALID);
	
		assertEquals(RCLDatasetStatus.VALID, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsUploadedAckValidWithWarningsListValidWithWarnings() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.UPLOADED, 
				DcfDatasetStatus.VALID_WITH_WARNINGS, DcfDatasetStatus.VALID_WITH_WARNINGS);

		assertEquals(RCLDatasetStatus.VALID_WITH_WARNINGS, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsUploadedAckRejEdListRejEd() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.UPLOADED, 
				DcfDatasetStatus.REJECTED_EDITABLE, DcfDatasetStatus.REJECTED_EDITABLE);

		assertEquals(RCLDatasetStatus.REJECTED_EDITABLE, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsUploadedAckRejListRej() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.UPLOADED, 
				DcfDatasetStatus.REJECTED, DcfDatasetStatus.REJECTED);

		assertEquals(RCLDatasetStatus.REJECTED, report.getRCLStatus());
		assertEquals("ERR804", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsUploadedAckDelListDel() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.UPLOADED, 
				DcfDatasetStatus.DELETED, DcfDatasetStatus.DELETED);

		assertEquals(RCLDatasetStatus.DELETED, report.getRCLStatus());
		assertEquals("ERR804", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsUploadedAckRejListDel() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.UPLOADED, 
				DcfDatasetStatus.DELETED, DcfDatasetStatus.REJECTED);
		
		assertEquals(RCLDatasetStatus.REJECTED, report.getRCLStatus());
		assertEquals("ERR804", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsUploadedAckSubmittedListRejEd() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.UPLOADED, 
				DcfDatasetStatus.REJECTED_EDITABLE, DcfDatasetStatus.SUBMITTED);
		
		assertEquals(RCLDatasetStatus.REJECTED_EDITABLE, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsSubmissionSentAckSubmittedListRejEd() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.SUBMISSION_SENT, 
				DcfDatasetStatus.REJECTED_EDITABLE, DcfDatasetStatus.SUBMITTED);

		assertEquals(RCLDatasetStatus.REJECTED_EDITABLE, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsRejectionSentAckSubmittedListRejEd() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.REJECTION_SENT, 
				DcfDatasetStatus.REJECTED_EDITABLE, DcfDatasetStatus.SUBMITTED);
		
		assertEquals(RCLDatasetStatus.REJECTED_EDITABLE, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsUploadedAckSubmittedListAccDwh() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.UPLOADED, 
				DcfDatasetStatus.ACCEPTED_DWH, DcfDatasetStatus.SUBMITTED);

		assertEquals(RCLDatasetStatus.ACCEPTED_DWH, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsSubmissionSentAckSubmittedListAccDwh() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.SUBMISSION_SENT, 
				DcfDatasetStatus.ACCEPTED_DWH, DcfDatasetStatus.SUBMITTED);
		
		assertEquals(RCLDatasetStatus.ACCEPTED_DWH, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsRejectionSentAckSubmittedListAccDwh() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.REJECTION_SENT, 
				DcfDatasetStatus.ACCEPTED_DWH, DcfDatasetStatus.SUBMITTED);
		
		assertEquals(RCLDatasetStatus.ACCEPTED_DWH, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsUploadedAckValidListRejEd() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.UPLOADED, 
				DcfDatasetStatus.REJECTED_EDITABLE, DcfDatasetStatus.VALID);
		
		assertEquals(RCLDatasetStatus.UPLOADED, report.getRCLStatus());
		assertEquals("ERR501", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsUploadedAckValidListSubmitted() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.UPLOADED, 
				DcfDatasetStatus.OTHER, DcfDatasetStatus.SUBMITTED);
		
		assertEquals(RCLDatasetStatus.UPLOADED, report.getRCLStatus());
		assertEquals("ERR501", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsValidListValid() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.VALID, 
				DcfDatasetStatus.VALID, DcfDatasetStatus.ACCEPTED_DWH);  // ack is ignored
		
		assertEquals(RCLDatasetStatus.VALID, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsValidListDeleted() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.VALID, 
				DcfDatasetStatus.DELETED, DcfDatasetStatus.ACCEPTED_DWH);  // ack is ignored
		
		assertEquals(RCLDatasetStatus.DRAFT, report.getRCLStatus());
		assertEquals("WARN501", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsSubmittedListRejEdt() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.SUBMITTED, 
				DcfDatasetStatus.REJECTED_EDITABLE, DcfDatasetStatus.ACCEPTED_DWH);  // ack is ignored
		
		assertEquals(RCLDatasetStatus.REJECTED_EDITABLE, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsSubmittedListAccDwh() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.SUBMITTED, 
				DcfDatasetStatus.ACCEPTED_DWH, DcfDatasetStatus.ACCEPTED_DWH);  // ack is ignored
		
		assertEquals(RCLDatasetStatus.ACCEPTED_DWH, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsValidListRejEd() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.VALID, 
				DcfDatasetStatus.REJECTED_EDITABLE, DcfDatasetStatus.ACCEPTED_DWH);  // ack is ignored
		
		assertEquals(RCLDatasetStatus.VALID, report.getRCLStatus());
		assertEquals("ERR501", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsSubmittedListValid() {
		Message m = refreshStatusWithReadyAck(RCLDatasetStatus.SUBMITTED, 
				DcfDatasetStatus.VALID, DcfDatasetStatus.ACCEPTED_DWH);  // ack is ignored
		
		assertEquals(RCLDatasetStatus.SUBMITTED, report.getRCLStatus());
		assertEquals("ERR501", m.getCode());
	}
	
	
	@Test
	public void refreshStatusNullAck() {
		DcfAck ack = new DcfAck(FileState.WAIT, null);
		getAck.setAck(ack);
		
		report.setStatus(RCLDatasetStatus.REJECTION_SENT);
		Message m = reportService.refreshStatus(report);
		
		// processing message
		assertEquals("WARN500", m.getCode());
	}
	
	@Test
	public void displayAckNoMessageId() {
		Message m = reportService.displayAck(null);
		assertEquals("ERR800", m.getCode());
	}
	
	@Test
	public void displayAckNoAck() {
		Message m = reportService.displayAck("31322");
		assertEquals("ERR803", m.getCode());
	}
	
	@Test
	public void displayAckNotReady() {
		DcfAck ack = new DcfAck(FileState.WAIT, null);
		getAck.setAck(ack);
		
		Message m = reportService.displayAck("31322");
		assertEquals("ERR803", m.getCode());
	}
	
	@Test
	public void displayAckReadyNoLog() {
		DcfAck ack = new DcfAck(FileState.READY, null);
		getAck.setAck(ack);
		
		Message m = reportService.displayAck("31322");
		assertEquals("ERR803", m.getCode());
	}
	
	@Test
	public void displayAckReadyLog() {
		DcfAck ack = new DcfAck(FileState.READY, new DcfAckLogMock(OkCode.OK, DcfDatasetStatus.REJECTED));
		getAck.setAck(ack);
		
		Message m = reportService.displayAck("31322");
		assertNull(m);
	}
	
	@Test
	public void createNotExistingReportNotExistingInDcf() throws DetailedSOAPException {
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2004");
		report.setMonth("04");
		report.setCountry("AT");
		report.setSchema(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		report.setStatus(RCLDatasetStatus.DRAFT);
		RCLError error = reportService.create(report);
		
		assertNull(error);
		assertEquals(1, daoService.getAll(report.getSchema()).size());
	}
	
	@Test
	public void createExistingReportNotExistingInDcf() throws DetailedSOAPException {
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2004");
		report.setMonth("04");
		report.setCountry("AT");
		report.setSchema(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		report.setStatus(RCLDatasetStatus.DRAFT);
		
		// add it
		daoService.add(report);
		
		RCLError error = reportService.create(report);
		
		assertNotNull(error);
		assertEquals("WARN304", error.getCode());
		assertEquals(1, daoService.getAll(report.getSchema()).size());
	}
	
	@Test
	public void createExistingReportExistingAlsoInDcf() throws DetailedSOAPException {
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2004");
		report.setMonth("04");
		report.setCountry("AT");
		report.setSchema(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		report.setStatus(RCLDatasetStatus.DRAFT);
		
		// add it
		daoService.add(report);
		
		Dataset d = new Dataset();
		d.setId("42842");
		d.setSenderId("AT0404.00");
		d.setStatus(DcfDatasetStatus.VALID);
		
		DatasetList list = new DatasetList();
		list.add(d);
		
		getDatasetsList.setList(list);
		
		RCLError error = reportService.create(report);
		
		assertNotNull(error);
		assertEquals("WARN304", error.getCode());
		assertEquals(1, daoService.getAll(report.getSchema()).size());
	}
	
	@Test
	public void createNotExistingReportExistingInDcfAsValid() throws DetailedSOAPException {
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2004");
		report.setMonth("04");
		report.setCountry("AT");
		report.setSchema(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		report.setStatus(RCLDatasetStatus.DRAFT);
		
		Dataset d = new Dataset();
		d.setId("42842");
		d.setSenderId("AT0404.00");
		d.setStatus(DcfDatasetStatus.VALID);
		
		DatasetList list = new DatasetList();
		list.add(d);
		
		getDatasetsList.setList(list);
		
		RCLError error = reportService.create(report);
		
		assertNotNull(error);
		assertEquals("WARN303", error.getCode());
		assertEquals(0, daoService.getAll(report.getSchema()).size());
	}
	
	@Test
	public void createNotExistingReportExistingInDcfAsRejectedEditable() throws DetailedSOAPException {
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2004");
		report.setMonth("04");
		report.setCountry("AT");
		report.setSchema(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		report.setStatus(RCLDatasetStatus.DRAFT);
		
		Dataset d = new Dataset();
		d.setId("42842");
		d.setSenderId("AT0404.00");
		d.setStatus(DcfDatasetStatus.REJECTED_EDITABLE);
		
		DatasetList list = new DatasetList();
		list.add(d);
		
		getDatasetsList.setList(list);
		
		RCLError error = reportService.create(report);
		
		assertNotNull(error);
		assertEquals("WARN303", error.getCode());
		assertEquals(0, daoService.getAll(report.getSchema()).size());
	}
	
	@Test
	public void createNotExistingReportExistingInDcfAsValidWithWarnings() throws DetailedSOAPException {
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2004");
		report.setMonth("04");
		report.setCountry("AT");
		report.setSchema(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		report.setStatus(RCLDatasetStatus.DRAFT);
		
		Dataset d = new Dataset();
		d.setId("42842");
		d.setSenderId("AT0404.00");
		d.setStatus(DcfDatasetStatus.VALID_WITH_WARNINGS);
		
		DatasetList list = new DatasetList();
		list.add(d);
		
		getDatasetsList.setList(list);
		
		RCLError error = reportService.create(report);
		
		assertNotNull(error);
		assertEquals("WARN303", error.getCode());
		assertEquals(0, daoService.getAll(report.getSchema()).size());
	}
	
	@Test
	public void createNotExistingReportExistingInDcfAsDeleted() throws DetailedSOAPException {
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2004");
		report.setMonth("04");
		report.setCountry("AT");
		report.setSchema(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		report.setStatus(RCLDatasetStatus.DRAFT);
		
		Dataset d = new Dataset();
		d.setId("42842");
		d.setSenderId("AT0404.00");
		d.setStatus(DcfDatasetStatus.DELETED);
		
		DatasetList list = new DatasetList();
		list.add(d);
		
		getDatasetsList.setList(list);
		
		RCLError error = reportService.create(report);
		
		assertNull(error);
		assertEquals(1, daoService.getAll(report.getSchema()).size());
		
		TableRow r = daoService.getAll(report.getSchema()).iterator().next();
		assertNotNull(r);
		
		// the id should NOT be saved
		assertNull(r.get(AppPaths.REPORT_DATASET_ID));
	}
	
	@Test
	public void createNotExistingReportExistingInDcfAsRejected() throws DetailedSOAPException {
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2004");
		report.setMonth("04");
		report.setCountry("AT");
		report.setSchema(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		report.setStatus(RCLDatasetStatus.DRAFT);
		
		Dataset d = new Dataset();
		d.setId("42842");
		d.setSenderId("AT0404.00");
		d.setStatus(DcfDatasetStatus.REJECTED);
		
		DatasetList list = new DatasetList();
		list.add(d);
		
		getDatasetsList.setList(list);
		
		RCLError error = reportService.create(report);
		
		assertNull(error);
		assertEquals(1, daoService.getAll(report.getSchema()).size());
		
		TableRow r = daoService.getAll(report.getSchema()).iterator().next();
		assertNotNull(r);
		
		// the id should be saved
		assertEquals(d.getId(), r.getCode(AppPaths.REPORT_DATASET_ID));
	}
	
	@Test
	public void createNotExistingReportExistingInDcfAsSubmitted() throws DetailedSOAPException {
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2004");
		report.setMonth("04");
		report.setCountry("AT");
		report.setSchema(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		report.setStatus(RCLDatasetStatus.DRAFT);
		
		Dataset d = new Dataset();
		d.setId("42842");
		d.setSenderId("AT0404.00");
		d.setStatus(DcfDatasetStatus.SUBMITTED);
		
		DatasetList list = new DatasetList();
		list.add(d);
		
		getDatasetsList.setList(list);
		
		RCLError error = reportService.create(report);
		
		assertNotNull(error);
		assertEquals("WARN302", error.getCode());
		assertEquals(0, daoService.getAll(report.getSchema()).size());
	}
	
	@Test
	public void createNotExistingReportExistingInDcfAsAcceptedDwh() throws DetailedSOAPException {
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2004");
		report.setMonth("04");
		report.setCountry("AT");
		report.setSchema(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		report.setStatus(RCLDatasetStatus.DRAFT);
		
		Dataset d = new Dataset();
		d.setId("42842");
		d.setSenderId("AT0404.00");
		d.setStatus(DcfDatasetStatus.ACCEPTED_DWH);
		
		DatasetList list = new DatasetList();
		list.add(d);
		
		getDatasetsList.setList(list);
		
		RCLError error = reportService.create(report);
		
		assertNotNull(error);
		assertEquals("WARN301", error.getCode());
		assertEquals(0, daoService.getAll(report.getSchema()).size());
	}
	
	@Test
	public void createNotExistingReportExistingInDcfAsProcessing() throws DetailedSOAPException {
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2004");
		report.setMonth("04");
		report.setCountry("AT");
		report.setSchema(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		report.setStatus(RCLDatasetStatus.DRAFT);
		
		Dataset d = new Dataset();
		d.setId("42842");
		d.setSenderId("AT0404.00");
		d.setStatus(DcfDatasetStatus.PROCESSING);
		
		DatasetList list = new DatasetList();
		list.add(d);
		
		getDatasetsList.setList(list);
		
		RCLError error = reportService.create(report);
		
		assertNotNull(error);
		assertEquals("WARN300", error.getCode());
		assertEquals(0, daoService.getAll(report.getSchema()).size());
	}
	
	@Test
	public void createNotExistingReportExistingInDcfAsOther() throws DetailedSOAPException {
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2004");
		report.setMonth("04");
		report.setCountry("AT");
		report.setSchema(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		report.setStatus(RCLDatasetStatus.DRAFT);
		
		Dataset d = new Dataset();
		d.setId("42842");
		d.setSenderId("AT0404.00");
		d.setStatus(DcfDatasetStatus.OTHER);
		
		DatasetList list = new DatasetList();
		list.add(d);
		
		getDatasetsList.setList(list);
		
		RCLError error = reportService.create(report);
		
		assertNotNull(error);
		assertEquals("ERR300", error.getCode());
		assertEquals(0, daoService.getAll(report.getSchema()).size());
	}
	
	private TseReport genRandReportWithChildrenInDatabase() {
		
		// first preferences and settings
		// because they will be used everywhere for
		// the other rows
		TableRow prefs = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(prefs);
		
		TableRow settings = RowCreatorMock.genRandSettings();
		int settingsId = daoService.add(settings);
		
		TseReport report = RowCreatorMock.genRandReport(prefId);
		report.setId("");
		int reportId = daoService.add(report);
		
		SummarizedInfo summInfo = RowCreatorMock.genRandSummInfo(reportId, settingsId, prefId);
		int summId = daoService.add(summInfo);
		
		/*CaseReport caseSample = RowCreatorMock.genRandCase(reportId, summId, settingsId, prefId);
		int caseId = daoService.add(caseSample);
		
		AnalyticalResult result = RowCreatorMock.genRandResult(reportId, summId, caseId, settingsId, prefId);
		daoService.add(result);*/
		
		return report;
	}
	
	private MessageConfigBuilder getSendConfig(TseReport report) throws DetailedSOAPException, ReportException {
		
		MessageConfigBuilder messageConfig = reportService.getSendMessageConfiguration(report);
		
		Dataset dcfDataset = reportService.getLatestDataset(report);
		ReportSendOperation op = reportService.getSendOperation(report, dcfDataset);
		
		messageConfig.setOpType(op.getOpType());
		
		return messageConfig;
	}
	
	private MessageConfigBuilder getRejectConfig(TseReport report) throws DetailedSOAPException, ReportException {
		
		MessageConfigBuilder messageConfig = reportService.getSendMessageConfiguration(report);
		
		messageConfig.setOpType(OperationType.REJECT);
		
		return messageConfig;
	}
	
	private MessageConfigBuilder getSubmitConfig(TseReport report) throws DetailedSOAPException, ReportException {
		
		MessageConfigBuilder messageConfig = reportService.getSendMessageConfiguration(report);
		
		messageConfig.setOpType(OperationType.SUBMIT);
		
		return messageConfig;
	}
	
	@Test
	public void sendReportNotExistingInDcf() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		reportService.exportAndSend(report, getSendConfig(report));
		
		assertEquals(RCLDatasetStatus.UPLOADED, report.getRCLStatus());
	}
	
	
	private Dataset setReportCopyInDcfWithStatus(Report report, DcfDatasetStatus status) {
		Dataset d = new Dataset();
		d.setId("32421");
		d.setSenderId(report.getSenderId());
		d.setStatus(status);
		
		DatasetList list = new DatasetList();
		list.add(d);
		
		getDatasetsList.setList(list);
		
		return d;
	}
	
	@Test
	public void sendReportExistingInDcfDeleted() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		Dataset d = setReportCopyInDcfWithStatus(report, DcfDatasetStatus.DELETED);
		
		reportService.send(report, d, getSendConfig(report), null);
		
		assertEquals(RCLDatasetStatus.UPLOADED, report.getRCLStatus());
		assertNotNull(report.getMessageId());
		assertFalse(report.getMessageId().isEmpty());
		assertNotNull(report.getId());
		assertFalse(d.getId().equals(report.getId()));
	}
	
	@Test
	public void sendReportExistingInDcfRejected() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		Dataset d = setReportCopyInDcfWithStatus(report, DcfDatasetStatus.REJECTED);
		
		reportService.send(report, d, getSendConfig(report), null);
		
		assertEquals(RCLDatasetStatus.UPLOADED, report.getRCLStatus());
		assertNotNull(report.getMessageId());
		assertFalse(report.getMessageId().isEmpty());
		assertNotNull(report.getId());
		assertEquals(d.getId(), report.getId());
	}
	
	@Test
	public void sendReportExistingInDcfRejectedEditable() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		Dataset d = setReportCopyInDcfWithStatus(report, DcfDatasetStatus.REJECTED_EDITABLE);
		
		reportService.send(report, d, getSendConfig(report), null);
		
		assertEquals(RCLDatasetStatus.UPLOADED, report.getRCLStatus());
		assertNotNull(report.getMessageId());
		assertFalse(report.getMessageId().isEmpty());
		assertNotNull(report.getId());
		assertEquals(d.getId(), report.getId());
	}
	
	@Test
	public void sendReportExistingInDcfValid() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		Dataset d = setReportCopyInDcfWithStatus(report, DcfDatasetStatus.VALID);
		
		reportService.send(report, d, getSendConfig(report), null);
		
		assertEquals(RCLDatasetStatus.UPLOADED, report.getRCLStatus());
		assertNotNull(report.getMessageId());
		assertFalse(report.getMessageId().isEmpty());
		assertNotNull(report.getId());
		assertEquals(d.getId(), report.getId());
	}
	
	@Test
	public void sendReportExistingInDcfValidWithWarnings() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		Dataset d = setReportCopyInDcfWithStatus(report, DcfDatasetStatus.VALID_WITH_WARNINGS);
		
		reportService.send(report, d, getSendConfig(report), null);
		
		assertEquals(RCLDatasetStatus.UPLOADED, report.getRCLStatus());
		assertNotNull(report.getMessageId());
		assertFalse(report.getMessageId().isEmpty());
		assertNotNull(report.getId());
		assertEquals(d.getId(), report.getId());
	}
	
	@Test
	public void sendReportExistingInDcfAcceptedDwh() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		Dataset d = setReportCopyInDcfWithStatus(report, DcfDatasetStatus.ACCEPTED_DWH);
		
		RCLDatasetStatus prev = report.getRCLStatus();
		
		reportService.send(report, d, getSendConfig(report), null);
		
		// not changed
		assertEquals(prev, report.getRCLStatus());
		assertNotNull(report.getMessageId());
		assertTrue(report.getMessageId().isEmpty());
	}

	@Test
	public void sendReportExistingInDcfSubmitted() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		RCLDatasetStatus prev = report.getRCLStatus();
		
		Dataset d = setReportCopyInDcfWithStatus(report, DcfDatasetStatus.SUBMITTED);
		
		reportService.send(report, d, getSendConfig(report), null);
		
		assertEquals(prev, report.getRCLStatus());
		assertNotNull(report.getMessageId());
		assertTrue(report.getMessageId().isEmpty());
	}
	
	@Test
	public void sendReportExistingInDcfAcceptedDcf() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		RCLDatasetStatus prev = report.getRCLStatus();
		
		Dataset d = setReportCopyInDcfWithStatus(report, DcfDatasetStatus.OTHER);
		
		reportService.send(report, d, getSendConfig(report), null);
		
		assertEquals(prev, report.getRCLStatus());
		assertNotNull(report.getMessageId());
		assertTrue(report.getMessageId().isEmpty());
	}
	
	@Test
	public void sendReportExistingInDcfProcessing() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		RCLDatasetStatus prev = report.getRCLStatus();
		
		Dataset d = setReportCopyInDcfWithStatus(report, DcfDatasetStatus.PROCESSING);
		
		reportService.send(report, d, getSendConfig(report), null);
		
		assertEquals(prev, report.getRCLStatus());
		assertNotNull(report.getMessageId());
		assertTrue(report.getMessageId().isEmpty());
	}
	
	@Test
	public void rejectReport() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		reportService.exportAndSend(report, getRejectConfig(report));
		
		assertEquals(RCLDatasetStatus.REJECTION_SENT, report.getRCLStatus());
		assertNotNull(report.getMessageId());
		assertFalse(report.getMessageId().isEmpty());
	}
	
	@Test
	public void submitReport() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		reportService.exportAndSend(report, getSubmitConfig(report));
		
		assertEquals(RCLDatasetStatus.SUBMISSION_SENT, report.getRCLStatus());
		assertNotNull(report.getMessageId());
		assertFalse(report.getMessageId().isEmpty());
	}
	
	@Test(expected = SendMessageException.class)
	public void sendReportWrongResponse() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXKO, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		reportService.exportAndSend(report, getSendConfig(report));
		
		assertEquals(RCLDatasetStatus.UPLOAD_FAILED, report.getRCLStatus());
	}
	
	@Test
	public void amendReport() {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		String oldVersion = report.getVersion();
		
		TableRowList children1 = daoService.getByParentId(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET), 
				report.getSchema().getSheetName(), report.getDatabaseId(), true);
		
		Report newVersion = reportService.amend(report);
		
		TableRowList children2 = daoService.getByParentId(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET), 
				report.getSchema().getSheetName(), newVersion.getDatabaseId(), true);
		
		assertEquals(TableVersion.createNewVersion(oldVersion), newVersion.getVersion());
		assertEquals(RCLDatasetStatus.DRAFT, newVersion.getRCLStatus());
		assertEquals(children1.size(), children2.size());
	}
}

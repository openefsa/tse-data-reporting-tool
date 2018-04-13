package providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ack.DcfAck;
import ack.FileState;
import ack.MessageValResCode;
import ack.OkCode;
import ack.OpResError;
import amend_manager.AmendException;
import app_config.AppPaths;
import dataset.Dataset;
import dataset.DatasetList;
import dataset.DcfDatasetStatus;
import dataset.Header;
import dataset.IDataset;
import dataset.Operation;
import dataset.RCLDatasetStatus;
import dcf_log.DcfAckLogMock;
import formula.FormulaException;
import global_utils.Message;
import message.MessageConfigBuilder;
import message.MessageResponse;
import message.SendMessageException;
import message.TrxCode;
import message_creator.OperationType;
import mocks.RowCreatorMock;
import mocks.TableDaoMock;
import report.DisplayAckResult;
import report.Report;
import report.ReportException;
import report.ReportSendOperation;
import soap.DetailedSOAPException;
import soap_test.GetAckMock;
import soap_test.GetDatasetMock;
import soap_test.GetDatasetsListMock;
import soap_test.SendMessageMock;
import table_relations.Relation;
import table_skeleton.TableCell;
import table_skeleton.TableRow;
import table_skeleton.TableRowList;
import table_skeleton.TableVersion;
import tse_config.CustomStrings;
import tse_report.TseReport;
import tse_summarized_information.SummarizedInfo;
import xlsx_reader.TableHeaders.XlsxHeader;
import xlsx_reader.TableSchemaList;

public class ReportServiceTest {

	private TseReportService reportService;
	private GetAckMock getAck;
	private GetDatasetsListMock<IDataset> getDatasetsList;
	private SendMessageMock sendMessage;
	private GetDatasetMock getDataset;
	private ITableDaoService daoService;
	private IFormulaService formulaService;
	private Report report;
	
	@Before
	public void init() {
		this.getAck = new GetAckMock();
		this.getDatasetsList = new GetDatasetsListMock<>();
		this.sendMessage = new SendMessageMock();
		this.getDataset = new GetDatasetMock();
		this.daoService = new TableDaoService(new TableDaoMock());
		
		this.formulaService = new FormulaService(daoService);
		
		this.reportService = new TseReportService(getAck, getDatasetsList, sendMessage, getDataset,
				daoService, formulaService);
		
		report = new TseReport();
		report.setId("11234");
		report.setMessageId("25232");
		report.setLastMessageId("25232");
		report.setLastModifyingMessageId("25232");
		report.setLastValidationMessageId("25232");
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2017");
		report.setStatus(RCLDatasetStatus.DRAFT);
		report.put(CustomStrings.EXCEPTION_COUNTRY_COL, "No");
	}
	
	
	@Test
	public void getContextId() throws FormulaException {
		TableRow settings = RowCreatorMock.genRandSettings();
		TableRow pref = RowCreatorMock.genRandPreferences();
		
		daoService.add(settings);
		daoService.add(pref);
		
		TableRow report = RowCreatorMock.genRandReport(pref.getDatabaseId());
		
		daoService.add(report);
		
		SummarizedInfo info = RowCreatorMock.genRandSummInfo(report.getDatabaseId(), 
				settings.getDatabaseId(), pref.getDatabaseId());
		info.put(CustomStrings.SUMMARIZED_INFO_TYPE, CustomStrings.SUMMARIZED_INFO_CWD_TYPE);
		info.put(CustomStrings.SEX_COL, new TableCell(CustomStrings.SEX_MALE, ""));
		
		daoService.add(info);
		
		String contextId = reportService.getContextId(info);
		
		assertFalse(contextId.isEmpty());
	}
	
	/**
	 * Perform a refresh status
	 * @param localStatus status of the local report
	 * @param dcfStatus status of the report in dcf
	 * @param ackStatus status of the retrieved ack
	 * @return
	 */
	private Message refreshStatusWithReadyAck(DcfDatasetStatus dcfStatus, boolean sameModyfingMessageId) {
		
		DcfAck ack = new DcfAck(FileState.READY, new DcfAckLogMock(OkCode.OK, DcfDatasetStatus.VALID));
		getAck.setAck(ack);

		Dataset dcfReport = new Dataset();
		dcfReport.setHeader(new Header("", "", "", "", ""));
		dcfReport.setOperation(new Operation("", "11234", "AT0404", "TSE.TEST", "SSD2_CENTRAL", "EFSA", ""));
		dcfReport.setId("11234");
		dcfReport.setSenderId("AT0404.00");
		dcfReport.setStatus(dcfStatus);
		
		report.setSenderId("AT0404");
		report.setVersion("00");
		
		if (sameModyfingMessageId) {
			String msgId = "12345";
			dcfReport.setLastModifyingMessageId(msgId);
			report.setLastModifyingMessageId(msgId);
		}
		else {
			dcfReport.setLastModifyingMessageId("12345y128");
			report.setLastModifyingMessageId("01923meism");
		}
		
		
		DatasetList list = new DatasetList();
		list.add(dcfReport);
		
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
		r3.setSenderId("AT0404.00");
		r3.setStatus(DcfDatasetStatus.VALID);
		
		DatasetList list = new DatasetList();
		list.add(r1);
		list.add(r2);
		list.add(r3);
		
		getDatasetsList.setList(list);
		
		DatasetList out = reportService.getDatasetsOf("AT0404.00", "2017");
		
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
		
		Dataset data = reportService.getDatasetById("AT0404.00", "2017", "11234");
		
		assertEquals(r1.getId(), data.getId());
		assertEquals(r1.getSenderId(), data.getSenderId());
	}
	
	@Test
	public void getDatasetUsingVersion() throws DetailedSOAPException {
		
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
		r3.setSenderId("AT0404.00");
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
		
		Dataset data = reportService.getDataset(report);
		
		assertEquals(r3.getId(), data.getId());
		assertEquals(r3.getSenderId(), data.getSenderId());
	}
	
	@Test
	public void getDatasetUsingId() throws DetailedSOAPException {
		
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
		r3.setSenderId("AT0404.00");
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
		
		Dataset data = reportService.getDataset(report);
		
		assertEquals(r3.getId(), data.getId());
		assertEquals(r3.getSenderId(), data.getSenderId());
	}
	
	@Test
	public void getSendOperationFirstSend() throws DetailedSOAPException, ReportException {
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2017");
		
		Dataset dataset = reportService.getDataset(report);
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
		
		Dataset dataset = reportService.getDataset(report);
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
		
		Dataset dataset = reportService.getDataset(report);
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
		
		Dataset dataset = reportService.getDataset(report);
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
		
		Dataset dataset = reportService.getDataset(report);
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
		
		Dataset dataset = reportService.getDataset(report);
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
		
		Dataset dataset = reportService.getDataset(report);
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
		
		Dataset dataset = reportService.getDataset(report);
		ReportSendOperation op = reportService.getSendOperation(report, dataset);
		assertEquals(OperationType.REPLACE, op.getOpType());
	}
	
	@Test
	public void refreshStatusFault() {
		
		DcfAck ack = new DcfAck(FileState.EXCEPTION, null);
		getAck.setAck(ack);
		
		RCLDatasetStatus localStatus = RCLDatasetStatus.UPLOADED; // status which uses the ack
		
		report.setStatus(localStatus);

		Message m = reportService.refreshStatus(report);
		
		assertEquals(RCLDatasetStatus.UPLOAD_FAILED, report.getRCLStatus());
		assertEquals("ERR809", m.getCode());
	}
	
	@Test
	public void refreshStatusAccessDenied() {
		
		DcfAck ack = new DcfAck(FileState.ACCESS_DENIED, null);
		getAck.setAck(ack);
		
		RCLDatasetStatus localStatus = RCLDatasetStatus.UPLOADED; // status which uses the ack
		
		report.setStatus(localStatus);

		Message m = reportService.refreshStatus(report);
		
		assertEquals(RCLDatasetStatus.UPLOAD_FAILED, report.getRCLStatus());
		assertEquals("ERR810", m.getCode());
	}
	
	
	@Test
	public void refreshStatusDiscardedMessage() {
		
		DcfAckLogMock log = new DcfAckLogMock(OkCode.KO, null);
		log.setMessageValResCode(MessageValResCode.DISCARDED);
		DcfAck ack = new DcfAck(FileState.READY, log);
		getAck.setAck(ack);
		
		RCLDatasetStatus localStatus = RCLDatasetStatus.UPLOADED; // status which uses the ack
		
		report.setStatus(localStatus);

		Message m = reportService.refreshStatus(report);
		
		assertEquals(RCLDatasetStatus.UPLOAD_FAILED, report.getRCLStatus());
		assertEquals("ERR807", m.getCode());
	}
	
	@Test
	public void refreshStatusOfUploadedReportWithKOAck() {
		
		DcfAck ack = new DcfAck(FileState.READY, new DcfAckLogMock(OkCode.KO, null));
		getAck.setAck(ack);
		
		RCLDatasetStatus localStatus = RCLDatasetStatus.UPLOADED; // status which uses the ack
		
		report.setStatus(localStatus);

		Message m = reportService.refreshStatus(report);
		
		assertEquals(RCLDatasetStatus.UPLOAD_FAILED, report.getRCLStatus());
		assertEquals("ERR806", m.getCode());
	}
	
	@Test
	public void refreshStatusOfRejectionSentReportWithKOAck() {
		
		DcfAck ack = new DcfAck(FileState.READY, new DcfAckLogMock(OkCode.KO, null));
		getAck.setAck(ack);
		
		RCLDatasetStatus localStatus = RCLDatasetStatus.REJECTION_SENT; // status which uses the ack
		
		report.setStatus(localStatus);

		Message m = reportService.refreshStatus(report);
		
		assertEquals(RCLDatasetStatus.REJECTION_FAILED, report.getRCLStatus());
		assertEquals("ERR806", m.getCode());
	}
	
	@Test
	public void refreshStatusOfSubmissionSentReportWithKOAck() {
		
		DcfAck ack = new DcfAck(FileState.READY, new DcfAckLogMock(OkCode.KO, null));
		getAck.setAck(ack);
		
		RCLDatasetStatus localStatus = RCLDatasetStatus.SUBMISSION_SENT; // status which uses the ack
		
		report.setStatus(localStatus);

		Message m = reportService.refreshStatus(report);
		
		assertEquals(RCLDatasetStatus.SUBMISSION_FAILED, report.getRCLStatus());
		assertEquals("ERR806", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalIsUploadedAckFail() {
		DcfAck ack = new DcfAck(FileState.FAIL, null); // in processing
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
	public void refreshStatusWithValidSameModifyingMessageId() {
		
		report.setStatus(RCLDatasetStatus.VALID);
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.VALID, true);
	
		assertEquals(RCLDatasetStatus.VALID, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusLocalUploadedUpToDateMessageId() {
		report.setStatus(RCLDatasetStatus.UPLOADED);
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.VALID, true);

		assertEquals(RCLDatasetStatus.VALID, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
		assertFalse(report.getId().isEmpty());
	}
	
	@Test
	public void refreshStatusWithValidWithWarningsSameModifyingMessageId() {
		report.setStatus(RCLDatasetStatus.VALID);
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.VALID_WITH_WARNINGS, true);

		assertEquals(RCLDatasetStatus.VALID_WITH_WARNINGS, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusWithRejectedEditableSameModifyingMessageId() {
		report.setStatus(RCLDatasetStatus.VALID);
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.REJECTED_EDITABLE, true);

		assertEquals(RCLDatasetStatus.REJECTED_EDITABLE, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusWithRejectedSameModifyingMessageId() {
		report.setStatus(RCLDatasetStatus.VALID);
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.REJECTED, true);

		assertEquals(RCLDatasetStatus.REJECTED, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusWithSubmittedSameModifyingMessageId() {
		report.setStatus(RCLDatasetStatus.VALID);
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.SUBMITTED, true);

		assertEquals(RCLDatasetStatus.SUBMITTED, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusWithAcceptedDwhSameModifyingMessageId() {
		report.setStatus(RCLDatasetStatus.VALID);
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.ACCEPTED_DWH, true);

		assertEquals(RCLDatasetStatus.ACCEPTED_DWH, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusWithAcceptedDcfSameModifyingMessageId() {
		report.setStatus(RCLDatasetStatus.VALID);
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.OTHER, true);

		assertEquals(RCLDatasetStatus.OTHER, report.getRCLStatus());
		assertEquals("OK500", m.getCode());
	}
	
	@Test
	public void refreshStatusWithDeletedSameModifyingMessageId() {
		report.setStatus(RCLDatasetStatus.VALID);
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.DELETED, true);

		assertEquals(RCLDatasetStatus.DRAFT, report.getRCLStatus());
		assertEquals("WARN501", m.getCode());  // automatic draft
	}
	
	@Test
	public void refreshStatusWithDeletedDifferentModifyingMessageId() {
		
		report.setStatus(RCLDatasetStatus.VALID);
		
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.DELETED, false);

		assertEquals(RCLDatasetStatus.DRAFT, report.getRCLStatus());
		assertEquals("WARN501", m.getCode());  // automatic draft
	}
	
	@Test
	public void refreshStatusWithRejectedDifferentModifyingMessageId() {
		
		report.setStatus(RCLDatasetStatus.VALID);
		
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.REJECTED, false);

		assertEquals(RCLDatasetStatus.DRAFT, report.getRCLStatus());
		assertEquals("ERR504", m.getCode());
	}
	
	@Test
	public void refreshStatusWithRejectedEditableDifferentModifyingMessageId() {
		
		report.setStatus(RCLDatasetStatus.VALID);
		
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.REJECTED_EDITABLE, false);

		assertEquals(RCLDatasetStatus.DRAFT, report.getRCLStatus());
		assertEquals("ERR504", m.getCode());
	}
	
	@Test
	public void refreshStatusWithValidDifferentModifyingMessageId() {
		report.setStatus(RCLDatasetStatus.VALID);
		
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.VALID, false);

		assertEquals(RCLDatasetStatus.DRAFT, report.getRCLStatus());
		assertEquals("ERR504", m.getCode());
	}
	
	@Test
	public void refreshStatusWithValidWithWarningsDifferentModifyingMessageId() {
		
		report.setStatus(RCLDatasetStatus.VALID);
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.VALID_WITH_WARNINGS, false);

		assertEquals(RCLDatasetStatus.DRAFT, report.getRCLStatus());
		assertEquals("ERR504", m.getCode());
	}
	
	@Test
	public void refreshStatusWithAcceptedDwhDifferentModifyingMessageId() {
		report.setStatus(RCLDatasetStatus.VALID);
		RCLDatasetStatus prevStat = report.getRCLStatus();
		
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.ACCEPTED_DWH, false);

		assertEquals(prevStat, report.getRCLStatus());
		assertEquals("ERR505", m.getCode());
	}
	
	@Test
	public void refreshStatusWithAcceptedDcfDifferentModifyingMessageId() {
		report.setStatus(RCLDatasetStatus.VALID);
		RCLDatasetStatus prevStat = report.getRCLStatus();
		
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.OTHER, false);

		assertEquals(prevStat, report.getRCLStatus());
		assertEquals("ERR505", m.getCode());
	}
	
	@Test
	public void refreshStatusWithSubmittedDifferentModifyingMessageId() {
		report.setStatus(RCLDatasetStatus.VALID);
		RCLDatasetStatus prevStat = report.getRCLStatus();
		
		Message m = refreshStatusWithReadyAck(DcfDatasetStatus.SUBMITTED, false);

		assertEquals(prevStat, report.getRCLStatus());
		assertEquals("ERR505", m.getCode());
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
	public void refreshStatusAckNotFound() {
		getAck.setAck(null);
		
		report.setStatus(RCLDatasetStatus.UPLOADED);  // allow to get ack
		Message m = reportService.refreshStatus(report);
		
		// processing message
		assertEquals("ERR803", m.getCode());
	}
	
	@Test
	public void refreshStatusNotExistingDataCollectionError() {
		
		DcfAckLogMock log = new DcfAckLogMock(OkCode.KO, null);
		log.setOpResError(OpResError.NOT_EXISTING_DC);
		log.setOpResLog(Arrays.asList("error"));
		
		DcfAck ack = new DcfAck(FileState.READY, log);
		getAck.setAck(ack);
		
		report.setStatus(RCLDatasetStatus.UPLOADED);
		Message m = reportService.refreshStatus(report);
		
		// processing message
		assertEquals("ERR407", m.getCode());
	}
	
	@Test
	public void refreshStatusUserNotAuthorizedForDataCollectionError() {
		
		DcfAckLogMock log = new DcfAckLogMock(OkCode.KO, null);
		log.setOpResError(OpResError.USER_NOT_AUTHORIZED);
		log.setOpResLog(Arrays.asList("error"));
		
		DcfAck ack = new DcfAck(FileState.READY, log);
		getAck.setAck(ack);
		
		report.setStatus(RCLDatasetStatus.UPLOADED);
		Message m = reportService.refreshStatus(report);
		
		// processing message
		assertEquals("ERR101", m.getCode());
	}
	
	@Test
	public void refreshStatusGeneralAckError() {
		
		DcfAckLogMock log = new DcfAckLogMock(OkCode.KO, null);
		log.setOpResError(OpResError.OTHER);
		log.setOpResLog(Arrays.asList("error"));
		
		DcfAck ack = new DcfAck(FileState.READY, log);
		getAck.setAck(ack);
		
		report.setStatus(RCLDatasetStatus.UPLOADED);
		Message m = reportService.refreshStatus(report);
		
		// processing message
		assertEquals("ERR502", m.getCode());
	}
	
	@Test
	public void displayAckNoMessageIdNoDatasetId() {
		
		report.remove(AppPaths.REPORT_DATASET_ID);
		report.remove(AppPaths.REPORT_MESSAGE_ID);
		DisplayAckResult m = reportService.displayAck(report);
		assertNull(m.getDownloadedAck());
		assertEquals(1, m.getMessages().size());
		assertEquals("ERR800", m.getMessages().get(0).getCode());
	}
	
	@Test
	public void displayAckNoDatasetId() {
		
		getAck.setAck(new DcfAck(FileState.READY, new DcfAckLogMock(OkCode.OK, DcfDatasetStatus.VALID)));
		
		report.setMessageId("12342");
		report.remove(AppPaths.REPORT_DATASET_ID);
		DisplayAckResult m = reportService.displayAck(report);
		assertEquals(report.getMessageId(), m.getDcfMessageId());  // local message id should be used
		assertTrue(m.getMessages().isEmpty());
		assertNotNull(m.getDownloadedAck());
	}
	
	@Test
	public void displayAckModificationOutOfDateAckReady() {
		
		// ready ack
		getAck.setAck(new DcfAck(FileState.READY, new DcfAckLogMock(OkCode.OK, DcfDatasetStatus.VALID)));
		
		DatasetList list = new DatasetList();
		Dataset d = new Dataset();
		d.setId(report.getId());
		d.setStatus(DcfDatasetStatus.VALID);
		d.setSenderId(TableVersion.mergeNameAndVersion(report.getSenderId(), report.getVersion()));
		d.setLastModifyingMessageId("15402");
		d.setLastValidationMessageId("15402");
		d.setLastMessageId("15402");
		list.add(d);
		getDatasetsList.setList(list);
		
		report.setMessageId("15000");
		report.setLastModifyingMessageId("15000");  // less than the one in dcf
		report.setLastValidationMessageId("15000");
		
		DisplayAckResult m = reportService.displayAck(report);
		
		assertEquals(report.getMessageId(), m.getDcfMessageId()); // local message id should be used
		assertEquals(1, m.getMessages().size());
		assertEquals("WARN800", m.getMessages().get(0).getCode());
		assertNotNull(m.getDownloadedAck());
		assertTrue(m.getDownloadedAck().exists());
	}
	
	@Test
	public void displayAckModificationUpToDateButLocalMessageIdBiggerThanLocalModificationAckReady() {
		
		getAck.setAck(new DcfAck(FileState.READY, new DcfAckLogMock(OkCode.OK, DcfDatasetStatus.VALID)));
		
		DatasetList list = new DatasetList();
		Dataset d = new Dataset();
		d.setId(report.getId());
		d.setStatus(DcfDatasetStatus.VALID);
		d.setSenderId(TableVersion.mergeNameAndVersion(report.getSenderId(), report.getVersion()));
		d.setLastModifyingMessageId("15402");
		d.setLastValidationMessageId("15402");
		d.setLastMessageId("15402");
		list.add(d);
		getDatasetsList.setList(list);
		
		report.setMessageId("15403");  // not equal to local last modifying
		report.setLastModifyingMessageId("15402");  // equal to the one in dcf
		
		DisplayAckResult m = reportService.displayAck(report);
		
		assertEquals(report.getMessageId(), m.getDcfMessageId()); // local message id should be used
		assertTrue(m.getMessages().isEmpty());
		assertNotNull(m.getDownloadedAck());
	}
	
	@Test
	public void displayAckModificationAndValidationUpToDateAckReady() {
		
		// ready ack
		getAck.setAck(new DcfAck(FileState.READY, new DcfAckLogMock(OkCode.OK, DcfDatasetStatus.VALID)));
		
		DatasetList list = new DatasetList();
		Dataset d = new Dataset();
		d.setId(report.getId());
		d.setStatus(DcfDatasetStatus.VALID);
		d.setSenderId(TableVersion.mergeNameAndVersion(report.getSenderId(), report.getVersion()));
		d.setLastModifyingMessageId("15402");
		d.setLastValidationMessageId("15402");
		d.setLastMessageId("15402");
		list.add(d);
		getDatasetsList.setList(list);
		
		report.setMessageId("15402");
		report.setLastModifyingMessageId("15402");  // equal to the one in dcf
		
		DisplayAckResult m = reportService.displayAck(report);
		
		assertEquals(d.getLastValidationMessageId(), m.getDcfMessageId()); // dcf validation message id should be used
		assertTrue(m.getMessages().isEmpty());
		assertNotNull(m.getDownloadedAck());
		assertTrue(m.getDownloadedAck().exists());
	}
	
	@Test
	public void displayAckModificationUpToDateValidationOutOfDateAckReady() {
		
		// ready ack
		getAck.setAck(new DcfAck(FileState.READY, new DcfAckLogMock(OkCode.OK, DcfDatasetStatus.VALID)));
		
		DatasetList list = new DatasetList();
		Dataset d = new Dataset();
		d.setId(report.getId());
		d.setStatus(DcfDatasetStatus.VALID);
		d.setSenderId(TableVersion.mergeNameAndVersion(report.getSenderId(), report.getVersion()));
		d.setLastModifyingMessageId("15402");
		d.setLastValidationMessageId("15403");  // bigger than local modifying
		d.setLastMessageId("15403");
		list.add(d);
		getDatasetsList.setList(list);
		
		report.setMessageId("15402");
		report.setLastModifyingMessageId("15402");  // equal to the one in dcf
		
		DisplayAckResult m = reportService.displayAck(report);

		assertEquals(d.getLastValidationMessageId(), m.getDcfMessageId()); // dcf validation message id should be used
		assertEquals(1, m.getMessages().size());
		assertEquals("WARN801", m.getMessages().get(0).getCode());
		assertNotNull(m.getDownloadedAck());
		assertTrue(m.getDownloadedAck().exists());
	}
	
	@Test
	public void displayAckModificationOutOfDateAckNotReady() {
		
		// ready ack
		getAck.setAck(new DcfAck(FileState.WAIT, null));
		
		DatasetList list = new DatasetList();
		Dataset d = new Dataset();
		d.setId(report.getId());
		d.setStatus(DcfDatasetStatus.VALID);
		d.setSenderId(TableVersion.mergeNameAndVersion(report.getSenderId(), report.getVersion()));
		d.setLastModifyingMessageId("15402");
		d.setLastValidationMessageId("15402");
		d.setLastMessageId("15402");
		list.add(d);
		getDatasetsList.setList(list);
		
		report.setMessageId("15000");
		report.setLastModifyingMessageId("15000");  // less than the one in dcf
		
		DisplayAckResult m = reportService.displayAck(report);
		
		assertEquals(report.getMessageId(), m.getDcfMessageId()); // local message id should be used
		assertEquals(2, m.getMessages().size());
		
		Message first = m.getMessages().get(0);
		Message second = m.getMessages().get(1);
		
		assertEquals("WARN800", first.getCode());  // inconsistent modification code
		assertEquals("WARN500", second.getCode());  // still in processing
		
		assertNull(m.getDownloadedAck());
	}
	
	@Test
	public void displayAckModificationAndValidationUpToDateAckNotReady() {
		
		// ready ack
		getAck.setAck(new DcfAck(FileState.WAIT, null));
		
		DatasetList list = new DatasetList();
		Dataset d = new Dataset();
		d.setId(report.getId());
		d.setStatus(DcfDatasetStatus.VALID);
		d.setSenderId(TableVersion.mergeNameAndVersion(report.getSenderId(), report.getVersion()));
		d.setLastModifyingMessageId("15402");
		d.setLastValidationMessageId("15402");
		d.setLastMessageId("15402");
		list.add(d);
		getDatasetsList.setList(list);
		
		report.setMessageId("15402");
		report.setLastModifyingMessageId("15402");  // equal to the one in dcf
		
		DisplayAckResult m = reportService.displayAck(report);
		
		assertEquals(d.getLastValidationMessageId(), m.getDcfMessageId()); // dcf validation message id should be used
		assertEquals(1, m.getMessages().size());
		assertEquals("WARN500", m.getMessages().get(0).getCode());  // still in processing
		assertNull(m.getDownloadedAck());
	}
	
	@Test
	public void displayAckModificationUpToDateButLocalMessageIdBiggerThanLocalModificationAckNotReady() {
		
		getAck.setAck(new DcfAck(FileState.WAIT, null));
		
		DatasetList list = new DatasetList();
		Dataset d = new Dataset();
		d.setId(report.getId());
		d.setStatus(DcfDatasetStatus.VALID);
		d.setSenderId(TableVersion.mergeNameAndVersion(report.getSenderId(), report.getVersion()));
		d.setLastModifyingMessageId("15402");
		d.setLastValidationMessageId("15402");
		d.setLastMessageId("15402");
		list.add(d);
		getDatasetsList.setList(list);
		
		report.setMessageId("15403");  // not equal to local last modifying
		report.setLastModifyingMessageId("15402");  // equal to the one in dcf
		
		DisplayAckResult m = reportService.displayAck(report);
		
		assertEquals(1, m.getMessages().size());
		assertEquals("WARN500", m.getMessages().get(0).getCode());  // still in processing
		assertNull(m.getDownloadedAck());
	}
	
	@Test
	public void displayAckModificationUpToDateValidationOutOfDateAckNotReady() {
		
		// ready ack
		getAck.setAck(new DcfAck(FileState.WAIT, null));
		
		DatasetList list = new DatasetList();
		Dataset d = new Dataset();
		d.setId(report.getId());
		d.setStatus(DcfDatasetStatus.VALID);
		d.setSenderId(TableVersion.mergeNameAndVersion(report.getSenderId(), report.getVersion()));
		d.setLastModifyingMessageId("15402");
		d.setLastValidationMessageId("15403");  // bigger than local modifying
		d.setLastMessageId("15403");
		list.add(d);
		getDatasetsList.setList(list);
		
		report.setMessageId("15402");
		report.setLastModifyingMessageId("15402");  // equal to the one in dcf
		
		DisplayAckResult m = reportService.displayAck(report);
		
		assertEquals(d.getLastValidationMessageId(), m.getDcfMessageId()); // dcf validation message id should be used
		assertEquals(2, m.getMessages().size());
		
		Message first = m.getMessages().get(0);
		Message second = m.getMessages().get(1);
		
		assertEquals("WARN801", first.getCode());  // validation out of date
		assertEquals("WARN500", second.getCode());  // still in processing
		assertNull(m.getDownloadedAck());
	}
	
	@Test
	public void displayAckNoAck() {
		
		// get datasets list mock
		DatasetList list = new DatasetList();
		Dataset d = new Dataset();
		d.setId(report.getId());
		d.setStatus(DcfDatasetStatus.VALID);
		d.setSenderId(TableVersion.mergeNameAndVersion(report.getSenderId(), report.getVersion()));
		d.setLastModifyingMessageId(report.getLastModifyingMessageId());
		d.setLastValidationMessageId("19402");
		list.add(d);
		getDatasetsList.setList(list);
		
		DisplayAckResult m = reportService.displayAck(report);
		assertNull(m.getDownloadedAck());
		assertEquals(1, m.getMessages().size());
		assertEquals("ERR803", m.getMessages().get(0).getCode());
	}
	
	@Test
	public void displayAckNoDatasetIdAckReadyWithoutLog() {
		
		DcfAck ack = new DcfAck(FileState.READY, null);
		getAck.setAck(ack);
		
		report.remove(AppPaths.REPORT_DATASET_ID);
		
		DisplayAckResult m = reportService.displayAck(report);
		assertNull(m.getDownloadedAck());
		assertEquals(1, m.getMessages().size());
		assertEquals("ERR803", m.getMessages().get(0).getCode());
	}
	
	@Test
	public void displayAckNoDatasetInDcf() {
		
		report.setId("19320923103");  // not in dcf
		
		// no dataset related to the report in dcf
		DisplayAckResult m = reportService.displayAck(report);
		assertNull(m.getDownloadedAck());
		assertEquals(1, m.getMessages().size());
		assertEquals("ERR808", m.getMessages().get(0).getCode());
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
		
		Dataset dcfDataset = reportService.getDataset(report);
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
		SAXException, SendMessageException, AmendException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		reportService.exportAndSend(report, getSendConfig(report));
		
		assertEquals(RCLDatasetStatus.UPLOADED, report.getRCLStatus());
	}
	
	
	private Dataset setReportCopyInDcfWithStatus(Report report, DcfDatasetStatus status) {
		Dataset d = new Dataset();
		d.setId(report.getId());
		d.setSenderId(TableVersion.mergeNameAndVersion(report.getSenderId(), report.getVersion()));
		d.setStatus(status);
		
		DatasetList list = new DatasetList();
		list.add(d);
		
		getDatasetsList.setList(list);
		
		return d;
	}
	
	@Test
	public void sendReportExistingInDcfDeleted() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException, AmendException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXOK, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		Dataset d = setReportCopyInDcfWithStatus(report, DcfDatasetStatus.DELETED);
		
		reportService.send(report, d, getSendConfig(report), null);
		
		assertEquals(RCLDatasetStatus.UPLOADED, report.getRCLStatus());
		assertNotNull(report.getMessageId());
		assertFalse(report.getMessageId().isEmpty());
		assertNotNull(report.getId());
	}
	
	@Test
	public void sendReportExistingInDcfRejected() throws DetailedSOAPException, ReportException, 
		IOException, ParserConfigurationException, 
		SAXException, SendMessageException, AmendException {
		
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
		SAXException, SendMessageException, AmendException {
		
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
		SAXException, SendMessageException, AmendException {
		
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
		SAXException, SendMessageException, AmendException {
		
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
		SAXException, SendMessageException, AmendException {
		
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
		SAXException, SendMessageException, AmendException {
		
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
		SAXException, SendMessageException, AmendException {
		
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
		SAXException, SendMessageException, AmendException {
		
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
		SAXException, SendMessageException, AmendException {
		
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
		SAXException, SendMessageException, AmendException {
		
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
		SAXException, SendMessageException, AmendException {
		
		sendMessage.setResponse(new MessageResponse("12342", TrxCode.TRXKO, null));
		
		TseReport report = genRandReportWithChildrenInDatabase();
		
		reportService.exportAndSend(report, getSendConfig(report));
		
		assertEquals(RCLDatasetStatus.UPLOAD_FAILED, report.getRCLStatus());
	}
	
	@Test
	public void amendReportCheckIfSummarizedInformationAreCopied() {
		
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
	
	@Test
	public void checkRGTDefaultCase() {
		
		TableRow pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		TableRow opt = RowCreatorMock.genRandSettings();
		int optId = daoService.add(opt);
		
		SummarizedInfo si = RowCreatorMock.genRandSummInfo(report.getDatabaseId(), optId, prefId);
		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, new TableCell(CustomStrings.SUMMARIZED_INFO_RGT_TYPE, ""));
		
		reportService.createDefaultRGTCase(report, si);
		
		TableRowList list = daoService.getAll(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));
		
		assertEquals(1, list.size());
		
		TableRow rgt = list.iterator().next();
		
		// blood as part for rgt
		assertEquals(CustomStrings.BLOOD_CODE, rgt.getCode(CustomStrings.PART_COL));
	}
	
	@Test
	public void checkDefaultCasesForNonCWD() throws IOException {
		
		TableRow pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		TableRow opt = RowCreatorMock.genRandSettings();
		int optId = daoService.add(opt);
		
		SummarizedInfo si = RowCreatorMock.genRandSummInfo(report.getDatabaseId(), optId, prefId);
		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, new TableCell(CustomStrings.SUMMARIZED_INFO_BSE_TYPE, ""));
		si.put(CustomStrings.TOT_SAMPLE_INCONCLUSIVE_COL, "1");
		si.put(CustomStrings.TOT_SAMPLE_POSITIVE_COL, "0");
		si.put(CustomStrings.TOT_SAMPLE_NEGATIVE_COL, "0");
		
		reportService.createDefaultCases(report, si);
		
		TableRowList list = daoService.getAll(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));
		
		// two cases for cwd
		assertEquals(1, list.size());
		
		Iterator<TableRow> iterator = list.iterator();
		
		TableRow case1 = iterator.next();
		
		assertEquals(CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE, case1.getCode(CustomStrings.SAMP_AN_ASSES_COL));
		
		// obex and lymph for cases part
		boolean hasObex1 = CustomStrings.OBEX_CODE.equals(case1.getCode(CustomStrings.PART_COL));
		
		assertTrue(hasObex1);
	}
	
	@Test
	public void checkDefaultCasesForCWD() throws IOException {
		
		TableRow pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		TableRow opt = RowCreatorMock.genRandSettings();
		int optId = daoService.add(opt);
		
		SummarizedInfo si = RowCreatorMock.genRandSummInfo(report.getDatabaseId(), optId, prefId);
		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, new TableCell(CustomStrings.SUMMARIZED_INFO_CWD_TYPE, ""));
		si.put(CustomStrings.TOT_SAMPLE_INCONCLUSIVE_COL, "1");
		si.put(CustomStrings.TOT_SAMPLE_POSITIVE_COL, "0");
		si.put(CustomStrings.TOT_SAMPLE_NEGATIVE_COL, "0");
		
		reportService.createDefaultCases(report, si);
		
		TableRowList list = daoService.getAll(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));
		
		// two cases for cwd
		assertEquals(2, list.size());
		
		Iterator<TableRow> iterator = list.iterator();
		
		TableRow case1 = iterator.next();
		TableRow case2 = iterator.next();
		
		assertEquals(CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE, case1.getCode(CustomStrings.SAMP_AN_ASSES_COL));
		assertEquals(CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE, case2.getCode(CustomStrings.SAMP_AN_ASSES_COL));
		
		// obex and lymph for cases part
		boolean hasObex1 = CustomStrings.OBEX_CODE.equals(case1.getCode(CustomStrings.PART_COL));
		boolean hasObex2 = CustomStrings.OBEX_CODE.equals(case2.getCode(CustomStrings.PART_COL));
		
		assertTrue(hasObex1 || hasObex2);
		
		boolean hasLymph1 = CustomStrings.RETROPHARYNGEAL_CODE.equals(case1.getCode(CustomStrings.PART_COL));
		boolean hasLymph2 = CustomStrings.RETROPHARYNGEAL_CODE.equals(case2.getCode(CustomStrings.PART_COL));
		
		assertTrue(hasLymph1 || hasLymph2);
	}
	
	@Test(expected = AmendException.class)
	public void exportReportWithAmendmentsButNoDifferencesException() throws IOException, ParserConfigurationException, 
		SAXException, ReportException, AmendException {
		
		// create two versions of the report
		TseReport report = genRandReportWithChildrenInDatabase();
		TseReport amendedReport = reportService.amend(report);
		
		MessageConfigBuilder builder = reportService.getSendMessageConfiguration(amendedReport);
		builder.setOpType(OperationType.INSERT);
		reportService.export(amendedReport, builder);
	}
	
	@Test
	public void exportReportWithAmendmentsButNoDifferencesStatusChangedToUploadFailed() throws IOException, ParserConfigurationException, 
		SAXException, ReportException, DetailedSOAPException, SendMessageException {
		
		// create two versions of the report
		TseReport report = genRandReportWithChildrenInDatabase();
		TseReport amendedReport = reportService.amend(report);

		boolean error = false; 
		try {
			reportService.exportAndSend(amendedReport, getSendConfig(amendedReport));
		}
		catch(AmendException e) {
			assertEquals(RCLDatasetStatus.UPLOAD_FAILED, amendedReport.getRCLStatus());
			error = true;
		}

		assertTrue(error);
	}
	
	@Test
	public void exportReportWithAmendmentsWithNewRecord() 
			throws IOException, ParserConfigurationException, SAXException, ReportException, AmendException {
		
		// create two versions of the report
		TseReport report = genRandReportWithChildrenInDatabase();
		TseReport amendedReport = reportService.amend(report);
		
		// new summarized information for the second report
		SummarizedInfo si = RowCreatorMock.genRandSummInfo(amendedReport.getDatabaseId(), 1, // settId not required
				amendedReport.getNumCode(CustomStrings.PREFERENCES_ID_COL));
		
		si.put(CustomStrings.RES_ID_COL, "jdbadabdjsabdjsb");  // needed to say that it is different from the other summ info
		si.put(CustomStrings.PROG_ID_COL, "proggoid");
		daoService.add(si);
		
		MessageConfigBuilder builder = reportService.getSendMessageConfiguration(amendedReport);
		builder.setOpType(OperationType.INSERT);
		File exportedFile = reportService.export(amendedReport, builder);
		
		assertNotNull(exportedFile);
		
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(exportedFile);

		assertEquals(1, doc.getElementsByTagName("result").getLength());  // 1 record
		assertEquals(0, doc.getElementsByTagName("amType").getLength());	
	}
	
	@Test
	public void exportReportWithAmendmentsWithUpdatedRecord() throws IOException, 
		ParserConfigurationException, SAXException, ReportException, AmendException {
		
		// create two versions of the report
		TseReport report = genRandReportWithChildrenInDatabase();
		TseReport amendedReport = reportService.amend(report);
		
		TableRowList list = daoService.getByParentId(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET), 
				amendedReport.getSchema().getSheetName(), amendedReport.getDatabaseId(), false);
		
		TableRow si = list.iterator().next();
		si.put(CustomStrings.TOT_SAMPLE_INCONCLUSIVE_COL, "20");  // update
		daoService.update(si);
		
		MessageConfigBuilder builder = reportService.getSendMessageConfiguration(amendedReport);
		builder.setOpType(OperationType.INSERT);
		File exportedFile = reportService.export(amendedReport, builder);
		
		assertNotNull(exportedFile);
		
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(exportedFile);

		assertEquals(1, doc.getElementsByTagName("result").getLength());
		assertEquals(1, doc.getElementsByTagName("amType").getLength());
		assertEquals("U", doc.getElementsByTagName("amType").item(0).getTextContent());
	}
	
	@Test
	public void exportReportWithAmendmentsWithDeletedRecordCreatingAnEmptyDatasetInDcf() 
			throws IOException, ParserConfigurationException, SAXException, ReportException, AmendException {
		
		// create two versions of the report
		TseReport report = genRandReportWithChildrenInDatabase();
		
		TseReport amendedReport = reportService.amend(report);

		TableRowList list = daoService.getByParentId(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET), 
				amendedReport.getSchema().getSheetName(), amendedReport.getDatabaseId(), false);

		// delete one row from the amended report => it will create an empty dataset
		TableRow si = list.iterator().next();
		daoService.delete(si.getSchema(), si.getDatabaseId());
		
		MessageConfigBuilder builder = reportService.getSendMessageConfiguration(amendedReport);
		builder.setOpType(OperationType.INSERT);
		File exportedFile = reportService.export(amendedReport, builder);
		
		assertNotNull(exportedFile);
		
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(exportedFile);

		assertEquals(1, doc.getElementsByTagName("result").getLength());
		assertEquals(1, doc.getElementsByTagName("amType").getLength());
		assertEquals("D", doc.getElementsByTagName("amType").item(0).getTextContent());
	}
	
	@Test
	public void exportReportWithAmendmentsWithDeletedRecordCreatingANonEmptyDatasetInDcf() 
			throws IOException, ParserConfigurationException, SAXException, ReportException, AmendException {
		
		// create two versions of the report
		TseReport report = genRandReportWithChildrenInDatabase();
		
		// add an additional summ info
		SummarizedInfo summInfo = RowCreatorMock.genRandSummInfo(report.getDatabaseId(), 1, 
				report.getNumCode(CustomStrings.PREFERENCES_ID_COL));
		daoService.add(summInfo);
		
		TseReport amendedReport = reportService.amend(report);

		TableRowList list = daoService.getByParentId(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET), 
				amendedReport.getSchema().getSheetName(), amendedReport.getDatabaseId(), false);

		// delete one row from the amended report
		TableRow si = list.iterator().next();
		daoService.delete(si.getSchema(), si.getDatabaseId());
		
		MessageConfigBuilder builder = reportService.getSendMessageConfiguration(amendedReport);
		builder.setOpType(OperationType.INSERT);
		File exportedFile = reportService.export(amendedReport, builder);
		
		assertNotNull(exportedFile);
		
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(exportedFile);

		assertEquals(1, doc.getElementsByTagName("result").getLength());
		assertEquals(1, doc.getElementsByTagName("amType").getLength());
		assertEquals("D", doc.getElementsByTagName("amType").item(0).getTextContent());
	}
	
	@Test
	public void exportReportWithoutAmendmentsWithInsertOperation() 
			throws IOException, ParserConfigurationException, SAXException, ReportException, AmendException {
		
		// create two versions of the report
		TseReport report = genRandReportWithChildrenInDatabase();
		
		MessageConfigBuilder builder = reportService.getSendMessageConfiguration(report);
		builder.setOpType(OperationType.INSERT);
		File exportedFile = reportService.export(report, builder);
		
		assertNotNull(exportedFile);
		
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(exportedFile);

		assertEquals(1, doc.getElementsByTagName("result").getLength());
	}
	
	@Test
	public void computeContextIdForAnalyticalResultForBSEExceptionCountry() 
			throws NoSuchAlgorithmException, ParseException, FormulaException {
		
		TableRow pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		TableRow settings = RowCreatorMock.genRandSettings();
		int setId = daoService.add(settings);
		
		TableRow report = RowCreatorMock.genRandReport(prefId);
		report.put(CustomStrings.REPORT_COUNTRY, "EE");
		report.put(CustomStrings.EXCEPTION_COUNTRY_COL, "Yes");
		report.put(AppPaths.REPORT_YEAR_COL, "2012"); // Important to have the same year in report and in data!
		
		int repId = daoService.add(report);

		TableRow result = new TableRow(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
		Relation.injectParent(report, result);
		
		String tg = "TG001A";
		String sampC = "CY";
		String sampY = "2012";
		String sampM = "2";
		String source = "F01.A057C";
		String prod = "F21.A07RV";
		String animage = "F31.A16NJ";
		String sex = "F32.A0C8Z";  // present but not considered in context
		
		result.put(CustomStrings.TARGET_GROUP_COL, tg);
		result.put("sampCountry", sampC);
		result.put("sampY", sampY);
		result.put("sampM", sampM);
		result.put("source", source);
		result.put("prod", prod);
		result.put("animage", animage);
		result.put("sex", sex);
		
		String sampMatCode = "A04MQ#";
		for (String facet: new String[] {source, prod, animage, sex})
			sampMatCode = sampMatCode + "$" + facet;
		
		result.put(CustomStrings.SAMP_MAT_CODE_COL, sampMatCode);
		result.put(CustomStrings.PROG_ID_COL, "tseTargetGroup=" + tg);
		
		String hashAlgorithm = "MD5";
		String value = tg + sampC + sampY + sampM + source + prod + animage;
		
		byte[] byteArray = value.getBytes();
		byte[] digest;
		digest = MessageDigest.getInstance(hashAlgorithm).digest(byteArray);
		
		String hash = DatatypeConverter.printHexBinary(digest);
		
		assertEquals(hash, reportService.getContextIdFrom(result));
	}
	
	@Test
	public void contextIdForAggregatedCWDForExceptionalCountry() throws NoSuchAlgorithmException, FormulaException {
		
		TableRow pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		TableRow settings = RowCreatorMock.genRandSettings();
		int setId = daoService.add(settings);
		
		TableRow report = RowCreatorMock.genRandReport(prefId);
		report.put(CustomStrings.REPORT_COUNTRY, "EE");
		report.put(CustomStrings.EXCEPTION_COUNTRY_COL, "Yes");
		report.put(AppPaths.REPORT_YEAR_COL, "2012"); // Important to have the same year in report and in data!
		
		int repId = daoService.add(report);
		
		TableRow aggr = new TableRow(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));
		Relation.injectParent(report, aggr);
		
		aggr.put(CustomStrings.SUMMARIZED_INFO_TYPE, CustomStrings.SUMMARIZED_INFO_CWD_TYPE);
		
		String tg = "TG006A";
		String sampC = "CY";
		String sampY = "2012";
		String sampM = "2";
		String source = "F01.A056N";
		String prod = "F21.A07RV";
		String animage = "F31.A16NF";
		String sex = "F32.A0C8Z";
		String psuId = "mypsuID";
		
		aggr.put(CustomStrings.TARGET_GROUP_COL, tg);
		aggr.put("sampCountry", sampC);
		aggr.put("sampY", sampY);
		aggr.put("sampM", sampM);
		aggr.put(CustomStrings.SOURCE_COL, new TableCell(source, ""));
		aggr.put(CustomStrings.PROD_COL, prod);
		aggr.put(CustomStrings.ANIMAGE_COL, animage);
		aggr.put(CustomStrings.SEX_COL, new TableCell(sex, ""));
		aggr.put(CustomStrings.PSU_ID_COL, psuId);
		
		int summId = daoService.add(aggr);
		
		String contextId = formulaService.solve(aggr, aggr.getSchema()
				.getById(CustomStrings.CONTEXT_ID_COL), XlsxHeader.LABEL_FORMULA);
		
		String hashAlgorithm = "MD5";
		String value = tg + sampC + sampY + sampM + source + prod + animage + sex + psuId;
		
		byte[] byteArray = value.getBytes();
		byte[] digest;
		digest = MessageDigest.getInstance(hashAlgorithm).digest(byteArray);
		
		String hash = DatatypeConverter.printHexBinary(digest);
		
		assertEquals(hash, contextId);
	}
	
	@Test
	public void contextIdForAggregatedCWDForNONExceptionalCountry() throws NoSuchAlgorithmException, FormulaException {
		
		TableRow pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		TableRow settings = RowCreatorMock.genRandSettings();
		int setId = daoService.add(settings);
		
		TableRow report = RowCreatorMock.genRandReport(prefId);
		report.put(CustomStrings.REPORT_COUNTRY, "CY");
		report.put(CustomStrings.EXCEPTION_COUNTRY_COL, "No");
		report.put(AppPaths.REPORT_YEAR_COL, "2012"); // Important to have the same year in report and in data!
		
		int repId = daoService.add(report);
		
		TableRow aggr = new TableRow(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));
		Relation.injectParent(report, aggr);
		
		aggr.put(CustomStrings.SUMMARIZED_INFO_TYPE, CustomStrings.SUMMARIZED_INFO_CWD_TYPE);
		
		String tg = "TG006A";
		String sampC = "CY";
		String sampY = "2012";
		String sampM = "2";
		String source = "F01.A056N";
		String prod = "F21.A07RV";
		String animage = "F31.A16NF";
		String sex = "F32.A0C9B";
		String psuId = "mypsuID";
		
		aggr.put(CustomStrings.TARGET_GROUP_COL, tg);
		aggr.put("sampCountry", sampC);
		aggr.put("sampY", sampY);
		aggr.put("sampM", sampM);
		aggr.put(CustomStrings.SOURCE_COL, new TableCell(source, ""));
		aggr.put(CustomStrings.PROD_COL, prod);
		aggr.put(CustomStrings.ANIMAGE_COL, animage);
		aggr.put(CustomStrings.SEX_COL, new TableCell(sex, ""));
		aggr.put(CustomStrings.PSU_ID_COL, psuId);
		
		int summId = daoService.add(aggr);
		
		String contextId = formulaService.solve(aggr, aggr.getSchema()
				.getById(CustomStrings.CONTEXT_ID_COL), XlsxHeader.LABEL_FORMULA);
		
		String hashAlgorithm = "MD5";
		String value = tg + sampC + sampY + sampM + source + prod + animage;
		
		byte[] byteArray = value.getBytes();
		byte[] digest;
		digest = MessageDigest.getInstance(hashAlgorithm).digest(byteArray);
		
		String hash = DatatypeConverter.printHexBinary(digest);
		
		assertEquals(hash, contextId);
	}
	
	@Test
	public void contextIdForAggregatedBSEForExceptionalCountry() throws NoSuchAlgorithmException, FormulaException {
		
		TableRow pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		TableRow settings = RowCreatorMock.genRandSettings();
		int setId = daoService.add(settings);
		
		TableRow report = RowCreatorMock.genRandReport(prefId);
		report.put(CustomStrings.REPORT_COUNTRY, "EE");
		report.put(CustomStrings.EXCEPTION_COUNTRY_COL, "Yes");
		report.put(AppPaths.REPORT_YEAR_COL, "2012"); // Important to have the same year in report and in data!
		
		int repId = daoService.add(report);

		TableRow aggr = new TableRow(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));
		Relation.injectParent(report, aggr);
		
		aggr.put(CustomStrings.SUMMARIZED_INFO_TYPE, CustomStrings.SUMMARIZED_INFO_BSE_TYPE);
		
		String tg = "TG006A";
		String sampC = "CY";
		String sampY = "2012";
		String sampM = "2";
		String source = "F01.A056N";
		String prod = "F21.A07RV";
		String animage = "F31.A16NF";
		String sex = "F32.A0C9B";
		
		aggr.put(CustomStrings.TARGET_GROUP_COL, new TableCell(tg, ""));
		aggr.put("sampCountry", sampC);
		aggr.put("sampY", sampY);
		aggr.put("sampM", sampM);
		aggr.put(CustomStrings.SOURCE_COL, new TableCell(source, ""));
		aggr.put(CustomStrings.PROD_COL, new TableCell(prod, ""));
		aggr.put(CustomStrings.ANIMAGE_COL, new TableCell(animage, ""));
		aggr.put(CustomStrings.SEX_COL, new TableCell(sex, ""));
		
		int summId = daoService.add(aggr);
		
		String contextId = formulaService.solve(aggr, aggr.getSchema()
				.getById(CustomStrings.CONTEXT_ID_COL), XlsxHeader.LABEL_FORMULA);
		
		String hashAlgorithm = "MD5";
		String value = tg + sampC + sampY + sampM + source + prod + animage;
		
		byte[] byteArray = value.getBytes();
		byte[] digest;
		digest = MessageDigest.getInstance(hashAlgorithm).digest(byteArray);
		
		String hash = DatatypeConverter.printHexBinary(digest);
		
		assertEquals(hash, contextId);
	}
	
	@Test
	public void contextIdForAggregatedBSEForNONExceptionalCountry() throws NoSuchAlgorithmException, FormulaException {
		
		TableRow pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		TableRow settings = RowCreatorMock.genRandSettings();
		int setId = daoService.add(settings);
		
		TableRow report = RowCreatorMock.genRandReport(prefId);
		report.put(CustomStrings.REPORT_COUNTRY, "CY");
		report.put(CustomStrings.EXCEPTION_COUNTRY_COL, "No");
		report.put(AppPaths.REPORT_YEAR_COL, "2012"); // Important to have the same year in report and in data!
		
		int repId = daoService.add(report);

		TableRow aggr = new TableRow(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));
		Relation.injectParent(report, aggr);
		
		aggr.put(CustomStrings.SUMMARIZED_INFO_TYPE, CustomStrings.SUMMARIZED_INFO_BSE_TYPE);
		
		String tg = "TG006A";
		String sampC = "CY";
		String sampY = "2012";
		String sampM = "2";
		String source = "F01.A056N";
		String prod = "F21.A07RV";
		String animage = "F31.A16NF";
		String sex = "F32.A0C9B";
		
		aggr.put(CustomStrings.TARGET_GROUP_COL, new TableCell(tg, ""));
		aggr.put("sampCountry", sampC);
		aggr.put("sampY", sampY);
		aggr.put("sampM", sampM);
		aggr.put(CustomStrings.SOURCE_COL, new TableCell(source, ""));
		aggr.put(CustomStrings.PROD_COL, new TableCell(prod, ""));
		aggr.put(CustomStrings.ANIMAGE_COL, new TableCell(animage, ""));
		aggr.put(CustomStrings.SEX_COL, new TableCell(sex, ""));
		
		int summId = daoService.add(aggr);
		
		String contextId = formulaService.solve(aggr, aggr.getSchema()
				.getById(CustomStrings.CONTEXT_ID_COL), XlsxHeader.LABEL_FORMULA);
		
		String hashAlgorithm = "MD5";
		String value = tg + sampC + sampY + sampM + source + prod + animage;
		
		byte[] byteArray = value.getBytes();
		byte[] digest;
		digest = MessageDigest.getInstance(hashAlgorithm).digest(byteArray);
		
		String hash = DatatypeConverter.printHexBinary(digest);
		
		assertEquals(hash, contextId);
	}
	
	@Test
	public void computeContextIdForAnalyticalResultForCWDNONExceptionalCountry() 
			throws NoSuchAlgorithmException, ParseException, FormulaException {
		
		TableRow pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		TableRow report = RowCreatorMock.genRandReport(prefId);
		report.put(CustomStrings.REPORT_COUNTRY, "CY");
		report.put(CustomStrings.EXCEPTION_COUNTRY_COL, "No");
		report.put(AppPaths.REPORT_YEAR_COL, "2012"); // Important to have the same year in report and in data!
		
		int repId = daoService.add(report);
		
		TableRow result = new TableRow(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
		Relation.injectParent(report, result);
		
		String tg = "TG006A";
		String sampC = "CY";
		String sampY = "2012";
		String sampM = "2";
		String source = "F01.A056N";
		String prod = "F21.A07RV";
		String animage = "F31.A16NJ";
		String sex = "F32.A0C9B";
		String psuId = "mypsuID";
		
		result.put(CustomStrings.TARGET_GROUP_COL, new TableCell(tg, ""));
		result.put("sampCountry", sampC);
		result.put("sampY", sampY);
		result.put("sampM", sampM);
		result.put(CustomStrings.SOURCE_COL, new TableCell(source, ""));
		result.put(CustomStrings.PROD_COL, new TableCell(prod, ""));
		result.put(CustomStrings.ANIMAGE_COL, new TableCell(animage, ""));
		result.put(CustomStrings.SEX_COL, new TableCell(sex, ""));
		result.put(CustomStrings.PSU_ID_COL, psuId);
		
		String sampMatCode = "A04MQ#";
		
		for (String facet: new String[] {source, prod, animage, sex})
			sampMatCode = sampMatCode + "$" + facet;
		
		result.put(CustomStrings.SAMP_MAT_CODE_COL, sampMatCode);
		result.put(CustomStrings.PROG_ID_COL, "tseTargetGroup=" + tg);
		result.put(CustomStrings.SAMP_UNIT_IDS_COL, "PSUId=" + psuId);
		
		String hashAlgorithm = "MD5";
		String value = tg + sampC + sampY + sampM + source + prod + animage;
		
		byte[] byteArray = value.getBytes();
		byte[] digest;
		digest = MessageDigest.getInstance(hashAlgorithm).digest(byteArray);
		
		String hash = DatatypeConverter.printHexBinary(digest);
		
		assertEquals(hash, reportService.getContextIdFrom(result));
	}
	
	@Test
	public void computeContextIdForAnalyticalResultForCWDExceptionalCountry() 
			throws NoSuchAlgorithmException, ParseException, FormulaException {
		
		TableRow pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		TableRow report = RowCreatorMock.genRandReport(prefId);
		report.put(CustomStrings.REPORT_COUNTRY, "EE");
		report.put(CustomStrings.EXCEPTION_COUNTRY_COL, "Yes");
		report.put(AppPaths.REPORT_YEAR_COL, "2012"); // Important to have the same year in report and in data!
		
		int repId = daoService.add(report);
		
		TableRow result = new TableRow(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
		Relation.injectParent(report, result);
		
		String tg = "TG001A";
		String sampC = "CY";
		String sampY = "2012";
		String sampM = "2";
		String source = "F01.A056N";
		String prod = "F21.A07RV";
		String animage = "F31.A16NJ";
		String sex = "F32.A0C8Z";
		String psuId = "mypsuID";
		
		result.put(CustomStrings.TARGET_GROUP_COL, new TableCell(tg, ""));
		result.put("sampCountry", sampC);
		result.put("sampY", sampY);
		result.put("sampM", sampM);
		result.put(CustomStrings.SOURCE_COL, new TableCell(source, ""));
		result.put(CustomStrings.PROD_COL, new TableCell(prod, ""));
		result.put(CustomStrings.ANIMAGE_COL, new TableCell(animage, ""));
		result.put(CustomStrings.SEX_COL, new TableCell(sex, ""));
		result.put(CustomStrings.PSU_ID_COL, psuId);
		
		String sampMatCode = "A04MQ#";
		
		for (String facet: new String[] {source, prod, animage, sex})
			sampMatCode = sampMatCode + "$" + facet;
		
		result.put(CustomStrings.SAMP_MAT_CODE_COL, sampMatCode);
		result.put(CustomStrings.PROG_ID_COL, "tseTargetGroup=" + tg);
		result.put(CustomStrings.SAMP_UNIT_IDS_COL, "PSUId=" + psuId);
		
		String hashAlgorithm = "MD5";
		String value = tg + sampC + sampY + sampM + source + prod + animage + sex + psuId;
		
		byte[] byteArray = value.getBytes();
		byte[] digest;
		digest = MessageDigest.getInstance(hashAlgorithm).digest(byteArray);
		
		String hash = DatatypeConverter.printHexBinary(digest);
		
		assertEquals(hash, reportService.getContextIdFrom(result));
	}
	
	@Test
	public void computeContextIdForAnalyticalResultForSCRAPIEExceptionCountry() 
			throws NoSuchAlgorithmException, ParseException, FormulaException {
		
		TableRow pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		TableRow report = RowCreatorMock.genRandReport(prefId);
		report.put(CustomStrings.REPORT_COUNTRY, "EE");
		report.put(CustomStrings.EXCEPTION_COUNTRY_COL, "Yes");
		report.put(AppPaths.REPORT_YEAR_COL, "2012"); // Important to have the same year in report and in data!
		
		int repId = daoService.add(report);
		
		TableRow result = new TableRow(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
		Relation.injectParent(report, result);
		
		String tg = "TG001A";
		String sampC = "CY";
		String sampY = "2012";
		String sampM = "2";
		String source = "F01.A057G";  // sheep
		String prod = "F21.A07RV";
		String animage = "F31.A16NJ";
		String sex = "F32.A0C8Z";  // present but not considered in context
		
		result.put(CustomStrings.TARGET_GROUP_COL, tg);
		result.put("sampCountry", sampC);
		result.put("sampY", sampY);
		result.put("sampM", sampM);
		result.put("source", source);
		result.put("prod", prod);
		result.put("animage", animage);
		result.put("sex", sex);
		
		String sampMatCode = "A04MQ#";
		
		for (String facet: new String[] {source, prod, animage, sex})
			sampMatCode = sampMatCode + "$" + facet;
		
		result.put(CustomStrings.SAMP_MAT_CODE_COL, sampMatCode);
		result.put(CustomStrings.PROG_ID_COL, "tseTargetGroup=" + tg);
		
		String hashAlgorithm = "MD5";
		String value = tg + sampC + sampY + sampM + source + prod + animage;
		
		byte[] byteArray = value.getBytes();
		byte[] digest;
		digest = MessageDigest.getInstance(hashAlgorithm).digest(byteArray);
		
		String hash = DatatypeConverter.printHexBinary(digest);
		
		assertEquals(hash, reportService.getContextIdFrom(result));
	}
	
	@Test
	public void computeContextIdForAnalyticalResultForSCRAPIENONExceptionCountry() 
			throws NoSuchAlgorithmException, ParseException, FormulaException {
		
		TableRow pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		TableRow report = RowCreatorMock.genRandReport(prefId);
		report.put(CustomStrings.REPORT_COUNTRY, "CY");
		report.put(CustomStrings.EXCEPTION_COUNTRY_COL, "No");
		report.put(AppPaths.REPORT_YEAR_COL, "2012"); // Important to have the same year in report and in data!
		
		int repId = daoService.add(report);
		
		TableRow result = new TableRow(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
		Relation.injectParent(report, result);
		
		String tg = "TG001A";
		String sampC = "CY";
		String sampY = "2012";
		String sampM = "2";
		String source = "F01.A057G";  // sheep
		String prod = "F21.A07RV";
		String animage = "F31.A16NJ";
		String sex = "F32.A0C8Z";  // present but not considered in context
		
		result.put(CustomStrings.TARGET_GROUP_COL, tg);
		result.put("sampCountry", sampC);
		result.put("sampY", sampY);
		result.put("sampM", sampM);
		result.put("source", source);
		result.put("prod", prod);
		result.put("animage", animage);
		result.put("sex", sex);
		
		String sampMatCode = "A04MQ#";
		
		for (String facet: new String[] {source, prod, animage, sex})
			sampMatCode = sampMatCode + "$" + facet;
		
		result.put(CustomStrings.SAMP_MAT_CODE_COL, sampMatCode);
		result.put(CustomStrings.PROG_ID_COL, "tseTargetGroup=" + tg);
		
		String hashAlgorithm = "MD5";
		String value = tg + sampC + sampY + sampM + source + prod + animage;
		
		byte[] byteArray = value.getBytes();
		byte[] digest;
		digest = MessageDigest.getInstance(hashAlgorithm).digest(byteArray);
		
		String hash = DatatypeConverter.printHexBinary(digest);
		
		assertEquals(hash, reportService.getContextIdFrom(result));
	}
}

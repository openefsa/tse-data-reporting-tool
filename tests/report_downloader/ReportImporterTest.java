package report_downloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Test;

import app_config.AppPaths;
import dataset.Dataset;
import dataset.DatasetList;
import dataset.DcfDatasetStatus;
import dataset.IDataset;
import dataset.NoAttachmentException;
import formula.FormulaException;
import mocks.TableDaoMock;
import providers.FormulaService;
import providers.IFormulaService;
import providers.ITableDaoService;
import providers.TableDaoService;
import providers.TseReportService;
import soap.DetailedSOAPException;
import soap_test.GetAckMock;
import soap_test.GetDatasetMock;
import soap_test.GetDatasetsListMock;
import soap_test.SendMessageMock;
import table_relations.Relation;
import table_skeleton.TableRow;
import table_skeleton.TableRowList;
import tse_config.CustomStrings;
import xlsx_reader.TableSchemaList;

public class ReportImporterTest {
	
	private TseReportService reportService;
	private GetAckMock getAck;
	private GetDatasetsListMock<IDataset> getDatasetsList;
	private SendMessageMock sendMessage;
	private GetDatasetMock getDataset;
	private ITableDaoService daoService;
	private IFormulaService formulaService;
	
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
	}
	
	@Test
	public void importFirstVersionOfReport() throws DetailedSOAPException, XMLStreamException, 
		IOException, FormulaException, NoAttachmentException, ParseException {
		
		String datasetId = "11920";
		
		getDataset.addDatasetFile(datasetId, new File("test-files" 
				+ System.getProperty("file.separator") + "dataset-first-version.xml"));
		
		DatasetList datasetVersions = new DatasetList();
		
		Dataset d = new Dataset();
		
		// note: these information come from the get datasets list
		// in the normal process flow
		d.setStatus(DcfDatasetStatus.ACCEPTED_DWH);
		d.setId(datasetId);
		d.setSenderId("AT1706.00");
		
		datasetVersions.add(d);
		
		// import the file
		TseReportImporter imp = new TseReportImporter(reportService, daoService);
		imp.setDatasetVersions(datasetVersions);
		imp.importReport();
		
		// check contents of the file with what was imported
		assertEquals(1, daoService.getAll(TableSchemaList.getByName(AppPaths.REPORT_SHEET)).size());
		assertEquals(3, daoService.getAll(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET)).size());
		assertEquals(3, daoService.getAll(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET)).size());
		assertEquals(5, daoService.getAll(TableSchemaList.getByName(CustomStrings.RESULT_SHEET)).size());
		
		// get the objects
		TableRow report = daoService.getAll(TableSchemaList.getByName(AppPaths.REPORT_SHEET)).iterator().next();
		TableRowList summInfos = daoService.getAll(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));
		TableRowList cases = daoService.getAll(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));
		TableRowList results = daoService.getAll(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
		
		int siIdForCheckingCase = -1;
		for(TableRow si: summInfos) {
			
			// under the same report
			assertEquals(String.valueOf(report.getDatabaseId()), si.getCode(CustomStrings.REPORT_ID_COL));
			
			// check contents
			if (si.getLabel(CustomStrings.RES_ID_COL).equals("0404_000069.0")) {
				siIdForCheckingCase = si.getDatabaseId();
				assertEquals("0404_000069", si.getLabel(CustomStrings.PROG_ID_COL));
			}
			if (si.getLabel(CustomStrings.RES_ID_COL).equals("0404_000068.0")) {
				assertEquals("0404_000068", si.getLabel(CustomStrings.PROG_ID_COL));
			}
		}
		
		for(TableRow c: cases) {
			
			assertNotNull(c.getCode(CustomStrings.PART_COL));
			if (c.getLabel(Relation.foreignKeyFromParent(CustomStrings.SUMMARIZED_INFO_SHEET))
					.equals(String.valueOf(siIdForCheckingCase))) {
				
				assertEquals("F02.A06AM", c.getCode(CustomStrings.PART_COL));
			}
		}
		//TODO check other contents
	}
	
	@Test
	public void importFirstVersionOfReport2() throws DetailedSOAPException, XMLStreamException, 
		IOException, FormulaException, NoAttachmentException, ParseException {
		
		String datasetId = "12648";
		
		getDataset.addDatasetFile(datasetId, new File("test-files" 
				+ System.getProperty("file.separator") + "LT1704.00.xml"));
		
		DatasetList datasetVersions = new DatasetList();
		
		Dataset d = new Dataset();
		
		// note: these information come from the get datasets list
		// in the normal process flow
		d.setStatus(DcfDatasetStatus.VALID);
		d.setId(datasetId);
		d.setSenderId("LT1704.00");
		
		datasetVersions.add(d);
		
		// import the file
		TseReportImporter imp = new TseReportImporter(reportService, daoService);
		imp.setDatasetVersions(datasetVersions);
		imp.importReport();
		
		// check contents of the file with what was imported
		assertEquals(1, daoService.getAll(TableSchemaList.getByName(AppPaths.REPORT_SHEET)).size());
		assertEquals(2, daoService.getAll(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET)).size());
		assertEquals(3, daoService.getAll(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET)).size());
		assertEquals(6, daoService.getAll(TableSchemaList.getByName(CustomStrings.RESULT_SHEET)).size());
	}
	
	@Test
	public void importFirstVersionWithOnlyOneRGT() throws DetailedSOAPException, 
		XMLStreamException, IOException, FormulaException, NoAttachmentException, ParseException {
		
		String datasetId = "11920";
		
		getDataset.addDatasetFile(datasetId, new File("test-files" 
				+ System.getProperty("file.separator") + "RGT-import-test.xml"));
		
		DatasetList datasetVersions = new DatasetList();
		
		Dataset d = new Dataset();
		
		// note: these information come from the get datasets list
		// in the normal process flow
		d.setStatus(DcfDatasetStatus.ACCEPTED_DWH);
		d.setId(datasetId);
		d.setSenderId("AT1706.00");
		
		datasetVersions.add(d);
		
		// import the file
		TseReportImporter imp = new TseReportImporter(reportService, daoService);
		imp.setDatasetVersions(datasetVersions);
		imp.importReport();
		
		assertEquals(1, daoService.getAll(TableSchemaList.getByName(AppPaths.REPORT_SHEET)).size());
		assertEquals(1, daoService.getAll(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET)).size());
		assertEquals(1, daoService.getAll(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET)).size());
		assertEquals(1, daoService.getAll(TableSchemaList.getByName(CustomStrings.RESULT_SHEET)).size());
		
		TableRowList cases = daoService.getAll(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));
		
		for (TableRow c: cases) {
			String animage = c.getCode(CustomStrings.ANIMAGE_COL);
			assertEquals("F31.A16ND", animage);
			
			// NO sex
			assertTrue(c.getCode(CustomStrings.SEX_COL).isEmpty());
		}
	}
	
	@Test
	public void importFirstVersionOfReportWithAllDataTypes() throws DetailedSOAPException, XMLStreamException, 
		IOException, FormulaException, NoAttachmentException, ParseException {

		String datasetId = "12288";
		
		getDataset.addDatasetFile(datasetId, new File("test-files" 
				+ System.getProperty("file.separator") + "import-all-filled.xml"));
		
		DatasetList datasetVersions = new DatasetList();
		
		Dataset d = new Dataset();
		
		// note: these information come from the get datasets list
		// in the normal process flow
		d.setStatus(DcfDatasetStatus.VALID);
		d.setId(datasetId);
		d.setSenderId("CY1803.00");
		
		datasetVersions.add(d);
		
		// import the file
		TseReportImporter imp = new TseReportImporter(reportService, daoService);
		imp.setDatasetVersions(datasetVersions);
		imp.importReport();
		
		assertEquals(1, daoService.getAll(TableSchemaList.getByName(AppPaths.REPORT_SHEET)).size());
		assertEquals(5, daoService.getAll(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET)).size());
		
		TableRowList cases = daoService.getAll(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));
		
		// breed and eval com are filled ?
		for (TableRow c: cases) {
			
			// skip negative samples
			if (c.getCode(CustomStrings.SAMP_EVENT_ASSES_COL).equals(CustomStrings.DEFAULT_ASSESS_NEG_CASE_CODE))
				continue;
			
			String breed = c.getCode(CustomStrings.BREED_COL);
			assertFalse(breed.isEmpty());
			String evalCom = c.getCode(CustomStrings.EVAL_COMMENT_COL);
			assertFalse(evalCom.isEmpty());
		}
	}
	
	@Test
	public void importFirstVersionCheckSexAndPsuId() throws DetailedSOAPException, XMLStreamException, 
		IOException, FormulaException, NoAttachmentException, ParseException {
		
		String datasetId = "12279";
		
		getDataset.addDatasetFile(datasetId, new File("test-files" 
				+ System.getProperty("file.separator") + "import-test-cyprus.xml"));
		
		DatasetList datasetVersions = new DatasetList();
		
		Dataset d = new Dataset();
		
		// note: these information come from the get datasets list
		// in the normal process flow
		d.setStatus(DcfDatasetStatus.VALID);
		d.setId(datasetId);
		d.setSenderId("CY1202.00");
		
		datasetVersions.add(d);
		
		// import the file
		TseReportImporter imp = new TseReportImporter(reportService, daoService);
		imp.setDatasetVersions(datasetVersions);
		imp.importReport();
		
		// check contents of the file with what was imported
		assertEquals(1, daoService.getAll(TableSchemaList.getByName(AppPaths.REPORT_SHEET)).size());
		assertEquals(4, daoService.getAll(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET)).size());

		// get the objects
		TableRow report = daoService.getAll(TableSchemaList.getByName(AppPaths.REPORT_SHEET)).iterator().next();
		TableRowList summInfos = daoService.getAll(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));
		TableRowList cases = daoService.getAll(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));
		TableRowList results = daoService.getAll(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
		
		// psu id and sex must be set for CWD
		for (TableRow si: summInfos) {

			if (si.getCode(CustomStrings.SUMMARIZED_INFO_TYPE)
					.equals(CustomStrings.SUMMARIZED_INFO_CWD_TYPE)) {
				String psuId = si.getCode(CustomStrings.PSU_ID_COL);
				assertTrue(psuId != null && !psuId.isEmpty());
				
				String sex = si.getCode(CustomStrings.SEX_COL);
				assertTrue(sex != null && !sex.isEmpty());
			}
		}
		
		for (TableRow c: cases) {
			TableRow si = daoService.getById(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET), 
					c.getNumCode(Relation.foreignKeyFromParent(CustomStrings.SUMMARIZED_INFO_SHEET)));
			
			// for non CWD, sex is optional
			if (!si.getCode(CustomStrings.SUMMARIZED_INFO_TYPE).equals(CustomStrings.SUMMARIZED_INFO_CWD_TYPE)) {
				
				// we know in the dataset this element has the sex
				if (c.getCode(CustomStrings.SAMPLE_ID_COL).equals("ofiasdfijdsiofds")) {
					String sex = c.getCode(CustomStrings.SEX_COL);
					assertTrue(sex != null && !sex.isEmpty());
				}
			}
		}
	}
	
	@Test
	public void importNonFirstVersionNonAcceptedDwhState() throws DetailedSOAPException, XMLStreamException, 
		IOException, FormulaException, NoAttachmentException, ParseException {
		
		String firstVersionId = "11513";
		String amendedVersionId = "11518";
		
		// prepare the get dataset request
		getDataset.addDatasetFile(firstVersionId, new File("test-files" 
				+ System.getProperty("file.separator") + "BE1011.00-amtype.xml"));
		
		getDataset.addDatasetFile(amendedVersionId, new File("test-files" 
				+ System.getProperty("file.separator") + "BE1011.01-amtype.xml"));
		
		DatasetList datasetVersions = new DatasetList();
		
		// note: these information come from the get datasets list
		// in the normal process flow
		Dataset d = new Dataset();
		d.setStatus(DcfDatasetStatus.ACCEPTED_DWH);
		d.setId(firstVersionId);
		d.setSenderId("BE1011.00");
		
		Dataset d2 = new Dataset();
		d2.setStatus(DcfDatasetStatus.ACCEPTED_DWH);
		d2.setId(amendedVersionId);
		d2.setSenderId("BE1011.01");
		
		datasetVersions.add(d);
		datasetVersions.add(d2);
		
		// import the file
		TseReportImporter imp = new TseReportImporter(reportService, daoService);
		imp.setDatasetVersions(datasetVersions);
		imp.importReport();

		// check contents of the file with what was imported
		// only the merged version is present
		assertEquals(1, daoService.getAll(TableSchemaList.getByName(AppPaths.REPORT_SHEET)).size());
		assertEquals(6, daoService.getAll(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET)).size());
		assertEquals(17, daoService.getAll(TableSchemaList.getByName(CustomStrings.RESULT_SHEET)).size());
	}
	
	@Test
	public void importNonFirstVersionInAcceptedDwhState() throws DetailedSOAPException, XMLStreamException, 
		IOException, FormulaException, NoAttachmentException, ParseException {
		
		String firstVersionId = "11513";
		String amendedVersionId = "11518";
		
		// prepare the get dataset request
		getDataset.addDatasetFile(firstVersionId, new File("test-files" 
				+ System.getProperty("file.separator") + "BE1011.00.xml"));
		
		getDataset.addDatasetFile(amendedVersionId, new File("test-files" 
				+ System.getProperty("file.separator") + "BE1011.01.xml"));
		
		DatasetList datasetVersions = new DatasetList();
		
		// note: these information come from the get datasets list
		// in the normal process flow
		Dataset d = new Dataset();
		d.setStatus(DcfDatasetStatus.ACCEPTED_DWH);
		d.setId(firstVersionId);
		d.setSenderId("BE1011.00");
		
		Dataset d2 = new Dataset();
		d2.setStatus(DcfDatasetStatus.ACCEPTED_DWH);
		d2.setId(amendedVersionId);
		d2.setSenderId("BE1011.01");
		
		datasetVersions.add(d);
		datasetVersions.add(d2);
		
		// import the file
		TseReportImporter imp = new TseReportImporter(reportService, daoService);
		imp.setDatasetVersions(datasetVersions);
		imp.importReport();

		// check contents of the file with what was imported
		// only the merged version is present
		assertEquals(1, daoService.getAll(TableSchemaList.getByName(AppPaths.REPORT_SHEET)).size());
		assertEquals(6, daoService.getAll(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET)).size());
		assertEquals(17, daoService.getAll(TableSchemaList.getByName(CustomStrings.RESULT_SHEET)).size());
	}
}

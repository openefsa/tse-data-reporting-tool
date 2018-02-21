package tse_main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

import mocks.RowCreatorMock;
import mocks.TableDaoMock;
import providers.FormulaService;
import providers.IFormulaService;
import providers.TableDaoService;
import table_skeleton.TableRow;
import table_skeleton.TableRowList;
import tse_analytical_result.AnalyticalResult;
import tse_case_report.CaseReport;
import tse_config.CustomStrings;
import tse_report.TseReport;
import tse_summarized_information.SummarizedInfo;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class TseSummarizedInfoImporterTest {

	private TableDaoService daoService;
	private TseSummarizedInfoImporter imp;
	
	@Before
	public void init() {
		this.daoService = new TableDaoService(new TableDaoMock());
		IFormulaService formulaService = new FormulaService(daoService);
		this.imp = new TseSummarizedInfoImporter(daoService, formulaService);
	}
	
	@Test
	public void checkSummarizedInformationCorrectness() {
		
		// create a report
		TableRow pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		TableRow opt = RowCreatorMock.genRandSettings();
		int optId = daoService.add(opt);
		
		TseReport report = RowCreatorMock.genRandReport(prefId);
		
		int reportId = daoService.add(report);
		
		SummarizedInfo si = RowCreatorMock.genRandSummInfo(reportId, optId, prefId);
		
		int siId = daoService.add(si);
		
		CaseReport cr = RowCreatorMock.genRandCase(reportId, siId, optId, siId);
		
		int caseId = daoService.add(cr);
		
		AnalyticalResult result = RowCreatorMock.genRandResult(reportId, siId, caseId, optId, prefId);
		daoService.add(result);
		
		// where to copy
		TseReport reportTarget = RowCreatorMock.genRandReport(prefId);
		daoService.add(reportTarget);
		
		// copy summ info
		TableSchema childSchema = TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET);

		// perform copy
		imp.copyByParent(childSchema, report, reportTarget);
		
		TableRowList copiedSummInfo = daoService.getByParentId(childSchema, reportTarget.getSchema().getSheetName(), 
				reportTarget.getDatabaseId(), false);
		
		assertEquals(1, copiedSummInfo.size());
		
		TableRow copiedSi = copiedSummInfo.iterator().next();
		
		// declared samples should be reset
		assertEquals("0", copiedSi.getCode(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES));
		assertEquals("0", copiedSi.getCode(CustomStrings.SUMMARIZED_INFO_NEG_SAMPLES));
		assertEquals("0", copiedSi.getCode(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES));
		assertEquals("0", copiedSi.getCode(CustomStrings.SUMMARIZED_INFO_TOT_SAMPLES));
		assertEquals("0", copiedSi.getCode(CustomStrings.SUMMARIZED_INFO_UNS_SAMPLES));
		
		// different res id (it was reinitialized)
		assertFalse(si.getCode(CustomStrings.RES_ID_COLUMN).equals(copiedSi.getCode(CustomStrings.RES_ID_COLUMN)));
		
		// different prog id (it was reinitialized)
		assertFalse(si.getCode(CustomStrings.RESULT_PROG_ID).equals(copiedSi.getCode(CustomStrings.RESULT_PROG_ID)));
	}
}

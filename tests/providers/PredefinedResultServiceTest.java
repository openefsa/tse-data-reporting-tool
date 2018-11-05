package providers;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import mocks.RowCreatorMock;
import mocks.TableDaoMock;
import table_skeleton.TableCell;
import table_skeleton.TableRow;
import table_skeleton.TableRowList;
import tse_case_report.CaseReport;
import tse_config.CustomStrings;
import tse_report.TseReport;
import tse_summarized_information.SummarizedInfo;

public class PredefinedResultServiceTest {

	private PredefinedResultService resultService;
	private ITableDaoService daoService;
	private IFormulaService formulaService;

	private TableRow pref;
	private TableRow opt;
	private TseReport report;
	private SummarizedInfo si;
	private CaseReport cr;
	
	@Before
	public void init() {
		
		this.daoService = new TableDaoService(new TableDaoMock());
		this.formulaService = new FormulaService(daoService);
		this.resultService = new PredefinedResultService(daoService, formulaService);
		
		pref = RowCreatorMock.genRandPreferences();
		int prefId = daoService.add(pref);
		
		opt = RowCreatorMock.genRandSettings();
		int optId = daoService.add(opt);
		
		report = RowCreatorMock.genRandReport(prefId);
		report.setYear("2005");
		report.setMonth("5");
		
		int reportId = daoService.add(report);
		
		si = RowCreatorMock.genRandSummInfo(reportId, optId, prefId);
		si.put(CustomStrings.ANIMAGE_COL, new TableCell("F31.A16NK", ""));  // < 24 months
		
		int siId = daoService.add(si);
		
		cr = RowCreatorMock.genRandCase(reportId, siId, optId, siId);
		cr.put(CustomStrings.BIRTH_MONTH_COL, "6");
		cr.put(CustomStrings.BIRTH_YEAR_COL, "2004");
		
		daoService.add(cr);
	}
	
	@Test
	public void defaultResultsForBSENoConfirmatoryCBSE() throws IOException {
		
		pref.put(CustomStrings.PREFERENCES_SCREENING_BSE, new TableCell("F639A", ""));
		pref.put(CustomStrings.PREFERENCES_DISCRIMINATORY_BSE, new TableCell("F658A", ""));
		
		// BSE
		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, new TableCell(CustomStrings.SUMMARIZED_INFO_BSE_TYPE, ""));
		
		// C-BSE asses
		cr.put(CustomStrings.SAMP_EVENT_ASSES_COL, new TableCell(CustomStrings.DEFAULT_ASSESS_CBSE_CASE_CODE, ""));
		
		TableRowList results = resultService.createDefaultResults(report, si, cr);
		
		assertEquals(2, results.size());
	}
	
	
	@Test
	public void defaultResultsForBSEConfirmatoryCBSE() throws IOException {
		
		// put confirmatory
		pref.put(CustomStrings.PREFERENCES_CONFIRMATORY_BSE, new TableCell("F626A", ""));
		pref.put(CustomStrings.PREFERENCES_SCREENING_BSE, new TableCell("F639A", ""));
		pref.put(CustomStrings.PREFERENCES_DISCRIMINATORY_BSE, new TableCell("F658A", ""));
		
		// BSE
		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, new TableCell(CustomStrings.SUMMARIZED_INFO_BSE_TYPE, ""));
		
		// C-BSE asses
		cr.put(CustomStrings.SAMP_EVENT_ASSES_COL, new TableCell(CustomStrings.DEFAULT_ASSESS_CBSE_CASE_CODE, ""));
		
		TableRowList results = resultService.createDefaultResults(report, si, cr);
		
		assertEquals(3, results.size());
	}
	
	@Test
	public void defaultResultsForSheepConfirmatoryInconclusive() throws IOException {
		
		// put confirmatory
		pref.put(CustomStrings.PREFERENCES_CONFIRMATORY_SCRAPIE, new TableCell("F626A", ""));
		pref.put(CustomStrings.PREFERENCES_SCREENING_SCRAPIE, new TableCell("F639A", ""));
		pref.put(CustomStrings.PREFERENCES_DISCRIMINATORY_SCRAPIE, new TableCell("F659A", ""));
		
		// scrapie
		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, new TableCell(CustomStrings.SUMMARIZED_INFO_SCRAPIE_TYPE, ""));
		si.put(CustomStrings.SOURCE_COL, new TableCell(CustomStrings.SOURCE_SHEEP_CODE, ""));
		
		// inc asses
		cr.put(CustomStrings.SAMP_EVENT_ASSES_COL, new TableCell(CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE, ""));
		
		TableRowList results = resultService.createDefaultResults(report, si, cr);
		
		assertEquals(4, results.size());
	}
	
	@Test
	public void defaultResultsForGoatConfirmatoryInconclusive() throws IOException {
		
		// put confirmatory
		pref.put(CustomStrings.PREFERENCES_CONFIRMATORY_SCRAPIE, new TableCell("F626A", ""));
		pref.put(CustomStrings.PREFERENCES_SCREENING_SCRAPIE, new TableCell("F639A", ""));
		pref.put(CustomStrings.PREFERENCES_DISCRIMINATORY_SCRAPIE, new TableCell("F659A", ""));
		
		// scrapie
		si.put(CustomStrings.SUMMARIZED_INFO_TYPE, new TableCell(CustomStrings.SUMMARIZED_INFO_SCRAPIE_TYPE, ""));
		si.put(CustomStrings.SOURCE_COL, new TableCell(CustomStrings.SOURCE_GOAT_CODE, ""));
		
		// inc asses
		cr.put(CustomStrings.SAMP_EVENT_ASSES_COL, new TableCell(CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE, ""));
		
		TableRowList results = resultService.createDefaultResults(report, si, cr);
		
		assertEquals(3, results.size());
	}
}

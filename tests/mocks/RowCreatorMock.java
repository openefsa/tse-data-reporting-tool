package mocks;

import dataset.RCLDatasetStatus;
import table_skeleton.TableCell;
import table_skeleton.TableRow;
import table_skeleton.TableVersion;
import tse_analytical_result.AnalyticalResult;
import tse_case_report.CaseReport;
import tse_config.CustomStrings;
import tse_report.TseReport;
import tse_summarized_information.SummarizedInfo;
import xlsx_reader.TableSchemaList;

public class RowCreatorMock {

	public static TableRow genRandSettings() {
		
		TableRow settings = new TableRow(TableSchemaList.getByName(CustomStrings.SETTINGS_SHEET));
		
		settings.put(CustomStrings.SETTINGS_USERNAME, "avonva");
		settings.put(CustomStrings.SETTINGS_PASSWORD, "Ab123456");
		settings.put(CustomStrings.SETTINGS_ORG_CODE, "EFSA");
		
		return settings;
	}
	
	public static TableRow genRandPreferences() {
		
		TableRow pref = new TableRow(TableSchemaList.getByName(CustomStrings.PREFERENCES_SHEET));
		
		pref.put(CustomStrings.PREFERENCES_COUNTRY, "AT");
		
		pref.put(CustomStrings.PREFERENCES_SCREENING_BSE, "F639A");
		pref.put(CustomStrings.PREFERENCES_SCREENING_SCRAPIE, "F639A");
		pref.put(CustomStrings.PREFERENCES_SCREENING_CWD, "F080A");
		
		pref.put(CustomStrings.PREFERENCES_DISCRIMINATORY_BSE, "F664A");
		pref.put(CustomStrings.PREFERENCES_DISCRIMINATORY_SCRAPIE, "F664A");
		pref.put(CustomStrings.PREFERENCES_DISCRIMINATORY_CWD, "F659A");
		
		return pref;
	}
	
	public static TseReport genRandReport(int prefId) {
		
		TseReport report = new TseReport();
		report.setSenderId("AT0404");
		report.setVersion(TableVersion.getFirstVersion());
		report.setYear("2004");
		report.setMonth("4");
		report.setCountry("AT");
		report.setId("12342");
		report.setSchema(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
		report.setStatus(RCLDatasetStatus.DRAFT);
		
		report.put(CustomStrings.PREFERENCES_ID_COL, String.valueOf(prefId));
		
		return report;
	}
	
	public static SummarizedInfo genRandSummInfo(int reportId, int settingsId, int prefId) {
		
		SummarizedInfo summInfo = new SummarizedInfo(CustomStrings.SUMMARIZED_INFO_TYPE, 
				CustomStrings.SUMMARIZED_INFO_BSE_TYPE);
		
		summInfo.put(CustomStrings.REPORT_ID_COL, String.valueOf(reportId));
		summInfo.put(CustomStrings.SETTINGS_ID_COL, String.valueOf(settingsId));
		summInfo.put(CustomStrings.PREFERENCES_ID_COL, String.valueOf(prefId));

		summInfo.put(CustomStrings.SUMMARIZED_INFO_SOURCE, "F01.A057A");
		summInfo.put(CustomStrings.SUMMARIZED_INFO_PROD, "F21.A07RV");
		summInfo.put(CustomStrings.SUMMARIZED_INFO_AGE, "F31.A16NK");
		summInfo.put(CustomStrings.SUMMARIZED_INFO_TARGET_GROUP, "TG001A");
		summInfo.put(CustomStrings.SUMMARIZED_INFO_NEG_SAMPLES, "1");
		summInfo.put(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES, "0");
		summInfo.put(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES, "0");
		summInfo.put(CustomStrings.SUMMARIZED_INFO_UNS_SAMPLES, "0");
		summInfo.put(CustomStrings.SUMMARIZED_INFO_TOT_SAMPLES, "1");
		
		// random res id
		summInfo.put(CustomStrings.RES_ID_COLUMN, Long.toHexString(Double.doubleToLongBits(Math.random())));
		
		return summInfo;
	}
	
	public static CaseReport genRandCase(int reportId, int summInfoId, int settingsId, int prefId) {
		
		CaseReport caseReport = new CaseReport();
		
		caseReport.put(CustomStrings.REPORT_ID_COL, String.valueOf(reportId));
		caseReport.put(CustomStrings.SI_ID_COL, String.valueOf(summInfoId));
		caseReport.put(CustomStrings.SETTINGS_ID_COL, String.valueOf(settingsId));
		caseReport.put(CustomStrings.PREFERENCES_ID_COL, String.valueOf(prefId));

		caseReport.put(CustomStrings.CASE_INFO_SAMPLE_ID, "kjed9okj3e");
		caseReport.put(CustomStrings.CASE_INFO_ASSESS, new TableCell(CustomStrings.DEFAULT_ASSESS_NEG_CASE_CODE, ""));
		caseReport.put(CustomStrings.SUMMARIZED_INFO_PART, new TableCell(CustomStrings.BRAIN_CODE, ""));
		
		return caseReport;
	}
	
	public static AnalyticalResult genRandResult(int reportId, int summInfoId, int caseId, int settingsId, int prefId) {
		
		AnalyticalResult caseReport = new AnalyticalResult();
		
		caseReport.put(CustomStrings.REPORT_ID_COL, String.valueOf(reportId));
		caseReport.put(CustomStrings.SI_ID_COL, String.valueOf(summInfoId));
		caseReport.put(CustomStrings.CASE_ID_COL, String.valueOf(caseId));
		caseReport.put(CustomStrings.SETTINGS_ID_COL, String.valueOf(settingsId));
		caseReport.put(CustomStrings.PREFERENCES_ID_COL, String.valueOf(prefId));

		caseReport.put(CustomStrings.PARAM_TYPE_COL, "P001A");
		caseReport.put(CustomStrings.CASE_INFO_SAMPLE_ID, "kjed9okj3e");
		caseReport.put("sampAnId", "jhgjhgjhgjhg");
		caseReport.put(CustomStrings.SUMMARIZED_INFO_PART, new TableCell(CustomStrings.BRAIN_CODE, ""));
		caseReport.put("progLegalRef", new TableCell("N123A", ""));
		
		return caseReport;
	}
}

package tse_config;

import app_config.AppPaths;

public class CustomStrings {
	
	public static final String PREDEFINED_RESULTS_FILE = AppPaths.CONFIG_FOLDER + "predefinedResults.xlsx";
	
	// sheets names
	public static final String RESULT_SHEET = "AnalyticalResults";
	public static final String CASE_INFO_SHEET = "CasesInformation";
	public static final String SUMMARIZED_INFO_SHEET = "SummarizedInformation";
	public static final String PREFERENCES_SHEET = "Preferences";
	public static final String SETTINGS_SHEET = "Settings";
	public static final String REPORT_SHEET = "Report";
	
	// variables names
	public static final String SETTINGS_USERNAME = "username";
	public static final String SETTINGS_PASSWORD = "password";
	public static final String SETTINGS_ORG_CODE = "orgCode";
	
	public static final String REPORT_COUNTRY = "country";
	
	public static final String PREFERENCES_CONFIRMATORY_BSE = "defConfirmatoryBSE";
	public static final String PREFERENCES_CONFIRMATORY_SCRAPIE = "defConfirmatorySCRAPIE";
	public static final String PREFERENCES_CONFIRMATORY_CWD = "defConfirmatoryCWD";
	
	public static final String SUMMARIZED_INFO_TOT_SAMPLES = "totSamplesTested";
	public static final String SUMMARIZED_INFO_POS_SAMPLES = "totSamplesPositive";
	public static final String SUMMARIZED_INFO_NEG_SAMPLES = "totSamplesNegative";
	public static final String SUMMARIZED_INFO_INC_SAMPLES = "totSamplesInconclusive";
	public static final String SUMMARIZED_INFO_TYPE = "type";
	public static final String SUMMARIZED_INFO_SOURCE = "source";
	
	public static final String SUMMARIZED_INFO_PART = "part";
	public static final String SUMMARIZED_INFO_PROD = "prod";
	public static final String SUMMARIZED_INFO_AGE = "animage";
	public static final String SUMMARIZED_INFO_TARGET_GROUP = "tseTargetGroup";
	public static final String SUMMARIZED_INFO_STATUS = "statusHerd";
	public static final String SUMMARIZED_INFO_TEST_TYPE = "anMethType";
	public static final String SUMMARIZED_INFO_PROG_INFO = "progInfo";
	public static final String SUMMARIZED_INFO_PROG_ID = "progId";
	public static final String SUMMARIZED_INFO_SAMP_MAT_CODE = "sampMatCode";
	
	public static final String SUMMARIZED_INFO_BSE_TYPE = "BSE";
	public static final String SUMMARIZED_INFO_SCRAPIE_TYPE = "SCRAPIE";
	public static final String SUMMARIZED_INFO_CWD_TYPE = "CWD";
	public static final String SUMMARIZED_INFO_SCREENING_TEST = "AT06A";
	public static final String SUMMARIZED_INFO_CONFIRMATORY_TEST = "AT08A";
	public static final String RESULT_DISCRIMINATORY_TEST = "AT12A";
	public static final String SUMMARIZED_INFO_MOLECULAR_TEST = "AT13A";
	
	public static final String CASE_INFO_SAMPLE_ID = "sampId";
	public static final String CASE_INFO_ANIMAL_ID = "animalId";
	public static final String CASE_INFO_CASE_ID = "tseNationalCaseId";
	public static final String CASE_INFO_ASSESS = "sampAnAsses";
	public static final String DEFAULT_ASSESS_NEG_CASE_CODE = "J051A";
	public static final String DEFAULT_ASSESS_INC_CASE_CODE = "J050A";
	public static final String DEFAULT_ASSESS_INC_CASE_LABEL = "Inconclusive";
	
	public static final String RESULT_ALLELE_1 = "allele1";
	public static final String RESULT_ALLELE_2 = "allele2";
	public static final String RESULT_TEST_TYPE = "anMethType";
	public static final String RESULT_EVAL_INFO = "evalInfo";
	public static final String RESULT_SCREENING_TEST = "AT06A";
	public static final String RESULT_PROG_ID = "progId";
	public static final String RESULT_SAMP_UNIT_IDS = "sampUnitIds";
	public static final String RESULT_SAMP_EVENT_INFO = "sampEventInfo";
	public static final String RESULT_SAMPLE_ID = "sampId";
	public static final String RESULT_SAMP_MAT_INFO = "sampMatInfo";
	public static final String RESULT_SAMP_AREA = "sampArea";
	public static final String RESULT_SAMP_DAY = "sampD";
	public static final String RESULT_TEST_RESULT = "resQualValue";
	public static final String RESULT_TEST_AIM = "testAim";
	
	public static final String PARAM_TYPE_COL = "paramType";
	public static final String PARAM_CODE_BASE_TERM_COL = "paramCodeBaseTerm";
	public static final String PARAM_CODE_COL = "paramCode";
	public static final String SUMMARIZED_INFO_PARAM_TYPE = "P003A";
	public static final String RESULT_PARAM_TYPE = "P001A";
	
	private static final String COUNTRY = "[a-zA-Z][a-zA-Z]";  // two letters
	private static final String YEAR_MONTH = "\\d{4}";
	private static final String VERSION = "(\\.\\d{2})?";  // either .01, .02 or .10, .50 (always two digits)
	public static final String VALID_SENDER_ID_PATTERN = COUNTRY + YEAR_MONTH + VERSION;
	
	// xml tags of dataset
	public static final String RES_ID_COLUMN = "resId";
	public static final String SENDER_DATASET_ID_COLUMN = "senderDatasetId";
	 
}

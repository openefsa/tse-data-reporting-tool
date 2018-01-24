package tse_config;

import app_config.AppPaths;

public class CustomStrings {
	
	public static final String PREDEFINED_RESULTS_FILE = AppPaths.CONFIG_FOLDER + "predefinedResults.xlsx";
	public static final String PREFERENCE_FOLDER = "preferences" + System.getProperty("file.separator");
	public static final String LOG_FOLDER = "logs" + System.getProperty("file.separator");
	
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
	
	public static final String PREFERENCES_SCREENING_BSE = "defScreeningBSE";
	public static final String PREFERENCES_SCREENING_SCRAPIE = "defScreeningSCRAPIE";
	public static final String PREFERENCES_SCREENING_CWD = "defScreeningCWD";
	
	public static final String PREFERENCES_CONFIRMATORY_BSE = "defConfirmatoryBSE";
	public static final String PREFERENCES_CONFIRMATORY_SCRAPIE = "defConfirmatorySCRAPIE";
	public static final String PREFERENCES_CONFIRMATORY_CWD = "defConfirmatoryCWD";
	
	public static final String PREFERENCES_DISCRIMINATORY_BSE = "defDiscriminatoryBSE";
	public static final String PREFERENCES_DISCRIMINATORY_SCRAPIE = "defDiscriminatorySCRAPIE";
	public static final String PREFERENCES_DISCRIMINATORY_CWD = "defDiscriminatoryCWD";
	
	public static final String SUMMARIZED_INFO_TOT_SAMPLES = "totSamplesTested";
	public static final String SUMMARIZED_INFO_POS_SAMPLES = "totSamplesPositive";
	public static final String SUMMARIZED_INFO_NEG_SAMPLES = "totSamplesNegative";
	public static final String SUMMARIZED_INFO_INC_SAMPLES = "totSamplesInconclusive";
	public static final String SUMMARIZED_INFO_UNS_SAMPLES = "totSamplesUnsuitable";
	public static final String SUMMARIZED_INFO_TYPE = "type";
	public static final String SUMMARIZED_INFO_SOURCE = "source";
	public static final String SOURCE_SHEEP_CODE = "F01.A057G";
	public static final String SOURCE_GOAT_CODE = "F01.A057P";
	
	public static final String SUMMARIZED_INFO_PART = "part";
	public static final String SUMMARIZED_INFO_PROD = "prod";
	public static final String SUMMARIZED_INFO_AGE = "animage";
	public static final String SUMMARIZED_INFO_TARGET_GROUP = "tseTargetGroup";

	public static final String SUMMARIZED_INFO_TEST_TYPE = "anMethType";
	public static final String SUMMARIZED_INFO_PROG_INFO = "progInfo";
	public static final String SUMMARIZED_INFO_PROG_ID = "progId";
	public static final String SUMMARIZED_INFO_SAMP_MAT_CODE = "sampMatCode";
	
	public static final String SUMMARIZED_INFO_BSE_TYPE = "BSE";
	public static final String SUMMARIZED_INFO_SCRAPIE_TYPE = "SCRAPIE";
	public static final String SUMMARIZED_INFO_CWD_TYPE = "CWD";
	public static final String SUMMARIZED_INFO_RGT_TYPE = "RGT";
	public static final String SUMMARIZED_INFO_SCREENING_TEST = "AT06A";
	public static final String SUMMARIZED_INFO_CONFIRMATORY_TEST = "AT08A";
	public static final String RESULT_DISCRIMINATORY_TEST = "AT12A";
	public static final String SUMMARIZED_INFO_MOLECULAR_TEST = "AT13A";
	
	public static final String CASE_INFO_STATUS = "statusHerd";
	public static final String CASE_INFO_SAMPLE_ID = "sampId";
	public static final String CASE_INFO_HERD_ID = "herdId";
	public static final String CASE_INFO_ANIMAL_ID = "animalId";
	public static final String CASE_INFO_CASE_ID = "tseNationalCaseId";
	public static final String CASE_INFO_HOLDING_ID = "sampHoldingId";
	public static final String CASE_INFO_ASSESS = "sampAnAsses";
	public static final String CASE_INDEX_CASE = "tseIndexCase";
	public static final String CASE_BIRTH_COUNTRY = "birthCountry";
	public static final String CASE_INFO_BORN_FLOCK = "birthInFlockHerd";
	public static final String CASE_INFO_BREED = "breed";
	public static final String CASE_INFO_COMMENT = "evalCom";
	public static final String BRAIN_CODE = "F02.A06AM";
	public static final String OBEX_CODE = "F02.A16YL";
	public static final String LYMPH_CODE = "F02.A0CJN";
	public static final String BLOOD_CODE = "F02.A06AL";
	public static final String DEFAULT_ASSESS_NEG_CASE_CODE = "J051A";
	public static final String DEFAULT_ASSESS_INC_CASE_CODE = "J050A";
	public static final String DEFAULT_ASSESS_INC_CASE_LABEL = "Inconclusive";
	
	public static final String CASE_INFO_BIRTH_YEAR = "birthYear";
	public static final String CASE_INDEX_BIRTH_MONTH = "birthMonth";
	
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
	public static final String AN_METH_CODE = "anMethCode";
	public static final String AN_METH_CODE_GENOTYPING = "F089A";
	public static final String RESULT_SAMP_MAT_CODE = "sampMatCode";
	
	public static final String PARAM_TYPE_COL = "paramType";
	public static final String PARAM_CODE_BASE_TERM_COL = "paramCodeBaseTerm";
	public static final String PARAM_CODE_COL = "paramCode";
	public static final String SUMMARIZED_INFO_PARAM_TYPE = "P003A";
	public static final String RESULT_PARAM_TYPE = "P001A";
	
	public static final String COUNTRY_REGEX = "[a-zA-Z][a-zA-Z]";  // two letters
	public static final String YEAR_MONTH_REGEX = "\\d{4}";
	public static final String VERSION_REGEX = AppPaths.REPORT_VERSION_REGEX;
	public static final String VALID_SENDER_ID_PATTERN = COUNTRY_REGEX + YEAR_MONTH_REGEX + VERSION_REGEX;
	
	// xml tags of dataset
	public static final String RES_ID_COLUMN = "resId";
	public static final String SENDER_DATASET_ID_COLUMN = "senderDatasetId";
	 
}

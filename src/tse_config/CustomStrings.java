package tse_config;

import app_config.AppPaths;

public class CustomStrings {
	
	public static final String PREDEFINED_RESULTS_FILE = AppPaths.CONFIG_FOLDER + "predefinedResults.xlsx";
	public static final String PREFERENCE_FOLDER = "preferences" + System.getProperty("file.separator");

	public static final String REPORT_ID_COL = "ReportId";
	public static final String SI_ID_COL = "SummarizedInformationId";
	public static final String CASE_ID_COL = "CasesInformationId";
	public static final String SETTINGS_ID_COL = "SettingsId";
	public static final String PREFERENCES_ID_COL = "PreferencesId";
	
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
	public static final String EXCEPTION_COUNTRY_COL = "isExceptionCountry";
	
	public static final String PREFERENCES_COUNTRY = "country";
	
	public static final String PREFERENCES_SCREENING_BSE = "defScreeningBSE";
	public static final String PREFERENCES_SCREENING_SCRAPIE = "defScreeningSCRAPIE";
	public static final String PREFERENCES_SCREENING_CWD = "defScreeningCWD";
	
	public static final String PREFERENCES_CONFIRMATORY_BSE = "defConfirmatoryBSE";
	public static final String PREFERENCES_CONFIRMATORY_SCRAPIE = "defConfirmatorySCRAPIE";
	public static final String PREFERENCES_CONFIRMATORY_CWD = "defConfirmatoryCWD";
	
	public static final String PREFERENCES_DISCRIMINATORY_BSE = "defDiscriminatoryBSE";
	public static final String PREFERENCES_DISCRIMINATORY_SCRAPIE = "defDiscriminatorySCRAPIE";
	public static final String PREFERENCES_DISCRIMINATORY_CWD = "defDiscriminatoryCWD";
	
	public static final String SUMMARIZED_INFO_TYPE = "type";
	public static final String SOURCE_COL = "source";
	public static final String PART_COL = "part";
	public static final String PROD_COL = "prod";
	public static final String ANIMAGE_COL = "animage";
	public static final String TARGET_GROUP_COL = "tseTargetGroup";
	public static final String PROG_INFO_COL = "progInfo";
	public static final String CONTEXT_ID_COL = "contextId";
	public static final String SAMP_MAT_CODE_COL = "sampMatCode";
	public static final String SEX_COL = "sex";
	public static final String PSU_ID_COL = "PSUId";
	public static final String TOT_SAMPLE_TESTED_COL = "totSamplesTested";
	public static final String TOT_SAMPLE_POSITIVE_COL = "totSamplesPositive";
	public static final String TOT_SAMPLE_NEGATIVE_COL = "totSamplesNegative";
	public static final String TOT_SAMPLE_INCONCLUSIVE_COL = "totSamplesInconclusive";
	public static final String TOT_SAMPLE_UNSUITABLE_COL = "totSamplesUnsuitable";

	public static final String SOURCE_SHEEP_CODE = "F01.A057G";
	public static final String SOURCE_GOAT_CODE = "F01.A057P";
	public static final String SEX_MALE = "F32.A0C9A";
	public static final String KILLED_TARGET_GROUP = "TG009A";
	public static final String EM_TARGET_GROUP = "TG003A";
	public static final String UNKNOWN_AGE_CLASS_CODE = "F31.A16PN";
	public static final String WILD_PROD = "F21.A07RY";
	public static final String FARMED_PROD = "F21.A07RV";
	
	public static final String SUMMARIZED_INFO_BSE_TYPE = "BSE";
	public static final String SUMMARIZED_INFO_SCRAPIE_TYPE = "SCRAPIE";
	public static final String SUMMARIZED_INFO_CWD_TYPE = "CWD";
	public static final String SUMMARIZED_INFO_RGT_TYPE = "RGT";
	public static final String SUMMARIZED_INFO_BSEOS_TYPE = "BSEOS";
	
	public static final String CONFIRMATORY_TEST_CODE = "AT08A";
	public static final String DISCRIMINATORY_TEST_CODE = "AT12A";
	public static final String MOLECULAR_TEST_CODE = "AT13A";

	public static final String STATUS_HERD_COL = "statusHerd";
	public static final String STATUS_HERD_INFECTED_CODE = "N";
	public static final String STATUS_HERD_NOT_INFECTED_CODE = "F";
	public static final String SAMPLE_ID_COL = "sampId";
	public static final String HERD_ID_COL = "herdId";
	public static final String ANIMAL_ID_COL = "animalId";
	public static final String NATIONAL_CASE_ID_COL = "tseNationalCaseId";
	public static final String SAMP_HOLDING_ID_COL = "sampHoldingId";
	public static final String SAMP_AN_ASSES_COL = "sampAnAsses";
	public static final String INDEX_CASE_COL = "tseIndexCase";
	
	public static final String INDEX_CASE_YES = "Y";
	public static final String INDEX_CASE_NO = "N";
	
	public static final String BIRTH_COUNTRY_COL = "birthCountry";
	public static final String BORN_FLOCK_HERD_COL = "birthInFlockHerd";
	
	public static final String BREED_COL = "breed";
	public static final String EVAL_COMMENT_COL = "evalCom";
	public static final String EVAL_COMMENT_BREED_ATTRIBUTE_NAME = "com";  // mapping with breed and evalCom
	
	public static final String BRAIN_CODE = "F02.A06AM";
	public static final String OBEX_CODE = "F02.A16YL";
	public static final String RETROPHARYNGEAL_CODE = "F02.A16YZ";
	public static final String BLOOD_CODE = "F02.A06AL";

	public static final String DEFAULT_ASSESS_CBSE_CASE_CODE = "J046A";
	public static final String DEFAULT_ASSESS_NEG_CASE_CODE = "J051A";
	public static final String DEFAULT_ASSESS_INC_CASE_CODE = "J050A";
	public static final String DEFAULT_ASSESS_INC_CASE_LABEL = "Inconclusive";
	
	public static final String BIRTH_YEAR_COL = "birthYear";
	public static final String BIRTH_MONTH_COL = "birthMonth";
	
	public static final String ALLELE_1_COL = "allele1";
	public static final String ALLELE_2_COL = "allele2";
	public static final String ANALYSIS_Y_COL = "analysisY";
	public static final String ALLELE_AFRR = "AFRR";
	public static final String ALLELE_ALRR = "ALRR";
	
	public static final String AN_METH_TYPE_COL = "anMethType";
	public static final String EVAL_INFO_COL = "evalInfo";
	public static final String SCREENING_TEST_CODE = "AT06A";
	public static final String PROG_ID_COL = "progId";
	public static final String SAMP_UNIT_IDS_COL = "sampUnitIds";
	public static final String SAMP_EVENT_INFO_COL = "sampEventInfo";
	public static final String SAMP_MAT_INFO_COL = "sampMatInfo";
	public static final String SAMP_AREA_COL = "sampArea";
	public static final String SAMP_DAY_COL = "sampD";
	public static final String RES_QUAL_VALUE_COL = "resQualValue";
	public static final String TEST_AIM_COL = "testAim";
	public static final String AN_METH_CODE_COL = "anMethCode";
	public static final String AN_METH_CODE_GENOTYPING = "F089A";
	
	public static final String PARAM_TYPE_COL = "paramType";
	public static final String PARAM_CODE_BASE_TERM_COL = "paramCodeBaseTerm";
	public static final String PARAM_CODE_COL = "paramCode";
	public static final String SUMMARIZED_INFO_PARAM_TYPE = "P003A";
	public static final String RESULT_PARAM_TYPE = "P001A";
	public static final String RGT_PARAM_CODE = "RF-00004629-PAR";
	
	public static final String COUNTRY_REGEX = "[a-zA-Z][a-zA-Z]";  // two letters
	public static final String YEAR_MONTH_REGEX = "\\d{4}";
	public static final String VERSION_REGEX = AppPaths.REPORT_VERSION_REGEX;
	public static final String VALID_SENDER_ID_PATTERN = COUNTRY_REGEX + YEAR_MONTH_REGEX + VERSION_REGEX;
	
	// xml tags of dataset
	public static final String RES_ID_COL = "resId";
	public static final String SENDER_DATASET_ID_COL = "senderDatasetId";
	 
}

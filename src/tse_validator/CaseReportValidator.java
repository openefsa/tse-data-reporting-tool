package tse_validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import i18n_messages.TSEMessages;
import providers.ITableDaoService;
import table_relations.Relation;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class CaseReportValidator extends SimpleRowValidatorLabelProvider {

	private static final Logger LOGGER = LogManager.getLogger(CaseReportValidator.class);

	public enum Check {
		OK, WRONG_RESULTS, NO_TEST_SPECIFIED, DUPLICATED_TEST, CASE_ID_FOR_NEGATIVE, INDEX_CASE_FOR_NEGATIVE,
		INDEX_CASE_FOR_FARMED_CWD, EM_FOR_NOT_INFECTED, NOT_CONSTANT_ANALYSIS_YEAR, INDEX_CASE_FOR_INFECTED,
		NOT_INDEX_CASE_FOR_FREE,
	}

	private ITableDaoService daoService;

	public CaseReportValidator(ITableDaoService daoService) {
		this.daoService = daoService;
	}

	public Collection<Check> isRecordCorrect(TableRow row) {

		Collection<Check> checks = new ArrayList<>();

		String caseId = row.getCode(CustomStrings.NATIONAL_CASE_ID_COL);
		String sampEventAsses = row.getCode(CustomStrings.SAMP_EVENT_ASSES_COL);
		// String statusHerd = row.getCode(CustomStrings.STATUS_HERD_COL);

		// Initialise the sum info before in order to get from it the statusHerd
		// which is not present in the samples anymore
		TableRow summInfo = daoService.getById(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET),
				row.getNumCode(Relation.foreignKeyFromParent(CustomStrings.SUMMARIZED_INFO_SHEET)));

		// case id cannot be specified
		if (!caseId.isEmpty() && sampEventAsses.equals(CustomStrings.DEFAULT_ASSESS_NEG_CASE_CODE)) {
			checks.add(Check.CASE_ID_FOR_NEGATIVE);
		}

		String indexCase = row.getCode(CustomStrings.INDEX_CASE_COL);

		// index case on negative sample
		if (!indexCase.isEmpty() && sampEventAsses.equals(CustomStrings.DEFAULT_ASSESS_NEG_CASE_CODE)) {
			checks.add(Check.INDEX_CASE_FOR_NEGATIVE);
		}

		// status herd doenst exists anymore in samples, instead is taken from summinfo
		// TSE48
		if (indexCase.equals(CustomStrings.INDEX_CASE_YES)
				&& (summInfo.getCode(CustomStrings.STATUS_HERD_COL).equals(CustomStrings.STATUS_HERD_INFECTED_CODE))) {
			checks.add(Check.INDEX_CASE_FOR_INFECTED);
		}

		// TSE49
		if (indexCase.equals(CustomStrings.INDEX_CASE_NO) && (summInfo.getCode(CustomStrings.STATUS_HERD_COL)
				.equals(CustomStrings.STATUS_HERD_NOT_INFECTED_CODE))) {
			checks.add(Check.NOT_INDEX_CASE_FOR_FREE);
		}

		TableSchema childSchema = TableSchemaList.getByName(CustomStrings.RESULT_SHEET);

		Collection<TableRow> results = daoService.getByParentId(childSchema, row.getSchema().getSheetName(),
				row.getDatabaseId(), false);

		// if in summinfo screening was set, but no screening
		// was found in the cases
		if (results.isEmpty()) {
			checks.add(Check.NO_TEST_SPECIFIED);
		}

		/*
		 * BR dropped since not valid any more if (isTestDuplicated(results)) {
		 * checks.add(Check.DUPLICATED_TEST); }
		 */

		/*
		 * BR dropped since not valid any more if (!isAnalysisYearConstant(results)) {
		 * checks.add(Check.NOT_CONSTANT_ANALYSIS_YEAR); }
		 */

		// check children errors
		if (row.hasChildrenError()) {
			checks.add(Check.WRONG_RESULTS);
		}

		/*
		 * BR dropped since not valid any more
		 * 
		 * String type = summInfo.getCode(CustomStrings.SUMMARIZED_INFO_TYPE); String
		 * farmed = summInfo.getCode(CustomStrings.PROD_COL);
		 * 
		 * // Index case 'No' for farmed cwd is forbidden if
		 * (indexCase.equals(CustomStrings.INDEX_CASE_NO) &&
		 * type.equals(CustomStrings.SUMMARIZED_INFO_CWD_TYPE)) { if
		 * (farmed.equals(CustomStrings.FARMED_PROD)) {
		 * checks.add(Check.INDEX_CASE_FOR_FARMED_CWD); } }
		 */

		// if eradication measure for status herd F in scrapie
		// if (type.equals(CustomStrings.SUMMARIZED_INFO_SCRAPIE_TYPE)) {

		if (summInfo.getCode(CustomStrings.STATUS_HERD_COL).equals(CustomStrings.STATUS_HERD_NOT_INFECTED_CODE)
				&& summInfo.getCode(CustomStrings.TARGET_GROUP_COL).equals(CustomStrings.EM_TARGET_GROUP)) {
			checks.add(Check.EM_FOR_NOT_INFECTED);
		}

		return checks;
	}

	public static boolean isAnalysisYearConstant(Collection<TableRow> results) {

		HashSet<String> set = new HashSet<>();
		for (TableRow row : results) {
			set.add(row.getCode(CustomStrings.ANALYSIS_Y_COL));
		}

		return (set.size() == 1);
	}

	public static boolean isTestDuplicated(Collection<TableRow> results) {

		HashSet<String> set = new HashSet<>();
		for (TableRow row : results) {
			set.add(row.getCode(CustomStrings.AN_METH_TYPE_COL));
		}

		return (set.size() != results.size());
	}

	public int getOverallWarningLevel(TableRow row) {
		int level = this.getWarningLevel(row);
		int parentLevel = super.getWarningLevel(row);

		return Math.max(level, parentLevel);
	}

	@Override
	public int getWarningLevel(TableRow row) {

		int level = 0;

		try {
			Collection<Check> checks = isRecordCorrect(row);

			if (checks.isEmpty())
				return level;

			Check check = checks.iterator().next();

			switch (check) {
			case OK:
			case EM_FOR_NOT_INFECTED:
			case INDEX_CASE_FOR_FARMED_CWD:
				level = 0;
				break;
			default:
				level = 1;
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Cannot check if the case is correct", e);
		}

		return level;
	}

	@Override
	public String getText(TableRow row) {

		String text = null;
		int parentLevel = super.getWarningLevel(row);

		if (parentLevel > 1)
			return super.getText(row);

		try {

			Collection<Check> checks = isRecordCorrect(row);

			if (checks.isEmpty())
				return super.getText(row);

			Check check = checks.iterator().next();

			switch (check) {
			case NO_TEST_SPECIFIED:
				text = TSEMessages.get("cases.missing.results");
				break;
			case WRONG_RESULTS:
				text = TSEMessages.get("cases.wrong.results");
				break;
			case DUPLICATED_TEST:
				text = TSEMessages.get("cases.duplicated.test.type");
				break;
			case CASE_ID_FOR_NEGATIVE:
				text = TSEMessages.get("cases.case.id.for.negative");
				break;
			case INDEX_CASE_FOR_NEGATIVE:
				text = TSEMessages.get("index.case.for.negative");
				break;
			case INDEX_CASE_FOR_INFECTED:
			case NOT_INDEX_CASE_FOR_FREE:
				text = TSEMessages.get("inconsistent.index.case.status.herd");
				break;
			case INDEX_CASE_FOR_FARMED_CWD: // bypass
			case EM_FOR_NOT_INFECTED:
			case NOT_CONSTANT_ANALYSIS_YEAR:
				text = super.getText(row);
				// text = TSEMessages.get("index.case.for.farmed.cwd");
				break;
			default:
				break;
			}

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Cannot check if the case is correct", e);
		}

		if (text == null)
			text = super.getText(row);

		return text;
	}

	@Override
	public Color getForeground(TableRow row) {

		int level = this.getWarningLevel(row);
		int parentLevel = super.getWarningLevel(row);

		if (parentLevel > level)
			return super.getForeground(row);

		Color color = null;

		try {

			Collection<Check> checks = isRecordCorrect(row);

			if (checks.isEmpty())
				return super.getForeground(row);

			Check check = checks.iterator().next();

			switch (check) {
			case NO_TEST_SPECIFIED:
			case WRONG_RESULTS:
			case DUPLICATED_TEST:
			case CASE_ID_FOR_NEGATIVE:
			case INDEX_CASE_FOR_NEGATIVE:
			case INDEX_CASE_FOR_INFECTED:
			case NOT_INDEX_CASE_FOR_FREE:
				// case INDEX_CASE_FOR_FARMED_CWD:
				// EM_FOR_NOT_INFECTED:
				color = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Cannot check if the case is correct", e);
		}

		if (color == null)
			color = super.getForeground(row);

		return color;
	}
}

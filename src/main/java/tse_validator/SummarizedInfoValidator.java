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
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class SummarizedInfoValidator extends SimpleRowValidatorLabelProvider {

	private static final Logger LOGGER = LogManager.getLogger(SummarizedInfoValidator.class);

	private ITableDaoService daoService;

	public SummarizedInfoValidator(ITableDaoService daoService) {
		this.daoService = daoService;
	}

	public enum SampleCheck {
		OK, MISSING_RGT_CASE, TOO_MANY_INCONCLUSIVES, TOO_FEW_INCONCLUSIVES, TOO_MANY_POSITIVES, TOO_FEW_POSITIVES,
		NON_WILD_FOR_KILLED, WRONG_CASES
	}

	private static int getDistinctCaseIndex(Collection<TableRow> cases, String sampEventAssesType, boolean exclude) {

		HashSet<String> hash = new HashSet<>();

		for (TableRow caseReport : cases) {

			String caseId = caseReport.getCode(CustomStrings.NATIONAL_CASE_ID_COL);
			String sampEventAsses = caseReport.getCode(CustomStrings.SAMP_EVENT_ASSES_COL);

			if ((sampEventAsses.equals(sampEventAssesType) && !exclude)
					|| (!sampEventAsses.equals(sampEventAssesType) && exclude))
				hash.add(caseId);
		}

		return hash.size();
	}

	private static int getDistinctCasesNumber(Collection<TableRow> cases) {

		HashSet<String> hash = new HashSet<>();

		for (TableRow caseReport : cases) {

			String caseId = caseReport.getCode(CustomStrings.NATIONAL_CASE_ID_COL);

			hash.add(caseId);
		}

		return hash.size();
	}

	private static double getPositiveCasesNumber(Collection<TableRow> cases) {
		int neg = getDistinctCaseIndex(cases, CustomStrings.DEFAULT_ASSESS_NEG_CASE_CODE, false);
		int inc = getDistinctCaseIndex(cases, CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE, false);

		double posNum = (getDistinctCasesNumber(cases) - neg - inc);

		return posNum;
	}

	/**
	 * Check if the row is correct or not
	 * 
	 * @param row
	 * @return
	 */
	public Collection<SampleCheck> isSampleCorrect(TableRow row) {

		Collection<SampleCheck> checks = new ArrayList<>();

		/*
		 * BR dropped since not valid any more
		 *
		 * String targetGroup = row.getCode(CustomStrings.TARGET_GROUP_COL); String prod
		 * = row.getCode(CustomStrings.PROD_COL);
		 * 
		 * // non wild for killed error if
		 * (targetGroup.equals(CustomStrings.KILLED_TARGET_GROUP) &&
		 * !prod.equals(CustomStrings.WILD_PROD)) {
		 * checks.add(SampleCheck.NON_WILD_FOR_KILLED); }
		 */

		String rowType = row.getCode(CustomStrings.SUMMARIZED_INFO_TYPE);
		boolean isRGT = rowType.equals(CustomStrings.SUMMARIZED_INFO_RGT_TYPE);

		// only for non rgt rows
		try {

			TableSchema childSchema = TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET);

			Collection<TableRow> cases = daoService.getByParentId(childSchema, row.getSchema().getSheetName(),
					row.getDatabaseId(), true);

			if (!isRGT) {

				// declared inc/pos
				int incSamples = row.getNumLabel(CustomStrings.TOT_SAMPLE_INCONCLUSIVE_COL);
				int posSamples = row.getNumLabel(CustomStrings.TOT_SAMPLE_POSITIVE_COL);

				// detailed inc
				int detailedIncSamples = getDistinctCaseIndex(cases, CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE, false);

				double detailedPosSamples = getPositiveCasesNumber(cases);

				if (detailedPosSamples > posSamples)
					checks.add(SampleCheck.TOO_MANY_POSITIVES);
				else if (detailedPosSamples < posSamples)
					checks.add(SampleCheck.TOO_FEW_POSITIVES);

				if (detailedIncSamples > incSamples)
					checks.add(SampleCheck.TOO_MANY_INCONCLUSIVES);
				else if (detailedIncSamples < incSamples)
					checks.add(SampleCheck.TOO_FEW_INCONCLUSIVES);
			} else if(isRGT) {
				// declared tested cases
				int testedSamples = row.getNumLabel(CustomStrings.TOT_SAMPLE_TESTED_COL);
				if(testedSamples<=0)
					checks.add(SampleCheck.MISSING_RGT_CASE);
			}

		} catch (NumberFormatException e) {
			e.printStackTrace();
			LOGGER.error("Cannot check if the summarized information is correct", e);
		}

		// check children errors
		if (row.hasChildrenError()) {
			checks.add(SampleCheck.WRONG_CASES);
		}

		return checks;
	}

	/**
	 * Get the warning level of the current row
	 * 
	 * @param row
	 * @return
	 */
	public int getOverallWarningLevel(TableRow row) {
		int parentLevel = super.getWarningLevel(row);
		int level = this.getLevel(row);

		return Math.max(parentLevel, level);
	}

	/**
	 * Get the warning level for the current row just for the specific
	 */
	private int getLevel(TableRow row) {

		int level = 0;

		Collection<SampleCheck> checks = isSampleCorrect(row);

		if (checks.isEmpty())
			return level;

		switch (checks.iterator().next()) {
		case MISSING_RGT_CASE:
		case TOO_MANY_INCONCLUSIVES:
		case NON_WILD_FOR_KILLED:
		case TOO_MANY_POSITIVES:
		case TOO_FEW_POSITIVES:
		case TOO_FEW_INCONCLUSIVES:
			level = 1;
			break;
		case WRONG_CASES:
			level = 2;
			break;
		default:
			break;
		}

		return level;
	}

	@Override
	public String getText(TableRow row) {

		String parentText = super.getText(row);
		int parentLevel = super.getWarningLevel(row);
		int level = this.getLevel(row);

		// if parent has bigger severity
		// use its text
		if (parentLevel > level)
			return parentText;

		String text = parentText;

		Collection<SampleCheck> checks = isSampleCorrect(row);

		if (checks.isEmpty())
			return text;

		switch (checks.iterator().next()) {
		case MISSING_RGT_CASE:
			text = TSEMessages.get("si.missing.rgt.case");
			break;
		case TOO_MANY_INCONCLUSIVES:
		case TOO_FEW_INCONCLUSIVES:
			text = TSEMessages.get("si.check.inc");
			break;
		case NON_WILD_FOR_KILLED:
			text = TSEMessages.get("si.non.wild.for.killed");
			break;
		case WRONG_CASES:
			text = TSEMessages.get("si.wrong.cases");
			break;
		case TOO_MANY_POSITIVES:
		case TOO_FEW_POSITIVES:
			text = TSEMessages.get("si.check.pos");
			break;
		default:
			break;
		}

		return text;
	}

	@Override
	public Color getForeground(TableRow row) {

		Color color = super.getForeground(row);

		int parentLevel = super.getWarningLevel(row);
		int level = this.getLevel(row);

		if (parentLevel > level)
			return color;

		Collection<SampleCheck> checks = isSampleCorrect(row);

		if (checks.isEmpty())
			return color;

		switch (checks.iterator().next()) {
		case OK:
			break;
		default:
			Display display = Display.getDefault();
			color = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
			break;
		}

		return color;
	}
}

package tse_validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import i18n_messages.TSEMessages;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class SummarizedInfoValidator extends SimpleRowValidatorLabelProvider {

	private static final Logger LOGGER = LogManager.getLogger(SummarizedInfoValidator.class);
	
	public enum SampleCheck {
		OK,
		MISSING_CASES,
		MISSING_RGT_CASE,
		CHECK_INC_CASES,
		TOO_MANY_SCREENING_NEGATIVES,
		TOOMANY_CASES,
		WRONG_CASES,
		NON_WILD_FOR_KILLED,
		//MORE
	}

	private int getDistinctCaseIndex(Collection<TableRow> cases, 
			String sampAnAssesType, boolean exclude) {

		HashSet<String> hash = new HashSet<>();

		for (TableRow caseReport : cases) {

			String caseId = caseReport.getCode(CustomStrings.CASE_INFO_CASE_ID);
			String sampAnAsses = caseReport.getCode(CustomStrings.CASE_INFO_ASSESS);

			if ((sampAnAsses.equals(sampAnAssesType) && !exclude) 
					||(!sampAnAsses.equals(sampAnAssesType) && exclude))
				hash.add(caseId);
		}

		return hash.size();
	}

	private Collection<TableRow> getNegativeCases(Collection<TableRow> cases) {
		
		Collection<TableRow> out = new ArrayList<>();
		for (TableRow c : cases) {
			if (c.getCode(CustomStrings.CASE_INFO_ASSESS)
					.equals(CustomStrings.DEFAULT_ASSESS_NEG_CASE_CODE))
				out.add(c);
		}
		
		return out;
	}
	
	private Collection<TableRow> getScreeningResults(TableRow sample) {
		
		Collection<TableRow> out = new ArrayList<>();
		
		TableSchema childSchema = TableSchemaList.getByName(CustomStrings.RESULT_SHEET);
		
		if (childSchema == null)
			return out;

		Collection<TableRow> results = sample.getChildren(childSchema);
		
		for (TableRow c : results) {
			if (c.getCode(CustomStrings.RESULT_TEST_TYPE)
					.equals(CustomStrings.RESULT_SCREENING_TEST))
				out.add(c);
		}
		
		return out;
	}
	
	private Collection<TableRow> getNegativeCaseScreeningResults(Collection<TableRow> cases) {
		
		Collection<TableRow> out = new ArrayList<>();
		for(TableRow negative: getNegativeCases(cases)) {
			out.addAll(getScreeningResults(negative));
		}
		
		return out;
	}
	
	/**
	 * Check if the row is correct or not
	 * @param row
	 * @return
	 */
	public SampleCheck isSampleCorrect(TableRow row) {

		String targetGroup = row.getCode(CustomStrings.SUMMARIZED_INFO_TARGET_GROUP);
		String prod = row.getCode(CustomStrings.SUMMARIZED_INFO_PROD);
		
		// non wild for killed error
		if (targetGroup.equals(CustomStrings.KILLED_TARGET_GROUP) 
				&& !prod.equals(CustomStrings.WILD_PROD)) {
			return SampleCheck.NON_WILD_FOR_KILLED;
		}
		
		String rowType = row.getCode(CustomStrings.SUMMARIZED_INFO_TYPE);
		boolean isRGT = rowType.equals(CustomStrings.SUMMARIZED_INFO_RGT_TYPE);

		// only for non rgt rows
		try {

			TableSchema childSchema = TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET);

			if (childSchema == null)
				return null;

			Collection<TableRow> cases = row.getChildren(childSchema);

			if (!isRGT) {

				// declared inc/pos
				int incSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES);
				int posSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES);
				int negSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_NEG_SAMPLES);
				int totPosInc = posSamples + incSamples;

				if (getNegativeCaseScreeningResults(cases).size() > negSamples) {
					return SampleCheck.TOO_MANY_SCREENING_NEGATIVES;
				}
				
				// detailed inc
				int detailedIncSamples = getDistinctCaseIndex(cases, 
						CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE, false);

				// detaled inc and pos together
				int detailedIncAndPos = getDistinctCaseIndex(cases, 
						CustomStrings.DEFAULT_ASSESS_NEG_CASE_CODE, true);

				// if #detailed < #declared
				if (detailedIncAndPos < totPosInc)
					return SampleCheck.MISSING_CASES;
				// if #declared < #detailed
				else if (detailedIncAndPos > totPosInc)
					return SampleCheck.TOOMANY_CASES;

				// if #detailed != #declared
				if (detailedIncSamples != incSamples)
					return SampleCheck.CHECK_INC_CASES;
			}
			else {

				if (cases.size() == 0) {
					return SampleCheck.MISSING_RGT_CASE;
				}
			}
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
			LOGGER.error("Cannot check if the summarized information is correct", e);
		}

		// check children errors
		if (row.hasChildrenError()) {
			return SampleCheck.WRONG_CASES;
		}

		return SampleCheck.OK;
	}

	/**
	 * Get the warning level of the current row
	 * @param row
	 * @return
	 */
	public int getOverallWarningLevel(TableRow row) {
		int parentLevel = super.getWarningLevel(row);
		int level = this.getLevel(row);

		return Math.max(parentLevel, level);
	}

	/**
	 * Get the warning level for the current row
	 * just for the specific 
	 */
	private int getLevel(TableRow row) {

		int level = 0;

		switch (isSampleCorrect(row)) {
		case MISSING_CASES:
		case MISSING_RGT_CASE:
		case TOOMANY_CASES:
		case CHECK_INC_CASES:
		case NON_WILD_FOR_KILLED:
		case TOO_MANY_SCREENING_NEGATIVES:
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

		switch (isSampleCorrect(row)) {
		case MISSING_CASES:
		case MISSING_RGT_CASE:
			text = TSEMessages.get("si.missing.cases");
			break;
		case TOOMANY_CASES:
			text = TSEMessages.get("si.too.many.cases");
			break;
		case CHECK_INC_CASES:
			text = TSEMessages.get("si.wrong.inc.cases");
			break;
		case NON_WILD_FOR_KILLED:
			text = TSEMessages.get("si.non.wild.for.killed");
			break;
		case WRONG_CASES:
			text = TSEMessages.get("si.wrong.cases");
			break;
		case TOO_MANY_SCREENING_NEGATIVES:
			text = TSEMessages.get("si.too.many.neg.screening");
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

		if (isSampleCorrect(row) != SampleCheck.OK) {
			Display display = Display.getDefault();
			color = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
		}

		return color;
	}
}

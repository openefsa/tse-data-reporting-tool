package tse_validator;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import table_skeleton.TableRow;
import tse_config.CustomStrings;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class SummarizedInfoValidator extends SimpleRowValidatorLabelProvider {
	
	private enum SampleCheck {
		OK,
		MISSING_CASES,
		CHECK_INC_CASES,
		TOOMANY_CASES,
		WRONG_CASES,
		//MORE
	}
	
	/**
	 * Check if the row is correct or not
	 * @param row
	 * @return
	 */
	private SampleCheck isSampleCorrect(TableRow row) {
		
		try {
			
			TableSchema childSchema = TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET);
			
			if (childSchema == null)
				return null;
			
			Collection<TableRow> cases = row.getChildren(childSchema);
			
			int incSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES);
			int posSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES);
			//int negSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_NEG_SAMPLES);
			int totPosInc = posSamples + incSamples;
			
			int detailedIncSamples = getDetailedIncCases(cases);
			int detailedNegSamples = getDetailedNegCases(cases);
			int detailedPosSamples = cases.size() - detailedIncSamples - detailedNegSamples;
			
			// if #detailed < #declared
			if (detailedPosSamples + detailedIncSamples < totPosInc)
				return SampleCheck.MISSING_CASES;
			// if #declared < #detailed
			else if (totPosInc < detailedIncSamples + detailedPosSamples)
				return SampleCheck.TOOMANY_CASES;
			
			int compare = checkCasesType(cases, CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE, incSamples);
			if (compare != 0)
				return SampleCheck.CHECK_INC_CASES;
			
			// check children errors
			if (row.hasChildrenError()) {
				return SampleCheck.WRONG_CASES;
			}
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
		return SampleCheck.OK;
	}
	
	private int getDetailedIncCases(Collection<TableRow> cases) {
		
		int inc = 0;
		for (TableRow caseInfo : cases) {
			if (caseInfo.getCode(CustomStrings.CASE_INFO_ASSESS).equals(CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE))
				inc++;
		}
		return inc;
	}
	
	private int getDetailedNegCases(Collection<TableRow> cases) {
		
		int neg = 0;
		for (TableRow caseInfo : cases) {
			if (caseInfo.getCode(CustomStrings.CASE_INFO_ASSESS).equals(CustomStrings.DEFAULT_ASSESS_NEG_CASE_CODE))
				neg++;
		}
		return neg;
	}
	
	private int checkCasesType(Collection<TableRow> cases, String value, int declaredValue) {
		Collection<String> values = new ArrayList<>();
		values.add(value);
		return this.checkCasesType(cases, values, declaredValue);
	}
	private int checkCasesType(Collection<TableRow> cases, Collection<String> values, int declaredValue) {
		
		int incCases = 0;
		for (TableRow caseInfo : cases) {
			if (values.contains(caseInfo.getCode(CustomStrings.CASE_INFO_ASSESS)))
				incCases++;
		}
		
		if (incCases == declaredValue)
			return 0;
		else if (declaredValue > incCases)
			return -1;
		else
			return 1;
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
		case TOOMANY_CASES:
		case CHECK_INC_CASES:
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
			text = "Add cases details";
			break;
		case TOOMANY_CASES:
			text = "Too many cases detailed";
			break;
		case CHECK_INC_CASES:
			text = "Check inconclusive cases number";
			break;
		case WRONG_CASES:
			text = "Check cases details";
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

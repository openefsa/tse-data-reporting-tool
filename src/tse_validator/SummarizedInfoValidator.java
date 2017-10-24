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
		LESS,
		MISSING_INC_CASES,
		MISSING_POS_CASES,
		MISSING_NEG_CASES,
		TOOMANY_INC_CASES,
		TOOMANY_POS_CASES,
		TOOMANY_NEG_CASES,
		WRONG_CASES,
		MORE
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
			
			// check children errors
			if (row.hasChildrenError()) {
				return SampleCheck.WRONG_CASES;
			}

			int incSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES);
			int posSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES);
			int negSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_NEG_SAMPLES);
			int tot = posSamples + incSamples + negSamples;
			
			int compare = checkCasesType(cases, CustomStrings.DEFAULT_ASSESS_NEG_CASE_CODE, negSamples);
			if (compare == 1)
				return SampleCheck.TOOMANY_NEG_CASES;
			else if (compare == -1)
				return SampleCheck.MISSING_NEG_CASES;
			
			compare = checkCasesType(cases, CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE, incSamples);
			if (compare == 1)
				return SampleCheck.TOOMANY_INC_CASES;
			else if (compare == -1)
				return SampleCheck.MISSING_INC_CASES;
			
			else {
				
				compare = (cases.size() - incSamples - negSamples);
				
				// else if inc and negative are equal but count is different
				if (compare < posSamples)
					return SampleCheck.MISSING_POS_CASES;
				else if (compare > posSamples) {
					return SampleCheck.TOOMANY_POS_CASES;
				}
			}

			
			if (cases.size() > tot)
				return SampleCheck.MORE;
			else if (cases.size() < tot)
				return SampleCheck.LESS;
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
		return SampleCheck.OK;
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
		case LESS:
		case MORE:
		case MISSING_INC_CASES:
		case MISSING_NEG_CASES:
		case MISSING_POS_CASES:
		case TOOMANY_INC_CASES:
		case TOOMANY_NEG_CASES:
		case TOOMANY_POS_CASES:
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
		case LESS:
			text = "Cases report incomplete";
			break;
		case MORE:
			text = "Too many cases reported";
			break;
		case MISSING_INC_CASES:
			text = "Missing inconclusive cases";
			break;
		case MISSING_NEG_CASES:
			text = "Missing negative cases";
			break;
		case MISSING_POS_CASES:
			text = "Missing positive cases";
			break;
		case TOOMANY_INC_CASES:
			text = "Check inconclusive cases";
			break;
		case TOOMANY_NEG_CASES:
			text = "Check negative cases";
			break;
		case TOOMANY_POS_CASES:
			text = "Check positive cases";
			break;
		case WRONG_CASES:
			text = "Check case report";
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

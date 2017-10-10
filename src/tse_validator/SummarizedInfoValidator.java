package tse_validator;

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
		CHECK_CASES,
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
			CaseReportValidator validator = new CaseReportValidator();
			for (TableRow caseInfo : cases) {
				if (validator.getWarningLevel(caseInfo) > 0) {
					return SampleCheck.WRONG_CASES;
				}
			}
			
			int incSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES);
			int posSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES);
			int tot = posSamples + incSamples;
			
			if (cases.size() > tot)
				return SampleCheck.MORE;
			else if (cases.size() < tot)
				return SampleCheck.LESS;
			
			// check inconclusive are coherent with cases
			int incCases = 0;
			for (TableRow caseInfo : cases) {
				if (caseInfo.getCode(CustomStrings.CASE_INFO_ASSESS)
						.equals(CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE))
					incCases++;
			}
			
			if (incCases != incSamples)
				return SampleCheck.CHECK_CASES;
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
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
		case LESS:
		case MORE:
		case CHECK_CASES:
			level = 1;
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
		case CHECK_CASES:
			text = "Check inconclusive cases";
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

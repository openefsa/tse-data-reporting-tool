package tse_validator;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import table_skeleton.TableRow;
import tse_config.CustomStrings;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class SummarizedInfoValidator extends SimpleRowValidatorLabelProvider {
	
	public enum SampleCheck {
		OK,
		MISSING_CASES,
		CHECK_INC_CASES,
		TOOMANY_CASES,
		WRONG_CASES,
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
	
	/**
	 * Check if the row is correct or not
	 * @param row
	 * @return
	 */
	public SampleCheck isSampleCorrect(TableRow row) {
		
		try {
			
			TableSchema childSchema = TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET);
			
			if (childSchema == null)
				return null;
			
			Collection<TableRow> cases = row.getChildren(childSchema);
			
			// declared inc/pos
			int incSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES);
			int posSamples = row.getNumLabel(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES);
			int totPosInc = posSamples + incSamples;
			
			// detailed inc
			int detailedIncSamples = getDistinctCaseIndex(cases, 
					CustomStrings.DEFAULT_ASSESS_INC_CASE_CODE, false);
			
			// detaled inc and pos together
			int detailedIncAndPos = getDistinctCaseIndex(cases, 
					CustomStrings.DEFAULT_ASSESS_NEG_CASE_CODE, true);
	
			/*System.out.println("Dec inc " + incSamples);
			System.out.println("Det inc " + detailedIncSamples);
			System.out.println("Dec pos/inc " + totPosInc);
			System.out.println("Det pos/inc " + detailedIncAndPos);*/
			
			// if #detailed < #declared
			if (detailedIncAndPos < totPosInc)
				return SampleCheck.MISSING_CASES;
			// if #declared < #detailed
			else if (detailedIncAndPos > totPosInc)
				return SampleCheck.TOOMANY_CASES;
			
			// if #detailed != #declared
			if (detailedIncSamples != incSamples)
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

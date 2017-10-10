package tse_validator;

import java.io.IOException;
import java.util.Collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import table_skeleton.TableRow;
import tse_config.CustomStrings;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class CaseReportValidator extends SimpleRowValidatorLabelProvider {
	
	private enum Check {
		OK,
		WRONG_RESULTS,
		SCREENING_MISSING,
		CONFIRMATORY_MISSING,
		WRONG_ALLELE
	}

	private Check isRecordCorrect(TableRow row) throws IOException {

		TableSchema childSchema = TableSchemaList.getByName(CustomStrings.RESULT_SHEET);
		Collection<TableRow> results = row.getChildren(childSchema);
		
		// check children errors
		ResultValidator resultValidator = new ResultValidator();
		for (TableRow result : results) {
			if (resultValidator.getWarningLevel(result) > 0) {
				return Check.WRONG_RESULTS;
			}
		}
		
		
		TableSchema parentSchema = TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET);
		TableRow summInfo = row.getParent(parentSchema);

		boolean summScreening = false;

		// if we have a screening in the summarized
		if (summInfo.getCode(CustomStrings.SUMMARIZED_INFO_TEST_TYPE)
				.equals(CustomStrings.SUMMARIZED_INFO_SCREENING_TEST)) {
			summScreening = true;
			
		}

		// but no screening in results
		boolean hasScreening = false;
		for (TableRow result : results) {
			if (result.getCode(CustomStrings.RESULT_TEST_TYPE)
					.equals(CustomStrings.RESULT_SCREENING_TEST)) {
				hasScreening = true;
				break;
			}
		}

		// if in summinfo screening was set, but no screening
		// was found in the cases
		if (summScreening && !hasScreening) {
			return Check.SCREENING_MISSING;
		}
		
		
		// at least one confirmatory should be reported
		boolean hasConfirmatory = false;
		for (TableRow result : results) {
			if (result.getCode(CustomStrings.RESULT_TEST_TYPE)
					.equals(CustomStrings.SUMMARIZED_INFO_CONFIRMATORY_TEST)) {
				hasConfirmatory = true;
				break;
			}
		}
		
		if (!hasConfirmatory) {
			return Check.CONFIRMATORY_MISSING;
		}
		

		String testType = row.getCode(CustomStrings.RESULT_TEST_TYPE);

		// if it is not molecular test
		if (!testType.equals(CustomStrings.SUMMARIZED_INFO_MOLECULAR_TEST)) {

			// check if alleles were set (it is an error!)
			String allele1 = row.getCode(CustomStrings.RESULT_ALLELE_1);
			String allele2 = row.getCode(CustomStrings.RESULT_ALLELE_2);

			boolean empty = allele1.isEmpty() && allele2.isEmpty();

			if (!empty) {
				return Check.WRONG_ALLELE;
			}
		}
		
		return Check.OK;
	}
	
	@Override
	public int getWarningLevel(TableRow row) {
		
		try {
			Check check = isRecordCorrect(row);
			
			if (check != Check.OK)
				return 1;
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		return 0;
	}
	
	@Override
	public String getText(TableRow row) {
		
		String text = null;
		
		int level = this.getWarningLevel(row);
		int parentLevel = super.getWarningLevel(row);
		
		if (parentLevel > level)
			return super.getText(row);
		
		try {
			
			Check check = isRecordCorrect(row);
			switch (check) {
			case WRONG_ALLELE:
				text = "Alleles not reportable";
				break;
			case SCREENING_MISSING:
				text = "Missing screening/rapid test";
				break;
			case CONFIRMATORY_MISSING:
				text = "Missing confirmatory test";
				break;
			case WRONG_RESULTS:
				text = "Check analytical results";
				break;
			default:
				break;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
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
			Check check = isRecordCorrect(row);
			switch (check) {
			case WRONG_ALLELE:
				color = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
				break;
			case SCREENING_MISSING:
			case CONFIRMATORY_MISSING:
			case WRONG_RESULTS:
				color = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW);
				break;
			default:
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (color == null)
			color = super.getForeground(row);

		return color;
	}
}

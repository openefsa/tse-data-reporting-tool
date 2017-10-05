package tse_validator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import table_skeleton.TableRow;
import tse_config.CustomStrings;

public class ResultValidator extends SimpleRowValidatorLabelProvider {

	private boolean alleleError;
	
	@Override
	public int getWarningLevel(TableRow row) {
		
		int level = super.getWarningLevel(row);
		
		// if we have a heavier warning use
		// the parent one
		if (level > 1)
			return level;
		
		String testType = row.getCode(CustomStrings.RESULT_TEST_TYPE);
		
		// if it is not molecular test
		if (!testType.equals("AT13A")) {
			
			// check if alleles were set (it is an error!)
			String allele1 = row.getCode(CustomStrings.RESULT_ALLELE_1);
			String allele2 = row.getCode(CustomStrings.RESULT_ALLELE_2);
			
			boolean empty = allele1.isEmpty() && allele2.isEmpty();
			
			if (!empty) {
				level = 1;
				alleleError = true;
			}
		}
		
		return level;
	}
	
	@Override
	public String getText(TableRow row) {
		
		int level = this.getWarningLevel(row);

		if (alleleError) {
			return "Alleles not reportable";
		}
		else return super.getText(row);
	}
	
	@Override
	public Color getForeground(TableRow row) {

		int level = this.getWarningLevel(row);

		if (alleleError) {
			return Display.getCurrent().getSystemColor(SWT.COLOR_RED);
		}
		else 
			return super.getForeground(row);
	}
}

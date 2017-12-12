package tse_validator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import i18n_messages.TSEMessages;
import table_skeleton.TableRow;
import tse_config.CustomStrings;

public class ResultValidator extends SimpleRowValidatorLabelProvider {

	private ErrorType error;
	
	public enum ErrorType {
		ALLELE_ERROR,
		NONE
	}
	
	public ErrorType getError(TableRow row) {
		getWarningLevel(row);
		return error;
	}
	
	@Override
	public int getWarningLevel(TableRow row) {
		
		error = ErrorType.NONE;
		
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
			
			boolean notEmpty = !allele1.isEmpty() || !allele2.isEmpty();

			if (notEmpty) {
				level = 1;
				error = ErrorType.ALLELE_ERROR;
			}
		}
		
		return level;
	}
	
	@Override
	public String getText(TableRow row) {

		getWarningLevel(row);
		
		if (error == ErrorType.ALLELE_ERROR) {
			return TSEMessages.get("results.alleles.not.reportable");
		}
		else return super.getText(row);
	}
	
	@Override
	public Color getForeground(TableRow row) {

		getWarningLevel(row);
		
		if (error == ErrorType.ALLELE_ERROR) {
			return Display.getCurrent().getSystemColor(SWT.COLOR_RED);
		}
		else 
			return super.getForeground(row);
	}
}

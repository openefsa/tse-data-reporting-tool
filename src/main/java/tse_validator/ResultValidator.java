package tse_validator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import i18n_messages.TSEMessages;
import table_skeleton.TableRow;
import tse_config.CustomStrings;

public class ResultValidator extends SimpleRowValidatorLabelProvider {

	public enum ErrorType {
		ALLELE_ERROR, WRONG_ALLELE_PAIR, NONE
	}

	public static ErrorType getError(TableRow row) {

		String testType = row.getCode(CustomStrings.AN_METH_TYPE_COL);

		String allele1 = row.getCode(CustomStrings.ALLELE_1_COL);
		String allele2 = row.getCode(CustomStrings.ALLELE_2_COL);

		// if it is not molecular test
		if (!testType.equals(CustomStrings.MOLECULAR_TEST_CODE)) {

			// check if alleles were set (it is an error!)
			boolean notEmpty = !allele1.isEmpty() || !allele2.isEmpty();

			if (notEmpty) {
				return ErrorType.ALLELE_ERROR;
			}
		}

		/*
		 * BR droped since not valid any more 
		 * boolean allele1Check =
		 * allele1.equals(CustomStrings.ALLELE_AFRR) ||
		 * allele1.equals(CustomStrings.ALLELE_ALRR); boolean allele2Check =
		 * allele2.equals(CustomStrings.ALLELE_AFRR) ||
		 * allele2.equals(CustomStrings.ALLELE_ALRR);
		 * 
		 * if (allele1Check && allele2Check) { return ErrorType.WRONG_ALLELE_PAIR; }
		 */

		return ErrorType.NONE;
	}

	@Override
	public int getWarningLevel(TableRow row) {

		int level = super.getWarningLevel(row);

		// if we have a heavier warning use
		// the parent one
		if (level > 1)
			return level;

		return getError(row) == ErrorType.NONE ? 0 : 1;
	}

	@Override
	public String getText(TableRow row) {

		String message = null;
		ErrorType error = getError(row);
		switch (error) {
		case ALLELE_ERROR:
			message = TSEMessages.get("results.alleles.not.reportable");
			break;
		case WRONG_ALLELE_PAIR:
			message = TSEMessages.get("results.wrong.alleles.pair");
			break;
		default:
			message = super.getText(row);
			break;
		}

		return message;
	}

	@Override
	public Color getForeground(TableRow row) {

		ErrorType error = getError(row);

		Color color;
		switch (error) {
		case ALLELE_ERROR:
			color = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
			break;
		case WRONG_ALLELE_PAIR:
			color = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW);
			break;
		default:
			color = super.getForeground(row);
			break;
		}

		return color;
	}
}

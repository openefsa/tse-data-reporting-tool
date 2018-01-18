package tse_validator;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import i18n_messages.TSEMessages;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class CaseReportValidator extends SimpleRowValidatorLabelProvider {
	
	public enum Check {
		OK,
		WRONG_RESULTS,
		NO_TEST_SPECIFIED,
		DUPLICATED_TEST
	}

	public Check isRecordCorrect(TableRow row) throws IOException {

		TableSchema childSchema = TableSchemaList.getByName(CustomStrings.RESULT_SHEET);
		Collection<TableRow> results = row.getChildren(childSchema);

		// if in summinfo screening was set, but no screening
		// was found in the cases
		if (results.isEmpty()) {
			return Check.NO_TEST_SPECIFIED;
		}
		
		if (isTestDuplicated(results)) {
			return Check.DUPLICATED_TEST;
		}
		
		// check children errors
		if (row.hasChildrenError()) {
			return Check.WRONG_RESULTS;
		}
		
		return Check.OK;
	}
	
	public boolean isTestDuplicated(Collection<TableRow> results) {
		
		HashSet<String> set = new HashSet<>();
		for (TableRow row : results) {
			set.add(row.getCode(CustomStrings.RESULT_TEST_TYPE));
		}
		
		return (set.size() != results.size());
	}
	
	public int getOverallWarningLevel(TableRow row) {
		int level = this.getWarningLevel(row);
		int parentLevel = super.getWarningLevel(row);
		
		return Math.max(level, parentLevel);
	}
	
	@Override
	public int getWarningLevel(TableRow row) {

		int level = 0;

		try {
			Check check = isRecordCorrect(row);

			if (check != Check.OK)
				level = 1;

		} catch (IOException e) {
			e.printStackTrace();
		}

		return level;
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
			case NO_TEST_SPECIFIED:
				text = TSEMessages.get("cases.missing.results");
				break;
			case WRONG_RESULTS:
				text = TSEMessages.get("cases.wrong.results");
				break;
			case DUPLICATED_TEST:
				text = TSEMessages.get("cases.duplicated.test.type");
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
			case NO_TEST_SPECIFIED:
			case WRONG_RESULTS:
			case DUPLICATED_TEST:
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

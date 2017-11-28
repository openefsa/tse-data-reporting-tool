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
	
	public enum Check {
		OK,
		WRONG_RESULTS,
		NO_TEST_SPECIFIED
	}

	public Check isRecordCorrect(TableRow row) throws IOException {

		TableSchema childSchema = TableSchemaList.getByName(CustomStrings.RESULT_SHEET);
		Collection<TableRow> results = row.getChildren(childSchema);

		// if in summinfo screening was set, but no screening
		// was found in the cases
		if (results.isEmpty()) {
			return Check.NO_TEST_SPECIFIED;
		}
		
		// check children errors
		if (row.hasChildrenError()) {
			return Check.WRONG_RESULTS;
		}
		
		return Check.OK;
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
				text = "Add tests details";
				break;
			case WRONG_RESULTS:
				text = "Check tests details";
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

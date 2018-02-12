package tse_validator;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import i18n_messages.TSEMessages;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class CaseReportValidator extends SimpleRowValidatorLabelProvider {
	
	private static final Logger LOGGER = LogManager.getLogger(CaseReportValidator.class);
	
	public enum Check {
		OK,
		WRONG_RESULTS,
		NO_TEST_SPECIFIED,
		DUPLICATED_TEST,
		CASE_ID_FOR_NEGATIVE
	}

	public Check isRecordCorrect(TableRow row) throws IOException {
		
		String caseId = row.getCode(CustomStrings.CASE_INFO_CASE_ID);
		String sampAnAsses = row.getCode(CustomStrings.CASE_INFO_ASSESS);
		
		// index case on negative sample
		if (!caseId.isEmpty() && sampAnAsses.equals(CustomStrings.DEFAULT_ASSESS_NEG_CASE_CODE)) {
			return Check.CASE_ID_FOR_NEGATIVE;
		}
		
		TableSchema childSchema = TableSchemaList.getByName(CustomStrings.RESULT_SHEET);
		Collection<TableRow> results = row.getChildren(childSchema, false);

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
			LOGGER.error("Cannot check if the case is correct", e);
		}

		return level;
	}
	
	@Override
	public String getText(TableRow row) {
		
		String text = null;
		int parentLevel = super.getWarningLevel(row);
		
		if (parentLevel > 1)
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
			case CASE_ID_FOR_NEGATIVE:
				text = TSEMessages.get("cases.case.id.for.negative");
				break;
			default:
				break;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Cannot check if the case is correct", e);
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
			case CASE_ID_FOR_NEGATIVE:
				color = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_YELLOW);
				break;
			default:
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Cannot check if the case is correct", e);
		}
		
		if (color == null)
			color = super.getForeground(row);

		return color;
	}
}

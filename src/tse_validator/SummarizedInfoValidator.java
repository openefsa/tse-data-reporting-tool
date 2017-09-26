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

public class SummarizedInfoValidator extends SimpleRowValidatorLabelProvider {

	private boolean isSampleCorrect(TableRow row) {

		try {
			
			TableSchema childSchema = TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET);
			Collection<TableRow> cases = row.getChildren(childSchema);
			
			String posSamplesStr = row.get(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES).getLabel();
			String incSamplesStr = row.get(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES).getLabel();
			
			// check number of positive and inconclusive
			// samples with the number of cases
			int posSamples = Integer.valueOf(posSamplesStr);
			int incSamples = Integer.valueOf(incSamplesStr);
			
			return (cases.size() == posSamples + incSamples);
		}
		catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	@Override
	public String getText(TableRow row) {
		
		String text = super.getText(row);

		if (!isSampleCorrect(row))
			text = "Cases report incomplete";
		
		/*switch (row.getStatus()) {

		case POSITIVE_MISSING:
			text = "Positive cases report incomplete";
			break;
		case INCONCLUSIVE_MISSING:
			text = "";
			break;
		default:
			break;
		}*/

		return text;
	}
	
	@Override
	public Color getForeground(TableRow row) {
		
		Color color = super.getForeground(row);

		if (!isSampleCorrect(row)) {
			Display display = Display.getDefault();
			color = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
		}
		
		return color;
	}
}

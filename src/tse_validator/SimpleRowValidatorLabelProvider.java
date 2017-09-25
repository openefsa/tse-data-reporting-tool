package tse_validator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import table_dialog.RowValidatorLabelProvider;
import table_skeleton.TableRow;

public class SimpleRowValidatorLabelProvider extends RowValidatorLabelProvider {

	@Override
	public String getText(TableRow row) {
		
		String text = "";

		switch (row.getStatus()) {
		case OK:
			text = "Validated";
			break;
		case MANDATORY_MISSING:
			text = "Missing mandatory fields";
			break;
		case ERROR:
			text = "Check case report";
			break;
		default:
			break;
		}

		return text;
	}
	
	@Override
	public Color getForeground(TableRow row) {
		return getRowColor(row);
	}
	
	/**
	 * Row color based on row status
	 * @param row
	 * @return
	 */
	private Color getRowColor(TableRow row) {
		
		Display display = Display.getDefault();
		
		Color red = display.getSystemColor(SWT.COLOR_RED);
	    Color green = display.getSystemColor(SWT.COLOR_DARK_GREEN);
	    Color yellow = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
	    
	    Color rowColor = green;
	    
	    switch (row.getStatus()) {
	    case OK:
	    	rowColor = green;
	    	break;
	    case ERROR:
	    case MANDATORY_MISSING:
	    	rowColor = red;
	    	break;
	    default:
	    	rowColor = yellow;  // default for general warnings
	    	break;
	    }
	    
	    return rowColor;
	}
}

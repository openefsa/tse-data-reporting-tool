package tse_validator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

import i18n_messages.TSEMessages;
import table_dialog.RowValidatorLabelProvider;
import table_skeleton.TableRow;

public class SimpleRowValidatorLabelProvider extends RowValidatorLabelProvider {
	
	@Override
	public String getText(TableRow row) {
		
		String text = "";

		switch (row.getRowStatus()) {
		case OK:
			text = TSEMessages.get("row.locally.validated");
			break;
		case CONDITIONAL_MISSING:
			text = TSEMessages.get("row.additional.fields.missing");
			break;
		case MANDATORY_MISSING:
			text = TSEMessages.get("row.mandatory.fields.missing");
			break;
		default:
			break;
		}

		return text;
	}
	
	@SuppressWarnings("static-method")
	public int getWarningLevel(TableRow row) {
		
		int level = 0;
		
		switch (row.getRowStatus()) {
		case OK:
			level = 0;
			break;
		case CONDITIONAL_MISSING:
			level = 3;
			break;
		case MANDATORY_MISSING:
			level = 5;
			break;
		default:
			break;
		}
		
		return level;
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
	@SuppressWarnings("static-method")
	private Color getRowColor(TableRow row) {
		
		Display display = Display.getDefault();
		
		Color red = display.getSystemColor(SWT.COLOR_RED);
	    Color green = display.getSystemColor(SWT.COLOR_DARK_GREEN);
	    Color yellow = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
	    Color magenta = display.getSystemColor(SWT.COLOR_MAGENTA);
	    
	    Color rowColor = green;
	    
	    switch (row.getRowStatus()) {
	    case OK:
	    	rowColor = green;
	    	break;
	    case CONDITIONAL_MISSING:
	    	rowColor = magenta;
	    	break;
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

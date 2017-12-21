package tse_config;

import javax.swing.JOptionPane;

import org.eclipse.swt.widgets.Shell;

import app_config.PropertiesReader;
import i18n_messages.TSEMessages;
import message.SendMessageErrorType;
import message.SendMessageException;
import table_database.TableDao;
import table_skeleton.TableRowList;
import xlsx_reader.TableSchemaList;

public class GeneralWarnings {

	public static void showExceptionStack(Shell shell, Exception e) {
		
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement ste : e.getStackTrace()) {
	        sb.append("\n\tat ");
	        sb.append(ste);
	    }
	    String trace = sb.toString();
		
		
		JOptionPane.showMessageDialog(null, TSEMessages.get("generic.error", PropertiesReader.getSupportEmail(), trace), 
				TSEMessages.get("error.title"), JOptionPane.ERROR_MESSAGE);
	}

	
	public static String[] getSendMessageWarning(SendMessageException sendE) {
		
		String title;
		String message;
		
		String messageError = sendE.getMessage();
		
		SendMessageErrorType type = SendMessageErrorType.fromString(messageError);

		switch(type) {
		case NON_DP_USER:
			
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("account.incomplete", PropertiesReader.getSupportEmail());

			break;
			
		case USER_WRONG_ORG:
			title = TSEMessages.get("error.title");
			TableDao dao = new TableDao(TableSchemaList.getByName(CustomStrings.SETTINGS_SHEET));

			String orgCode = "";
			TableRowList settingsList = dao.getAll();
			if (!settingsList.isEmpty()) {
				orgCode = settingsList.get(0).getLabel(CustomStrings.SETTINGS_ORG_CODE);
			}
			message = TSEMessages.get("account.wrong.org", orgCode);
			break;
		case USER_WRONG_PROFILE:
			
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("account.incorrect", PropertiesReader.getSupportEmail());
			
			break;
			
		default:
			
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("send.message.failed", PropertiesReader.getSupportEmail(), sendE.getErrorMessage());
			break;
		}
		
		return new String[] {title, message};
	}
}

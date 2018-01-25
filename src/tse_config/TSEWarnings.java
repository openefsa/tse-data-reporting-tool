package tse_config;

import org.eclipse.swt.SWT;

import app_config.PropertiesReader;
import global_utils.Message;
import global_utils.Warnings;
import i18n_messages.TSEMessages;
import message.SendMessageErrorType;
import message.SendMessageException;
import table_database.TableDao;
import table_skeleton.TableRowList;
import xlsx_reader.TableSchemaList;

public class TSEWarnings {
	
	public static Message getSendMessageWarning(SendMessageException sendE) {
		
		Message msg = null;
		String messageError = sendE.getMessage();
		
		SendMessageErrorType type = SendMessageErrorType.fromString(messageError);

		switch(type) {
		case NON_DP_USER:
			msg = Warnings.createFatal(TSEMessages.get("account.incomplete",
					PropertiesReader.getSupportEmail()));
			break;
			
		case USER_WRONG_ORG:
			TableDao dao = new TableDao(TableSchemaList.getByName(CustomStrings.SETTINGS_SHEET));

			String orgCode = "";
			TableRowList settingsList = dao.getAll();
			if (!settingsList.isEmpty()) {
				orgCode = settingsList.get(0).getLabel(CustomStrings.SETTINGS_ORG_CODE);
			}
			
			msg = Warnings.create(TSEMessages.get("error.title"), 
					TSEMessages.get("account.wrong.org", orgCode), SWT.ICON_ERROR);
			break;
		case USER_WRONG_PROFILE:
			
			msg = Warnings.createFatal(TSEMessages.get("account.incorrect",
					PropertiesReader.getSupportEmail()));
			
			break;
			
		default:
			
			msg = Warnings.createFatal(TSEMessages.get("send.message.failed",
					PropertiesReader.getSupportEmail()));
			break;
		}
		
		return msg;
	}
}

package tse_config;

import org.eclipse.swt.SWT;

import app_config.PropertiesReader;
import dataset.IDataset;
import global_utils.Message;
import global_utils.Warnings;
import i18n_messages.TSEMessages;
import message.SendMessageErrorType;
import message.SendMessageException;
import table_database.TableDao;
import table_skeleton.TableRowList;
import xlsx_reader.TableSchemaList;

public class TSEWarnings {
	
	public static Message getSendMessageWarning(SendMessageException sendE, IDataset... reports) {
		
		Message msg = null;
		String messageError = sendE.getMessage();
		
		SendMessageErrorType type = SendMessageErrorType.fromString(messageError);

		switch(type) {
		case NON_DP_USER:
			msg = Warnings.createFatal(TSEMessages.get("account.incomplete",
					PropertiesReader.getSupportEmail()), reports);
			break;
			
		case USER_WRONG_ORG:
			TableDao dao = new TableDao();

			String orgCode = "";
			TableRowList settingsList = dao.getAll(TableSchemaList.getByName(CustomStrings.SETTINGS_SHEET));
			if (!settingsList.isEmpty()) {
				orgCode = settingsList.get(0).getLabel(CustomStrings.SETTINGS_ORG_CODE);
			}
			
			msg = Warnings.create(TSEMessages.get("error.title"), 
					TSEMessages.get("account.wrong.org", orgCode), SWT.ICON_ERROR);
			break;
		case USER_WRONG_PROFILE:
			
			msg = Warnings.createFatal(TSEMessages.get("account.incorrect",
					PropertiesReader.getSupportEmail()), reports);
			
			break;
			
		default:
			//TODO check if it is possible to give a more excplicit message based on what is returned from the DCF 
			msg = Warnings.createFatal(TSEMessages.get("send.message.failed",
					messageError, PropertiesReader.getSupportEmail()), reports);
			break;
		}
		
		return msg;
	}
}

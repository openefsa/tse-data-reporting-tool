package tse_summarized_information;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.xml.sax.SAXException;

import amend_manager.AmendException;
import app_config.AppPaths;
import app_config.PropertiesReader;
import dataset.Dataset;
import global_utils.Message;
import global_utils.Warnings;
import i18n_messages.TSEMessages;
import message.SendMessageException;
import providers.TseReportService;
import report.NotOverwritableDcfDatasetException;
import report.ReportActions;
import report.ReportException;
import report.ReportSendOperation;
import soap.DetailedSOAPException;
import tse_config.TSEWarnings;
import tse_report.TseReport;

public class TseReportActions extends ReportActions {

	private TseReportService reportService;
	private TseReport report;
	private Shell shell;
	
	public TseReportActions(Shell shell, TseReport report, 
			TseReportService reportService) {
		super(shell, report, reportService);
		this.shell = shell;
		this.report = report;
		this.reportService = reportService;
	}
	
	/**
	 * Amend a report
	 * @param shell
	 * @param report
	 * @param listener
	 */
	public TseReport amend() {
		
		boolean confirm = askConfirmation(ReportAction.AMEND);
		
		if (!confirm)
			return null;
		
		shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
		
		// create a new version of the report in the db
		// it affects directly the current object
		TseReport amendedReport = reportService.amend(report);
		
		shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
		
		// we can returned the modified object
		return amendedReport;
	}

	@Override
	public void end(ReportAction action) {
		
		switch(action) {
		
		case SEND:
			sendEnd();
			break;
		case REJECT:
			rejectEnd();
			break;
		case SUBMIT:
			submitEnd();
			break;
		case ACCEPTED_DWH_BETA:
			acceptedDwhBetaEnd();
			break;
		default:
			unsupportedEnd();
			break;
		}
	}
	
	@Override
	public void manageException(Exception e, ReportAction action) {
		
		switch(action) {
		case SEND:
			sendException(e);
			break;
		case REJECT:
		case SUBMIT:
			rejectSubmitException(e);
			break;
		default:
			break;
		}
	}
	
	private void rejectSubmitException(Exception e) {
		
		Message msg = null;
		
		if (e instanceof IOException || e instanceof ParserConfigurationException
				|| e instanceof SAXException) {
			msg = Warnings.createFatal(TSEMessages.get("report.io.error", PropertiesReader.getSupportEmail()), getReport());
		}
		else if (e instanceof SendMessageException) {
			msg = TSEWarnings.getSendMessageWarning((SendMessageException) e, getReport());
		}
		else if (e instanceof DetailedSOAPException) {
			msg = Warnings.createSOAPWarning((DetailedSOAPException) e);
		}
		else if (e instanceof ReportException) {
			String title = TSEMessages.get("error.title");
			String message = TSEMessages.get("report.unsupported.action");
			msg = Warnings.create(title, message, SWT.ICON_ERROR);
		}
		else {
			msg = Warnings.createFatal(TSEMessages.get("generic.error", PropertiesReader.getSupportEmail()), getReport());
		}
		
		if (msg != null)
			msg.open(shell);
	}
	
	private void sendException(Exception e) {
		
		Message msg = null;
		
		if (e instanceof IOException) {
			msg = Warnings.createFatal(TSEMessages.get("report.io.error", PropertiesReader.getSupportEmail()), getReport());
		}
		else if (e instanceof DetailedSOAPException) {
			msg = Warnings.createSOAPWarning((DetailedSOAPException) e);
		}
		else if (e instanceof SAXException || e instanceof ParserConfigurationException) {
			msg = Warnings.createFatal(TSEMessages.get("gde2.missing", AppPaths.MESSAGE_GDE2_XSD, 
					PropertiesReader.getSupportEmail()), getReport());
		}
		else if (e instanceof SendMessageException) {
			
			SendMessageException sendE = (SendMessageException) e;
			
			msg = TSEWarnings.getSendMessageWarning(sendE, getReport());
		}
		else if (e instanceof ReportException) {
			msg = Warnings.createFatal(TSEMessages.get("send.failed.no.senderId", PropertiesReader.getSupportEmail()), getReport());
		}
		else if (e instanceof NotOverwritableDcfDatasetException) {
			msg = getUnsupportedOpWarning(((NotOverwritableDcfDatasetException) e).getDataset());
		}
		else if (e instanceof AmendException) {
			msg = Warnings.create(TSEMessages.get("report.send.empty.error"));
		}
		else {
			msg = Warnings.createFatal(TSEMessages.get("generic.error", PropertiesReader.getSupportEmail()), getReport());
		}
		
		if (msg != null)
			msg.open(shell);
	}
	
	private void unsupportedEnd() {
		String title = TSEMessages.get("error.title");
		String message = TSEMessages.get("report.unsupported.action");
		Warnings.warnUser(shell, title, message);
	}
	
	private void sendEnd() {
		String title = TSEMessages.get("success.title");
		String message = TSEMessages.get("send.success");
		int icon = SWT.ICON_INFORMATION;
		
		Warnings.warnUser(shell, title, message, icon);
	}
	
	private void rejectEnd() {
		String title = TSEMessages.get("success.title");
		String message = TSEMessages.get("reject.success");
		int style = SWT.ICON_INFORMATION;
		
		Warnings.warnUser(shell, title, message, style);
	}

	private void submitEnd() {
		String title = TSEMessages.get("success.title");
		String message = TSEMessages.get("submit.success");
		int style = SWT.ICON_INFORMATION;
		
		Warnings.warnUser(shell, title, message, style);
	}
	
	//shahaal
	private void acceptedDwhBetaEnd() {
		String title = TSEMessages.get("success.title");
		String message = TSEMessages.get("acceptedDwhBeta.success");
		int style = SWT.ICON_INFORMATION;
		
		Warnings.warnUser(shell, title, message, style);
	}
	
	private Message getUnsupportedOpWarning(Dataset dataset) {
		
		String datasetId = dataset.getId();

		String title = TSEMessages.get("error.title");
		String message;
		boolean fatal = false;
		
		switch(dataset.getRCLStatus()) {
		case ACCEPTED_DWH:
			message = TSEMessages.get("send.warning.acc.dwh", datasetId);
			break;
		case SUBMITTED:
			message = TSEMessages.get("send.warning.submitted", datasetId);
			break;
		case PROCESSING:
			message = TSEMessages.get("send.warning.processing", datasetId);
			break;
		default:
			message = TSEMessages.get("send.error.acc.dcf", PropertiesReader.getSupportEmail());
			fatal = true;
			break;
		}
		
		return fatal ? Warnings.createFatal(message, getReport()) : Warnings.create(title, message, SWT.ICON_ERROR);
	}
	
	/**
	 * Warning based on the required operation and on the status of the dataset
	 * @param shell
	 * @param operation
	 * @return
	 */
	public boolean showSendWarning(Shell shell, ReportSendOperation operation) {
		
		boolean goOn = true;
		
		Message msg = null;
		
		String title = null;
		String message = null;
		int style = SWT.ICON_ERROR;
		boolean needConfirmation = false;
		
		// new dataset
		if (operation.getStatus() == null)
			return true;
		
		String datasetId = operation.getDataset().getId();
		
		switch(operation.getStatus()) {
		case ACCEPTED_DWH:
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("send.warning.acc.dwh", datasetId);
			msg = Warnings.create(title, message);
			goOn = false;
			break;
		case SUBMITTED:
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("send.warning.submitted", datasetId);
			msg = Warnings.create(title, message);
			goOn = false;
			break;
		case PROCESSING:
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("send.warning.processing", datasetId);
			msg = Warnings.create(title, message);
			goOn = false;
			break;
		case REJECTED_EDITABLE:
		case VALID:
		case VALID_WITH_WARNINGS:
			
			title = TSEMessages.get("warning.title");
			message = TSEMessages.get("send.warning.replace", datasetId, operation.getStatus().getLabel());
			style = SWT.YES | SWT.NO | SWT.ICON_WARNING;
			needConfirmation = true;
			msg = Warnings.create(title, message, style);
			break;
		case REJECTED:
		case DELETED:
			// Do nothing, just avoid the default case
			goOn = true;

			break;
		default:
			title = TSEMessages.get("error.title");
			message = TSEMessages.get("send.error.acc.dcf", PropertiesReader.getSupportEmail());
			msg = Warnings.createFatal(message, operation.getDataset());
			goOn = false;
			break;
		}
		
		if (msg != null) {
			
			int val = msg.open(shell);
			
			// if the caller need confirmation
			if (needConfirmation) {
				
				goOn = val == SWT.YES;
			}
		}
		
		// default answer is no
		return goOn;
	}

	@Override
	public boolean askReplaceConfirmation(Dataset dataset) {
		
		String datasetId = dataset.getId();
		String status = dataset.getRCLStatus().getLabel();
		
		String title = TSEMessages.get("warning.title");
		String message = TSEMessages.get("send.warning.replace", datasetId, status);
		int style = SWT.YES | SWT.NO | SWT.ICON_WARNING;
		
		int val = Warnings.warnUser(shell, title, message, style);
		
		return val == SWT.YES;
	}

	@Override
	public boolean askConfirmation(ReportAction action) {
		
		String title = TSEMessages.get("warning.title");
		String message = null;
		switch(action) {
		case SUBMIT:
			message = TSEMessages.get("submit.confirm");
			break;
		case REJECT:
			message = TSEMessages.get("reject.confirm");
			break;
		case SEND:
			message = TSEMessages.get("send.confirm");
			break;
		case ACCEPTED_DWH_BETA:
			message = TSEMessages.get("beta_accepted_dhw.confirm");
			break;
		case AMEND:
			message = TSEMessages.get("amend.confirm");
			break;
		default:
			break;
		}
		
		if (message == null)
			return false;
		
		int val = Warnings.warnUser(shell, title, 
				message,
				SWT.ICON_WARNING | SWT.YES | SWT.NO);
		
		return val == SWT.YES;
	}
}

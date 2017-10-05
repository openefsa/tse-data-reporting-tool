package tse_main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;

import org.xml.sax.SAXException;

import app_config.AppPaths;
import app_config.DebugConfig;
import dataset.Dataset;
import dataset.DatasetParser;
import dataset.DatasetStatus;
import message.MessageResponse;
import message.SendMessageException;
import message_creator.DatasetXmlCreator;
import message_creator.OperationType;
import table_relations.Relation;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import tse_config.CustomStrings;
import webservice.GetDataset;
import webservice.SendMessage;

public class FileActions {

	/**
	 * Export the report and send it to the dcf. It also saves the dataset id
	 * and message id into the report data
	 * @param report
	 * @throws IOException
	 * @throws SOAPException
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws SendMessageException 
	 */
	public static void exportAndSendReport(TableRow report) 
			throws IOException, SOAPException, ParserConfigurationException, 
			SAXException, SendMessageException {
		
		// set report filename
		String filename = "report-" + getTodayTimestamp() + ".xml";
		
		System.out.println("Exporting report in " + filename);
		
		// export the report and get an handle to the exported file
		File file = exportReport(report, filename);
		
		try {
			
			System.out.println("Sending report " + filename);
			
			// send the report and get the response to the message
			MessageResponse response = sendReport(file);

			// delete file (not needed anymore)
			
			if (!DebugConfig.debug)
				file.delete();

			// if correct response then save the message id
			// into the report
			if (response.isCorrect()) {
				
				// save the message id
				report.put(CustomStrings.REPORT_MESSAGE_ID, response.getMessageId());
				report.put(CustomStrings.REPORT_STATUS, DatasetStatus.UPLOADED.getStatus());
				report.update();
			}
			else {
				
				// set upload failed status if message is not valid
				report.put(CustomStrings.REPORT_STATUS, DatasetStatus.UPLOAD_FAILED.getStatus());
				report.update();
				
				throw new SendMessageException(response);
			}
		}
		catch (SOAPException e) {
			
			// delete file also if exception occurs
			if (!DebugConfig.debug)
				file.delete();
			
			throw e;
		}
	}

	
	/**
	 * Export a report into a file
	 * @param report
	 * @param filename
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static File exportReport(TableRow report, String filename) 
			throws IOException, ParserConfigurationException, SAXException {

		File file = new File(AppPaths.TEMP_FOLDER + filename);

		// instantiate xml creator and inject the required parents
		// of the configuration table (in order to be able to
		// retrieve the data for the message header)
		DatasetXmlCreator creator = new DatasetXmlCreator(file) {

			@Override
			public Collection<TableRow> getConfigMessageParents() {

				Collection<TableRow> parents = new ArrayList<>();

				// add the report data
				parents.add(report);

				// add the settings data
				try {
					parents.add(Relation.getGlobalParent(CustomStrings.SETTINGS_SHEET));
				} catch (IOException e) {
					e.printStackTrace();
				}

				return parents;
			}
		};

		// set the correct operation type required
		OperationType opType = getSendOperation(report);
		creator.setOpType(opType);
		
		// export the report
		creator.export(report);
		
		return file;
	}
	
	/**
	 * Given a report and its state, get the operation
	 * that is correct for sending it to the dcf.
	 * For example, if the report was never sent then the operation
	 * will be {@link OperationType#INSERT}.
	 * @param report
	 * @return
	 */
	private static OperationType getSendOperation(TableRow report) {
		
		OperationType opType = OperationType.NOT_SUPPORTED;
		
		TableColumnValue datasetId = report.get(CustomStrings.REPORT_DATASET_ID);
		TableColumnValue datasetStatus = report.get(CustomStrings.REPORT_STATUS);

		// if dataset never sent
		if (datasetId == null || datasetId.isEmpty()) {
			opType = OperationType.INSERT;
		}
		else {
			
			// if it was sent, check dataset status
			if (datasetStatus == null || datasetStatus.isEmpty())
				return opType;
			
			// get the status of the dataset
			DatasetStatus status = DatasetStatus.fromString(datasetStatus.getLabel());
			
			switch (status) {
			case REJECTED:
			case REJECTED_EDITABLE:
			case VALID:
			case VALID_WITH_WARNINGS:
				opType = OperationType.REPLACE;
				break;
			case DELETED:
				opType = OperationType.INSERT;
				break;
			default:
				opType = OperationType.NOT_SUPPORTED;
				break;
			}
		}
		
		return opType;
	}
	
	/**
	 * Send a report to the dcf
	 * @param file
	 * @return
	 * @throws SOAPException
	 */
	private static MessageResponse sendReport(File file) throws SOAPException {
		SendMessage req = new SendMessage(file);
		MessageResponse response = req.send();
		return response;
	}
	
	/**
	 * Get a string which contains the today timestamp in format: yyyyMMdd-HHmmss
	 * @return
	 */
	private static String getTodayTimestamp() {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
		String todayTs = sdf.format(System.currentTimeMillis());
		return todayTs;
	}
}

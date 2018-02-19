package providers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import dataset.Dataset;
import dataset.IDataset;
import dataset.RCLDatasetStatus;
import message.MessageConfigBuilder;
import soap_interface.IGetAck;
import soap_interface.IGetDataset;
import soap_interface.IGetDatasetsList;
import soap_interface.ISendMessage;
import table_relations.Relation;
import table_skeleton.TableRow;
import table_skeleton.TableVersion;
import tse_analytical_result.AnalyticalResult;
import tse_case_report.CaseReport;
import tse_config.CustomStrings;
import tse_report.TseReport;
import tse_summarized_information.SummarizedInfo;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

public class TseReportService extends ReportService {

	private static final Logger LOGGER = LogManager.getLogger(TseReportService.class);
	
	private IFormulaService formulaService;
	
	public TseReportService(IGetAck getAck, IGetDatasetsList<IDataset> getDatasetsList, 
			ISendMessage sendMessage, IGetDataset getDataset, ITableDaoService daoService, IFormulaService formulaService) {
		super(getAck, getDatasetsList, sendMessage, getDataset, daoService);
		
		this.formulaService = formulaService;
	}

	public MessageConfigBuilder getSendMessageConfiguration(TseReport report) {
		
		Collection<TableRow> messageParents = new ArrayList<>();

		// add the report data
		messageParents.add(report);

		// add the settings data
		try {
			
			TableRow settings = Relation.getGlobalParent(
					CustomStrings.SETTINGS_SHEET, getDaoService());
					
			messageParents.add(settings);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		MessageConfigBuilder builder = new MessageConfigBuilder(formulaService, messageParents);

		return builder;
	}
	
	/**
	 * Create a new version of the report and save it into the database.
	 * The version is automatically increased
	 * @return
	 */
	public TseReport amend(TseReport report) {

		TseReport amendedReport = new TseReport();
		amendedReport.copyValues(report);
		
		Stack<TableRow> elements = new Stack<>();
		elements.add(amendedReport);
		
		SummarizedInfo summInfo = null;
		CaseReport caseReport = null;
		AnalyticalResult result = null;
		while (!elements.isEmpty()) {
			
			TableRow currentElement = elements.pop();
			
			boolean isReport = currentElement.getSchema().equals(TableSchemaList.getByName(CustomStrings.REPORT_SHEET));
			boolean isSumm = currentElement.getSchema().equals(TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET));
			boolean isCase = currentElement.getSchema().equals(TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET));
			boolean isResult = currentElement.getSchema().equals(TableSchemaList.getByName(CustomStrings.RESULT_SHEET));
			
			TableSchema childSchema = null;
			if (isReport) {
				childSchema = TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET);
			}
			else if (isSumm) {
				childSchema = TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET);
			}
			else if (isCase) {
				childSchema = TableSchemaList.getByName(CustomStrings.RESULT_SHEET);
			}
			
			// get the element children (before changing its id)
			Collection<TableRow> children = null;

			if (!isResult) {
				children = getDaoService().getByParentId(childSchema, 
						currentElement.getSchema().getSheetName(), 
						currentElement.getDatabaseId(), true);
			}
			
			if (isReport) {
				
				// get current version
				String currentVersion = report.getVersion();
				
				// increase version starting from the current
				String newVersion = TableVersion.createNewVersion(currentVersion);

				amendedReport.setVersion(newVersion);
				
				// new version is in draft
				amendedReport.setStatus(RCLDatasetStatus.DRAFT);
				
				amendedReport.setId("");
				amendedReport.setMessageId("");
				getDaoService().add(amendedReport);
			}
			else if (isSumm) {
				summInfo = new SummarizedInfo();
				summInfo.copyValues(currentElement);
				Relation.injectParent(amendedReport, summInfo);
				getDaoService().add(summInfo);
			}
			else if (isCase) {
				caseReport = new CaseReport();
				caseReport.copyValues(currentElement);
				Relation.injectParent(amendedReport, caseReport);
				Relation.injectParent(summInfo, caseReport);
				getDaoService().add(caseReport);
			}
			else if (isResult) {
				result = new AnalyticalResult();
				result.copyValues(currentElement);
				Relation.injectParent(amendedReport, result);
				Relation.injectParent(summInfo, result);
				Relation.injectParent(caseReport, result);
				getDaoService().add(result);
			}

			// add the children
			if (!isResult)
				elements.addAll(children);
		}
		
		return amendedReport;
	}
	
	/**
	 * Create a report from a dataset
	 * @param dataset
	 * @return
	 */
	public TseReport reportFromDataset(Dataset dataset) {
		
		TseReport report = new TseReport();
		
		String senderDatasetId = dataset.getOperation().getSenderDatasetId();
		
		report.setId(dataset.getId());
		
		String[] split = Dataset.splitSenderId(senderDatasetId);
		
		String senderId = senderDatasetId;
		String version = null;
		if (split != null && split.length > 1) {
			senderId = split[0];
			version = split[1];
			report.setVersion(version);
		}
		else {
			report.setVersion(TableVersion.getFirstVersion());
		}
		
		report.setSenderId(senderId);
		
		if (dataset.getRCLStatus() != null)
			report.setStatus(dataset.getRCLStatus());
		else
			report.setStatus(RCLDatasetStatus.DRAFT);
		
		// split FR1705... into country year and month
		if (senderId.length() < 6) {
			LOGGER.error("Report#fromDataset Cannot parse sender dataset id, expected at least 6 characters, found " 
					+ senderId);
			report.setCountry("");
			report.setYear("");
			report.setMonth("");
		}
		else {
			
			String countryCode = senderDatasetId.substring(0, 2);
			String year = "20" + senderDatasetId.substring(2, 4);
			String month = senderDatasetId.substring(4, 6);
			
			// remove the padding
			if (month.substring(0, 1).equals("0"))
				month = month.substring(1, 2);
			
			report.setCountry(countryCode);
			report.setYear(year);
			report.setMonth(month);
		}

		report.setMessageId("");
		
		// add the preferences
		try {
			Relation.injectGlobalParent(report, CustomStrings.PREFERENCES_SHEET, getDaoService());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return report;
	}
}

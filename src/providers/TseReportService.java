package providers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

import dataset.IDataset;
import dataset.RCLDatasetStatus;
import message.MessageConfigBuilder;
import soap_interface.IGetAck;
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

	private IFormulaService formulaService;
	
	public TseReportService(IGetAck getAck, IGetDatasetsList<IDataset> getDatasetsList, 
			ISendMessage sendMessage, ITableDaoService daoService, IFormulaService formulaService) {
		super(getAck, getDatasetsList, sendMessage, daoService);
		
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
}

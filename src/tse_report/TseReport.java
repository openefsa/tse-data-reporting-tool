package tse_report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import app_config.AppPaths;
import dataset.DcfDatasetStatus;
import dataset.RCLDatasetStatus;
import report.EFSAReport;
import report.Report;
import report.ReportList;
import table_database.TableDao;
import table_relations.Relation;
import table_skeleton.TableRow;
import table_skeleton.TableVersion;
import tse_config.CatalogLists;
import tse_config.CustomStrings;
import tse_summarized_information.SummarizedInfo;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;

/**
 * A TSE report
 * @author avonva
 *
 */
public class TseReport extends Report {
	
	public TseReport() {
		super(getReportSchema());
	}
	
	public TseReport(TableRow row) {
		super(row);
	}
	
	public static TableSchema getReportSchema() {
		return TableSchemaList.getByName(CustomStrings.REPORT_SHEET);
	}
	
	public static TseReport createDefault() throws IOException {
		
		TseReport report = new TseReport();
		report.setCountry("Default");
		report.setSenderId("Default");
		report.setStatus(RCLDatasetStatus.DRAFT);
		report.setMonth("");
		report.setYear("");
		report.setVersion(TableVersion.getFirstVersion());
		report.setMessageId("");
		report.setId("");
		
		Relation.injectGlobalParent(report, CustomStrings.PREFERENCES_SHEET);
		
		return report;
	}
	
	@Override
	public Collection<TableRow> getRecords() {

		// children schemas
		TableSchema[] schemas = new TableSchema[] {
				TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET),
				TableSchemaList.getByName(CustomStrings.RESULT_SHEET)
		};

		Collection<TableRow> records = getRecords(schemas);
		
		// remove random genotyping from the summarized information
		List<TableRow> filteredRecords = records.stream().filter(new Predicate<TableRow>() {
			@Override
			public boolean test(TableRow arg0) {
				
				if (SummarizedInfo.isSummarizedInfo(arg0)) {
					
					String type = arg0.getCode(CustomStrings.SUMMARIZED_INFO_TYPE);
					
					if (type.equals(CustomStrings.SUMMARIZED_INFO_RGT_TYPE))
						return false;
				}
				
				return true;
			}
		}).collect(Collectors.toList());
		
		return filteredRecords;
	}
	
	/**
	 * Get all the elements of the report (summ info, case, analytical results)
	 * @return
	 */
	public Collection<TableRow> getAllRecords() {

		// children schemas
		TableSchema[] schemas = new TableSchema[] {
				TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET),
				TableSchemaList.getByName(CustomStrings.CASE_INFO_SHEET),
				TableSchemaList.getByName(CustomStrings.RESULT_SHEET)
		};

		return getRecords(schemas);
	}
	
	public Collection<TableRow> getRecords(TableSchema schema) {
		TableSchema[] schemas = new TableSchema[] {schema};
		return getRecords(schemas);
	}
	
	public Collection<TableRow> getRecords(TableSchema[] schemas) {
		
		Collection<TableRow> records = new ArrayList<>();
		
		// for each child schema get the rows related to the report
		for (TableSchema schema : schemas) {
			
			TableDao dao = new TableDao();
			
			Collection<TableRow> children = dao.getByParentId(schema, CustomStrings.REPORT_SHEET, 
					this.getDatabaseId(), true, "desc");
			
			records.addAll(children);
		}

		return records;
	}

	/**
	 * Get all the report versions that are stored locally
	 */
	public ReportList getAllVersions() {

		ReportList allVersions = new ReportList();
		
		TableDao dao = new TableDao();
		
		// get all the versions of the report by the dataset sender id
		Collection<TableRow> reports = dao.getByStringField(this.getSchema(), AppPaths.REPORT_SENDER_ID, 
				this.getSenderId());
		
		for (TableRow report : reports) {
			allVersions.add(new TseReport(report));
		}
		
		return allVersions;
	}

	@Override
	public EFSAReport getPreviousVersion() {
		
		String currentVersion = this.getVersion();
		
		ReportList allVersions = getAllVersions();

		// sort starting from the newest
		allVersions.sort();
		
		// search the previous version if present
		EFSAReport previousVersion = null;
		
		boolean isNext = false;
		for (EFSAReport report : allVersions) {

			if (isNext) {
				previousVersion = report;
				break;
			}
			
			if (report.getVersion().equals(currentVersion))
				isNext = true;
		}
		
		return previousVersion;
	}
	
	public void setCountry(String country) {
		this.put(CustomStrings.REPORT_COUNTRY, 
				getTableColumnValue(country, CatalogLists.COUNTRY_LIST));
	}
	
	public String getCountry() {
		return this.getCode(CustomStrings.REPORT_COUNTRY);
	}
	
	@Override
	public String getRowIdFieldName() {
		return CustomStrings.RES_ID_COLUMN;
	}
	
	/**
	 * Check if the dataset can be edited or not
	 * @return
	 */
	public boolean isEditable() {
		return getRCLStatus().isEditable();
	}

	@Override
	public String getDecomposedSenderId() {
		return this.getSenderId();  // for report is already decomposed
	}

	@Override
	public void setStatus(DcfDatasetStatus status) {
		try {
			throw new UnsupportedOperationException("Cannot use this method");
		}
		catch(UnsupportedOperationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public DcfDatasetStatus getStatus() {
		
		try {
			throw new UnsupportedOperationException("Cannot use this method");
		}
		catch(UnsupportedOperationException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}

package tse_main;

import providers.IFormulaService;
import providers.ITableDaoService;
import table_importer.TableImporter;
import table_skeleton.TableRow;
import tse_config.CustomStrings;

/**
 * Copy the summarized information of a report into another report.
 * Note that data related to samples, prog id and res id are reset.
 * @author avonva
 *
 */
public class TseSummarizedInfoImporter extends TableImporter {

	private IFormulaService formulaService;
	
	public TseSummarizedInfoImporter(ITableDaoService daoService, IFormulaService formulaService) {
		super(daoService);
		this.formulaService = formulaService;
	}
	
	@Override
	public void filterRowData(TableRow row) {
		
		// remove declared data related to samples
		row.put(CustomStrings.TOT_SAMPLE_INCONCLUSIVE_COL, "0");
		row.put(CustomStrings.TOT_SAMPLE_POSITIVE_COL, "0");
		row.put(CustomStrings.TOT_SAMPLE_UNSUITABLE_COL, "0");
		row.put(CustomStrings.TOT_SAMPLE_NEGATIVE_COL, "0");
		row.put(CustomStrings.TOT_SAMPLE_TESTED_COL, "0");

		this.formulaService.Initialise(row, CustomStrings.PROG_ID_COL);
		this.formulaService.Initialise(row, CustomStrings.RES_ID_COL);
	}
}

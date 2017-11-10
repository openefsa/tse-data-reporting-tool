package tse_main;

import table_importer.TableImporter;
import table_skeleton.TableRow;
import tse_config.CustomStrings;

public class TseSummarizedInfoImporter extends TableImporter {

	@Override
	public void filterRowData(TableRow row) {
		
		// remove declared data related to samples
		row.put(CustomStrings.SUMMARIZED_INFO_INC_SAMPLES, "0");
		row.put(CustomStrings.SUMMARIZED_INFO_POS_SAMPLES, "0");
		row.put(CustomStrings.SUMMARIZED_INFO_UNS_SAMPLES, "0");
		row.put(CustomStrings.SUMMARIZED_INFO_NEG_SAMPLES, "0");
		row.put(CustomStrings.SUMMARIZED_INFO_TOT_SAMPLES, "0");
		row.initialize(CustomStrings.RESULT_PROG_ID);
		row.initialize(CustomStrings.RES_ID_COLUMN);
	}
}

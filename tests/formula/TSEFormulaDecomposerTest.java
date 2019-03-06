package formula;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.HashMap;

import org.junit.Test;

import report_downloader.TSEFormulaDecomposer;
import table_skeleton.TableCell;
import tse_config.CustomStrings;

public class TSEFormulaDecomposerTest {

	@Test
	public static void decomposeNameValueField() throws ParseException {
		TSEFormulaDecomposer decomposer = new TSEFormulaDecomposer();
		HashMap<String, TableCell> values = decomposer.decompose(CustomStrings.SAMP_UNIT_IDS_COL, "PSUId=my samp");
		
		assertTrue(values.containsKey(CustomStrings.PSU_ID_COL));
		assertNotNull(values.get(CustomStrings.PSU_ID_COL));
		assertEquals("my samp", values.get(CustomStrings.PSU_ID_COL).getCode());
		assertEquals("my samp", values.get(CustomStrings.PSU_ID_COL).getLabel());
	}
}

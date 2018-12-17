package report_downloader;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import formula.AttributeElement;
import formula.FoodexElement;
import formula.FormulaDecomposer;
import table_skeleton.TableCell;
import tse_config.CustomStrings;

public class TSEFormulaDecomposer extends FormulaDecomposer {

	private static final Logger LOGGER = LogManager.getLogger(TSEFormulaDecomposer.class);
	
	public HashMap<String, TableCell> decompose(String columnId, String rowValue) throws ParseException {

		HashMap<String, TableCell> values = new HashMap<>();

		if (rowValue.isEmpty())
			return values;
		
		switch(columnId) {
		case CustomStrings.SAMP_MAT_CODE_COL:
			values = splitFoodexWithHeaders(rowValue);
			break;
		case CustomStrings.PROG_INFO_COL:
		case CustomStrings.EVAL_INFO_COL:
		case CustomStrings.SAMP_UNIT_IDS_COL:
		case CustomStrings.SAMP_EVENT_INFO_COL:
		case CustomStrings.SAMP_MAT_INFO_COL:
		case CustomStrings.SAMP_INFO_COL:
			values = splitSimple(rowValue);
			break;
		case CustomStrings.PARAM_CODE_COL:
			values = splitAlleles(rowValue);
			break;
		}
		
		return values;
	}
	
	/**
	 * Split a simple attribute field with names
	 * @param value
	 * @return
	 * @throws ParseException 
	 */
	private HashMap<String, TableCell> splitSimple(String value) throws ParseException {
	
		HashMap<String, TableCell> rowValues = new HashMap<>();
		
		Collection<AttributeElement> list = this.split(value);

		for (AttributeElement elem : list) {
			
			TableCell colVal = new TableCell();
			colVal.setCode(elem.getValue());
			colVal.setLabel(elem.getValue());
			
			rowValues.put(elem.getId(), colVal);
		}

		return rowValues;
	}
	
	private HashMap<String, TableCell> splitAlleles(String value) throws ParseException {
		
		FoodexElement foodexCode = this.splitFoodex(value, 
				AttributeIdentifier.NAME_VALUE);
		
		HashMap<String, TableCell> rowValues = new HashMap<>();
		
		int count = 0;
		for (AttributeElement facet : foodexCode.getFacetList()) {
			
			if (facet.getId().equals("allele")) {
				
				TableCell colVal = new TableCell();
				colVal.setCode(facet.getValue());
				
				String colId = count == 0 ? CustomStrings.ALLELE_1_COL : CustomStrings.ALLELE_2_COL;
				
				// save value with id the attribute id
				rowValues.put(colId, colVal);
				
				count++;
			}
		}
		
		return rowValues;
	}
	
	/**
	 * Split a foodex code with headers and return the row values
	 * related to them
	 * @param value
	 * @return
	 * @throws ParseException 
	 */
	private HashMap<String, TableCell> splitFoodexWithHeaders(String value) throws ParseException {
		
		FoodexElement foodexCode = this.splitFoodex(value, 
				AttributeIdentifier.FACET_HEADER);
		
		HashMap<String, TableCell> rowValues = new HashMap<>();
		
		for (AttributeElement facet : foodexCode.getFacetList()) {
			
			String facetHeader = facet.getId();
			
			String columnId = getColumnByHeader(facetHeader);
			
			if (columnId == null) {
				LOGGER.error("Not supported facet header " + facetHeader);
				continue;
			}
			
			TableCell colVal = new TableCell();
			colVal.setCode(facet.getId() + "." + facet.getValue());  // we want both header and value for code
			
			// save the mapping between the row column and the extracted value
			rowValues.put(columnId, colVal);
		}
		
		return rowValues;
	}
	
	/**
	 * get the id of the column of the row schema which contains the
	 * facet code related to the facet header
	 * @param header
	 * @return
	 */
	private String getColumnByHeader(String header) {
		
		String columnId = null;
		
		switch(header) {
		case "F01":
			columnId = CustomStrings.SOURCE_COL;
			break;
		case "F02":
			columnId = CustomStrings.PART_COL;
			break;
		case "F21":
			columnId = CustomStrings.PROD_COL;
			break;
		case "F31":
			columnId = CustomStrings.ANIMAGE_COL;
			break;
		case "F32":
			columnId = CustomStrings.SEX_COL;
			break;
		default:
			break;
		}
		
		return columnId;
	}
}

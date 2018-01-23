package report_downloader;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;

import formula.AttributeElement;
import formula.FoodexElement;
import formula.FormulaDecomposer;
import table_skeleton.TableCell;
import table_skeleton.TableRow;
import tse_config.CustomStrings;

public class TSEFormulaDecomposer extends FormulaDecomposer {

	private TableRow row;
	
	public TSEFormulaDecomposer(TableRow row) {
		this.row = row;
	}
	
	public HashMap<String, TableCell> decompose(String columnId) throws ParseException {
		
		HashMap<String, TableCell> values = new HashMap<>();
		
		String rowValue = row.getCode(columnId);
		
		if (rowValue.isEmpty())
			return values;
		
		switch(columnId) {
		case CustomStrings.SUMMARIZED_INFO_SAMP_MAT_CODE:
			values = splitFoodexWithHeaders(rowValue);
			break;
		case CustomStrings.SUMMARIZED_INFO_PROG_INFO:
		case CustomStrings.RESULT_EVAL_INFO:
		case CustomStrings.RESULT_SAMP_UNIT_IDS:
		case CustomStrings.RESULT_SAMP_EVENT_INFO:
		case CustomStrings.RESULT_SAMP_MAT_INFO:
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
				
				String colId = count == 0 ? CustomStrings.RESULT_ALLELE_1 : CustomStrings.RESULT_ALLELE_2;
				
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
				System.err.println("Not supported facet header " + facetHeader);
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
			columnId = CustomStrings.SUMMARIZED_INFO_SOURCE;
			break;
		case "F02":
			columnId = CustomStrings.SUMMARIZED_INFO_PART;
			break;
		case "F21":
			columnId = CustomStrings.SUMMARIZED_INFO_PROD;
			break;
		case "F31":
			columnId = CustomStrings.SUMMARIZED_INFO_AGE;
			break;
		default:
			break;
		}
		
		return columnId;
	}
}

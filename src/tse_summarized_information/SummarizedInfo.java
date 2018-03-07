package tse_summarized_information;

import formula.FormulaException;
import formula.FormulaSolver;
import table_skeleton.TableCell;
import table_skeleton.TableRow;
import tse_config.CatalogLists;
import tse_config.CustomStrings;
import xlsx_reader.TableHeaders.XlsxHeader;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;
import xml_catalog_reader.XmlLoader;

public class SummarizedInfo extends TableRow {

	public SummarizedInfo(TableRow row) {
		super(row);
	}
	
	public SummarizedInfo() {
		super(getSummarizedInfoSchema());
	}
	
	public SummarizedInfo(String typeColumnId, TableCell type) {
		super(getSummarizedInfoSchema());
		super.put(typeColumnId, type);
	}
	
	public SummarizedInfo(String typeColumnId, String type) {
		super(getSummarizedInfoSchema());
		super.put(typeColumnId, type);
	}

	public static boolean isSummarizedInfo(TableRow row) {
		return row.getSchema().equals(SummarizedInfo.getSummarizedInfoSchema());
	}
	
	public static TableSchema getSummarizedInfoSchema() {
		return TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET);
	}
	
	public String getSpecies() {
		return this.getCode(CustomStrings.SOURCE_COL);
	}
	
	public String getType() {
		return this.getCode(CustomStrings.SUMMARIZED_INFO_TYPE);
	}
	
	public boolean isRGT() {
		return this.getType().equals(CustomStrings.SUMMARIZED_INFO_RGT_TYPE);
	}
	
	public boolean isBSEOS() {
		return this.getType().equals(CustomStrings.SUMMARIZED_INFO_BSEOS_TYPE);
	}
	
	public String getProgId() {
		return this.getLabel(CustomStrings.PROG_ID_COL);
	}
	
	public String computeContextId() throws FormulaException {
		FormulaSolver solver = new FormulaSolver(this);
		return solver.solve(this.getSchema().getById(CustomStrings.CONTEXT_ID_COL), 
				XlsxHeader.LABEL_FORMULA.getHeaderName()).getSolvedFormula();
	}
	
	public void setType(String type) {
		this.put(CustomStrings.SUMMARIZED_INFO_TYPE, 
				getTableColumnValue(type, CatalogLists.TSE_LIST));
	}
	
	/**
	 * Get the animal type of the summ info using the species field
	 * @return
	 */
	public String getTypeBySpecies() {
		
		String species = getSpecies();

		// get the type whose species is the current one
		String listId = XmlLoader.getByPicklistKey(CatalogLists.SPECIES_LIST)
				.getElementByCode(species).getListId();
		
		return listId;
	}
}

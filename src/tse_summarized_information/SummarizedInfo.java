package tse_summarized_information;

import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import tse_config.CatalogLists;
import tse_config.CustomStrings;
import xlsx_reader.TableSchema;
import xlsx_reader.TableSchemaList;
import xml_catalog_reader.XmlLoader;

public class SummarizedInfo extends TableRow {

	public SummarizedInfo(TableRow row) {
		super(row);
	}
	
	public SummarizedInfo(String typeColumnId, TableColumnValue type) {
		super(getSummarizedInfoSchema());
		super.put(typeColumnId, type);
	}

	public static TableSchema getSummarizedInfoSchema() {
		return TableSchemaList.getByName(CustomStrings.SUMMARIZED_INFO_SHEET);
	}
	
	public String getSpecies() {
		return this.getCode(CustomStrings.SUMMARIZED_INFO_SOURCE);
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

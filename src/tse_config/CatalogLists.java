package tse_config;

import table_dialog.RowCreatorViewer;

/**
 * Class that contains the main nodes of the .xml files
 * that contains the catalogues. Note that this should
 * be used only if you need to specify a particular list
 * for a UI component, as the {@link RowCreatorViewer}
 * @author avonva
 *
 */
public class CatalogLists {

	/**
	 * Name of the main node of the .xml which contains the list of tses
	 */
	public static final String TSE_LIST = "tseLists";
	public static final String SPECIES_LIST = "animalSpeciesLists";
	public static final String COUNTRY_LIST = "countryList";
	public static final String TEST_LIST = "anMethTypesLists";
	public static final String ALL_TEST_ID = "ALL";
	public static final String RESULTS_LISTS = "resultsLists";
	public static final String AGE_CLASS_LIST = "ageClassLists";
}

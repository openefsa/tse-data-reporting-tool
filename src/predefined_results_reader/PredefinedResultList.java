package predefined_results_reader;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import app_config.BooleanValue;

public class PredefinedResultList extends ArrayList<PredefinedResult> {
	
	private static final Logger LOGGER = LogManager.getLogger(PredefinedResultList.class);
	
	private static final long serialVersionUID = -6372192676663884532L;
	
	private static PredefinedResultList predefinedResultsCache;
	
	/**
	 * Get a predefined result using the record type and the samp an asses fields
	 * @param recordType
	 * @param sampAnAsses
	 * @return
	 */
	public PredefinedResult get(String recordType, String source, boolean confirmatoryTested, String sampAnAsses) {
		
		for (PredefinedResult prh : this) {
			
			String thisRecordType = prh.get(PredefinedResultHeader.RECORD_TYPE);
			String thisSource = prh.get(PredefinedResultHeader.SOURCE);
			String thisSampAnAsses = prh.get(PredefinedResultHeader.SAMP_AN_ASSES);
			String thisConfTested = prh.get(PredefinedResultHeader.CONFIRMATORY_EXECUTED);
			
			boolean confCheck = (BooleanValue.isTrue(thisConfTested) && confirmatoryTested)
					|| (BooleanValue.isFalse(thisConfTested) && !confirmatoryTested);

			if (isFieldEqual(thisRecordType, recordType)
					&& isFieldEqual(thisSource, source)
					&& isFieldEqual(thisSampAnAsses, sampAnAsses)
					&& confCheck) {
				return prh;
			}
		}
		
		return null;
	}
	
	/**
	 * Check if field a is equal to field b or not
	 * @param a
	 * @param b
	 * @return
	 */
	private boolean isFieldEqual(String predefResValue, String rowValue) {
		
		// always match an empty field in the configuration
		if(predefResValue == null || predefResValue.isEmpty() || predefResValue.equals("null")) {
			return true;
		}
		
		// no match if our value is null
		if (rowValue == null)
			return false;
		
		return rowValue.equals(predefResValue);
	}

	/**
	 * Get all the predefined results
	 * @return
	 * @throws IOException
	 */
	public static PredefinedResultList getAll() {
		
		// if first time
		if (predefinedResultsCache == null) {
			
			predefinedResultsCache = new PredefinedResultList();
			
			PredefinedResultsReader reader;
			
			try {
				
				reader = new PredefinedResultsReader();
				reader.readFirstSheet();
				
				predefinedResultsCache = reader.getResults();
				reader.close();
				
			} catch (IOException e) {
				e.printStackTrace();
				LOGGER.error("Cannot retrieve predefined results list", e);
			}
		}

		return predefinedResultsCache;
	}
}

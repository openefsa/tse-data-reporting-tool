package predefined_results_reader;

import java.io.IOException;
import java.util.ArrayList;

public class PredefinedResultList extends ArrayList<PredefinedResult> {
	private static final long serialVersionUID = -6372192676663884532L;
	
	
	private static PredefinedResultList predefinedResultsCache;
	
	/**
	 * Get a predefined result using the record type and the samp an asses fields
	 * @param recordType
	 * @param sampAnAsses
	 * @return
	 */
	public PredefinedResult get(String recordType, String sampAnAsses) {
		
		for (PredefinedResult prh : this) {
			
			String thisRecordType = prh.get(PredefinedResultHeader.RECORD_TYPE);
			String thisSampAnAsses = prh.get(PredefinedResultHeader.SAMP_AN_ASSES);
			
			if (thisRecordType.equals(recordType) && thisSampAnAsses.equals(sampAnAsses)) {
				return prh;
			}
		}
		
		return null;
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
			}
		}

		return predefinedResultsCache;
	}
}

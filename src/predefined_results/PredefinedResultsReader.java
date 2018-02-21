package predefined_results;

import java.io.IOException;

import org.apache.poi.ss.usermodel.Row;

import tse_config.CustomStrings;
import xlsx_reader.XlsxReader;

public class PredefinedResultsReader extends XlsxReader {

	private PredefinedResultList results;
	private PredefinedResult result;
	
	public PredefinedResultsReader() throws IOException {
		super(CustomStrings.PREDEFINED_RESULTS_FILE);
		this.results = new PredefinedResultList();
	}

	@Override
	public void processCell(String header, String value) {
		
		PredefinedResultHeader enumHeader = PredefinedResultHeader.fromString(header);
		if (enumHeader == null)
			return;
		
		result.put(enumHeader, value);
	}

	@Override
	public void startRow(Row row) {
		result = new PredefinedResult();
	}

	@Override
	public void endRow(Row row) {
		results.add(result);
		result = null;
	}
	
	/**
	 * Get the read results
	 * @return
	 */
	public PredefinedResultList getResults() {
		return results;
	}
	
	/*
	public static void main(String[] args) throws IOException {
		PredefinedResultsReader reader = new PredefinedResultsReader();
		reader.readFirstSheet();
		
		PredefinedResultList results = reader.getResults();
		reader.close();
	}*/
}

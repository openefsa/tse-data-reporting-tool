package html_viewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.poi.ss.usermodel.Row;

import user_config.AppPaths;
import xlsx_reader.XlsxReader;

/**
 * Read all the help files from the .xlsx
 * @author avonva
 *
 */
public class HelpParser extends XlsxReader {

	private String tableName;
	private String htmlFilename;
	
	private Collection<Help> helps;
	
	public HelpParser(String filename) throws IOException {
		super(filename);
		helps = new ArrayList<>();
	}
	
	/**
	 * Read all the relations and returns it
	 * @return
	 * @throws IOException
	 */
	public Collection<Help> read() throws IOException {
		super.read(AppPaths.HELP_SHEET);
		return helps;
	}
	
	public static boolean isHelpSheet(String sheetName) {
		return AppPaths.HELP_SHEET.equals(sheetName);
	}

	@Override
	public void processCell(String header, String value) {
		
		HelpHeader h = null;
		try {
			h = HelpHeader.fromString(header);  // get enum from string
		}
		catch(IllegalArgumentException e) {
			return;
		}

		if(h == null)
			return;
		
		switch(h) {
		case TABLE_NAME:
			this.tableName = value;
			break;
		case HTML_FILENAME:
			this.htmlFilename = value;
			break;
		}
	}

	@Override
	public void startRow(Row row) {}

	@Override
	public void endRow(Row row) {
		
		if(tableName == null || htmlFilename == null)
			return;
		
		// create a new help and put it into the collection
		Help help = new Help(tableName, htmlFilename);
		helps.add(help);
	}
}

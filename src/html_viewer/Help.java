package html_viewer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import tse_config.AppPaths;

public class Help {

	private String tableName;
	private String htmlFileName;
	
	public Help(String tableName, String htmlFileName) {
		this.tableName = tableName;
		this.htmlFileName = htmlFileName;
	}
	
	public String getTableName() {
		return tableName;
	}
	public String getHtmlFileName() {
		return htmlFileName;
	}
	
	/**
	 * Get the help related to the required table
	 * @param tableName
	 * @return
	 */
	public static Help getHelp(String tableName) {
		
		Help help = null;
		
		try {
			
			HelpParser parser = new HelpParser(AppPaths.TABLES_SCHEMA_FILE);
			
			Collection<Help> helps = parser.read();
			for (Help h : helps) {
				if (h.getTableName().equals(tableName))
					help = h;
			}
			
			parser.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return help;
	}
	
	/**
	 * Open the help in the html viewer
	 * @param shell
	 */
	public void open() {
		
		File file = new File(htmlFileName);
		HtmlViewer viewer = new HtmlViewer();
		viewer.open(file);
	}
}

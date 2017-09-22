package xml_catalog_reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import app_config.AppPaths;
import tse_config.CustomPaths;

/**
 * Load all the .xml contents which are contained in {@link CustomPaths#XML_FOLDER}
 * @author avonva
 *
 */
public class XmlLoader {

	// cache in memory to speed up
	private static Collection<XmlContents> contents = new ArrayList<>();
	
	/**
	 * Get a picklist by its identification key
	 * @param id
	 * @return
	 */
	public static XmlContents getByPicklistKey(String id) {
		
		// if empty, refresh contents
		if (contents.isEmpty()) {
			refresh();
		}

		for (XmlContents item : contents) {
			if (item.getCode().equals(id)) {
				return item;
			}
		}

		return null;
	}
	
	/**
	 * Refresh the xml contents
	 */
	private static void refresh() {
		
		File dir = new File(AppPaths.XML_FOLDER);
		
		// parse each xml and put it into the contents list
		for (File xml : dir.listFiles()) {
			
			try {
				// parse the xml file
				XmlParser parser = new XmlParser(xml);
				
				XmlContents doc = parser.parse();
				
				// save the parsed contents
				contents.add(doc);
				
				parser.close();
			} catch (XMLStreamException | IOException e) {
				e.printStackTrace();
			}
		}
	}
}

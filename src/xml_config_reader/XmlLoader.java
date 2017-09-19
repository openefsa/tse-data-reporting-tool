package xml_config_reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import app_config.AppPaths;

/**
 * Load all the .xml contents which are contained in {@link AppPaths#XML_FOLDER}
 * @author avonva
 *
 */
public class XmlLoader {

	// cache in memory to speed up
	private static Collection<XmlContents> contents = new ArrayList<>();
	
	public static XmlContents getByPicklistKey(String id) {
		
		// if empty, refresh contents
		if (contents.isEmpty()) {
			refresh();
		}
		
		for (XmlContents item : contents) {
			if (item.equals(id))
				return item;
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
				
				// save the parsed contents
				contents.add(parser.parse());
				
				parser.close();
			} catch (XMLStreamException | IOException e) {
				e.printStackTrace();
			}
		}
	}
}

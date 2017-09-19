package xml_config_reader;

import java.io.FileNotFoundException;

import javax.xml.stream.XMLStreamException;

/**
 * Test the xml parser
 * @author avonva
 *
 */
public class XmlParserTest {

	public static void main(String[] args) throws FileNotFoundException, XMLStreamException {
		XmlParser parser = new XmlParser(args[0]);
		XmlContents contents = parser.parse();
		System.out.println(contents);
	}
}

package xml_config_reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Parser for the .xml configuration files which provide the data that need
 * to be visualized.
 * @author avonva
 *
 */
public class XmlParser {

	private String filename = "";
	private boolean parsingDescriptionNode;  // true if we are parsing the description node
	private SelectionList selectionList;     // single selection list which is created step by step
	private Selection selection;             // single selection which is created step by step
	private XmlContents xmlContents;         // object which contains the .xml contents
	private InputStream input;               // input xml
	private XMLEventReader eventReader;      // xml parser
	private int elementCounter;              // count the number of element of the .xml
	
	/**
	 * Initialize the parser with a {@link File} object which
	 * contains the path to the file to parse
	 * @param file file which points to the .xml file to parse
	 * @throws FileNotFoundException
	 * @throws XMLStreamException
	 */
	public XmlParser(File file) throws FileNotFoundException, XMLStreamException {
		this(new FileInputStream(file));
		this.filename = file.getName();
	}
	
	/**
	 * Initialize the parser with a {@link String} object which
	 * contains the path to the file to parse
	 * @param filename path to the .xml file to parse
	 * @throws FileNotFoundException
	 * @throws XMLStreamException
	 */
	public XmlParser(String filename) throws FileNotFoundException, XMLStreamException {
		this(new File(filename));
		this.filename = filename;
	}
	
	/**
	 * Initialize the parser with an {@link InputStream} object
	 * @param input input stream which contains the .xml file to parse
	 * @throws XMLStreamException
	 */
	public XmlParser(InputStream input) throws XMLStreamException {
		
		this.input = input;
		this.xmlContents = new XmlContents();
		this.elementCounter = 0;
		
		// initialize xml parser
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_COALESCING, true);
		this.eventReader = factory.createXMLEventReader(input);
	}


	/**
	 * Parse the .xml document and get the contents in a java object
	 * {@link XmlContents}
	 * @throws XMLStreamException
	 */
	public XmlContents parse() throws XMLStreamException {
		
		// for each node of the xml
		while (eventReader.hasNext()) {

			// read the node
			XMLEvent event = eventReader.nextEvent();

			// actions based on the node type
			switch(event.getEventType()) {

			// if starting xml node
			case XMLStreamConstants.START_ELEMENT:
				start (event);
				break;

			// if looking the xml contents
			case XMLStreamConstants.CHARACTERS:
				parseCharacters (event);
				break;

			// if ending xml node
			case  XMLStreamConstants.END_ELEMENT:
				end(event);
				break;
			}
		}
		
		return this.xmlContents;
	}
	
	/**
	 * Parse the a node when it starts
	 * @param event
	 */
	private void start(XMLEvent event) {
		
		// count start element
		elementCounter++;
		
		StartElement startElement = event.asStartElement();

		String qName = startElement.getName().getLocalPart();
		
		// if we have the first element of the .xml, i.e. the main node
		// save it, it will identify the content of the .xml file
		if (elementCounter == 1) {
			xmlContents.setCode(qName);
			return;
		}
		
		switch(qName) {
		
		case XmlNodes.SELECTION_LIST:
			
			// create a new selection list
			this.selectionList = new SelectionList();
			
			// read the id of the selection list
			Attribute id = startElement.getAttributeByName(new QName(XmlNodes.SELECTION_LIST_ID_ATTR));
			
			// id is required
			if (id != null) {
				// set the id
				selectionList.setId(id.getValue());
				break;
			}
			
			// set the list code using the main node of the .xml
			selectionList.setListCode(xmlContents.getCode());
			
			break;
			
		case XmlNodes.SELECTION:
			
			// selection object cannot be outside of a selection list
			if(this.selectionList == null) {
				printError(XmlNodes.SELECTION + " cannot be outside of " + XmlNodes.SELECTION_LIST);
				break;
			}
			
			// create a new selection
			this.selection = new Selection();
			
			// read the code of the selection
			Attribute code = startElement.getAttributeByName(new QName(XmlNodes.SELECTION_CODE_ATTR));
			
			// id is required
			if (code == null) {
				printError(XmlNodes.SELECTION + " must have a code in the node attributes");
				break;
			}
			
			// set the code of the selection
			selection.setCode(code.getValue());
			
			// set the list id for the selection object
			selection.setListId(selectionList.getId());
			
			break;
			
		case XmlNodes.DESCRIPTION:
			
			// description cannot be outside of selection object
			if(this.selection == null) {
				printError(XmlNodes.DESCRIPTION + " cannot be outside of " + XmlNodes.SELECTION);
				break;
			}
			
			this.parsingDescriptionNode = true;
			
			break;
		default:
			break;
		}
	}
	
	/**
	 * Parse the characters of the xml
	 * @param event
	 */
	private void parseCharacters (XMLEvent event) {
		
		// get the xml node value
		String contents = event.asCharacters().getData();

		// cannot parse null content
		if (contents == null)
			return;

		// if we are parsing the description node
		// set the description for the selection
		if (parsingDescriptionNode) {
			selection.setDescription(contents);
		}
	}
	
	/**
	 * Parse a node when it ends
	 * @param event
	 */
	private void end (XMLEvent event) {
		
		// get the xml node
		EndElement endElement = event.asEndElement();
		String qName = endElement.getName().getLocalPart();
		
		switch (qName) {
		
		// add the selection list to the xml contents
		case XmlNodes.SELECTION_LIST:
			xmlContents.addElement(selectionList);
			selectionList = null;
			break;
			
		// add the selection to the list
		case XmlNodes.SELECTION:
			selectionList.add(selection);
			selection = null;
			break;
			
		// close description node
		case XmlNodes.DESCRIPTION:
			this.parsingDescriptionNode = false;
			break;
			
		default:
			break;
		}
	}
	
	/**
	 * Print an error in the console adding the filename to the text if present
	 * @param text
	 */
	private void printError(String text) {
		System.err.println(filename + ": " + text);
	}
	
	/**
	 * Close the parser
	 * @throws XMLStreamException 
	 * @throws IOException 
	 */
	public void close () throws XMLStreamException, IOException {
		
		if (eventReader != null)
			eventReader.close();

		eventReader = null;
		
		if (input != null)
			input.close();
	}
}

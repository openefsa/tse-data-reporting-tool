package webservice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.apache.xerces.dom.DOMInputImpl;
import org.apache.xerces.impl.xs.XSImplementationImpl;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.w3c.dom.Document;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import app_config.AppPaths;
import table_database.TableDao;
import table_skeleton.TableRow;
import tse_config.CustomPaths;
import xlsx_reader.TableSchema;

/**
 * Abstract class used to create soap requests and to process soap responses
 * @author avonva
 *
 */

public abstract class SOAPAction {

	private String namespace;

	/**
	 * Set the url where we make the request and the namespace of the request
	 * @param url
	 * @param namespace
	 */
	public SOAPAction(String namespace) {
		this.namespace = namespace;
	}

	/**
	 * Create the request and get the response. Process the response and return the results.
	 * @param soapConnection
	 * @return
	 * @throws SOAPException
	 */
	public Object makeRequest(String url) throws SOAPException {

		// Connect to the url
		SOAPConnectionFactory connectionFactory = SOAPConnectionFactory.newInstance();
		
		// open the connection
		SOAPConnection soapConnection = connectionFactory.createConnection();

		// create the request message
		SOAPMessage request = createRequest(soapConnection);

		// get the response
		SOAPMessage response = soapConnection.call(request, url);

		// close the soap connection
		soapConnection.close();
		
		// parse the response and get the result
		return processResponse(response);
	}

	/**
	 * Create the standard structure of a SOAPMessage, including the
	 * authentication block
	 * @param username
	 * @param password
	 * @return
	 * @throws SOAPException
	 */

	public SOAPMessage createTemplateSOAPMessage(String prefix) throws SOAPException {

		// create the soap message
		MessageFactory msgFactory = MessageFactory.newInstance();
		SOAPMessage soapMsg = msgFactory.createMessage();
		SOAPPart soapPart = soapMsg.getSOAPPart();
		
		// add the content type header
		soapMsg.getMimeHeaders().addHeader("Content-Type", "text/xml;charset=UTF-8");
		
		// set the username and password for the https connection
		// in order to be able to authenticate me to the DCF
		Authenticator myAuth = new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				
				try {
					
					// get username and password from the settings
					TableDao dao = new TableDao(TableSchema.load(CustomPaths.SETTINGS_SHEET));
					TableRow sett = dao.getAll().iterator().next();
					
					String username = sett.get(CustomPaths.SETTINGS_USERNAME).getCode();
					String password = sett.get(CustomPaths.SETTINGS_PASSWORD).getCode();
					
					// return the authentication
					return new PasswordAuthentication(username, password.toCharArray());
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				return null;
			}
		};

		// set the default authenticator
		Authenticator.setDefault(myAuth);

		// create the envelope and name it
		SOAPEnvelope envelope = soapPart.getEnvelope();
		envelope.addNamespaceDeclaration(prefix, namespace);

		// return the message
		return soapMsg;
	}
	
	/**
	 * Get an xml document starting from a string text formatted as xml
	 * @param xml
	 * @return
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws Exception
	 */
	public static Document getDocument(String xml) 
			throws ParserConfigurationException, SAXException, IOException {

		// create the factory object to create the document object
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    
	    // get the builder from the factory
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    
	    // Set the input source (the text string)
	    InputSource is = new InputSource(new StringReader(xml));
	    
	    // get the xml document and return it
	    return builder.parse(is);
	}
	
	/**
	 * Load an xml document using a file
	 * @param file
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document getDocument(File file) 
			throws ParserConfigurationException, SAXException, IOException {
		
		// create the factory object to create the document object
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    
	    // get the builder from the factory
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    
	    // get the xml document and return it
	    return builder.parse(file);
	}
	
	/**
	 * Get a document from an input stream
	 * @param input
	 * @return
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document getDocument(InputStream input) 
			throws ParserConfigurationException, SAXException, IOException {
		
		// create the factory object to create the document object
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    
	    // get the builder from the factory
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    
	    // get the xml document and return it
	    return builder.parse(input);
	}
	
	/**
	 * Get the first attachment of the message
	 * @param message
	 * @return
	 * @throws SOAPException
	 */
	public AttachmentPart getFirstAttachmentPart(SOAPMessage message) throws SOAPException {

		// get the attachment
		Iterator<?> iter = message.getAttachments();
		
		if (!iter.hasNext()) {
			System.err.println("No attachment found for ");
			try {
				message.writeTo(System.err);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		// get the response attachment
		AttachmentPart attachment = (AttachmentPart) iter.next();
		
		return attachment;
	}
	
	/**
	 * Get the xsd from the attachment
	 * @param response
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassCastException
	 * @throws SOAPException
	 * @throws IOException
	 * @throws ParserConfigurationException 
	 * @throws SAXException 
	 */
	public Document getXsdAttachment(SOAPMessage response) 
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, 
			ClassCastException, SOAPException, IOException, ParserConfigurationException, SAXException {
		
		//String filename = AppPaths.TEMP_FOLDER + "temp_" + System.currentTimeMillis() + ".xsd";
		
		// write the .xsd file
		//File file = new File(filename);
		//writeAttachment(response, file);
		
		AttachmentPart part = getFirstAttachmentPart(response);
		InputStream stream = part.getRawContent();
		
        // parse the document
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(stream); 
		
		/*// load the schema from the input stream
		XSImplementationImpl impl = new XSImplementationImpl();
		XSLoader schemaLoader = impl.createXSLoader(null);
		
		LSInput input = new DOMInputImpl();
		input.setByteStream(stream);
		
		XSModel model = schemaLoader.load(input);
		
		// delete the temporary file
		//file.delete();
		*/
		stream.close();
		
		return doc;
	}
	
	/**
	 * Write the first attachment of the response
	 * @param response
	 * @param file
	 * @throws SOAPException
	 * @throws IOException
	 */
	public void writeAttachment(SOAPMessage response, File file) 
			throws SOAPException, IOException {
		
		// get the attachment
		AttachmentPart attachment = getFirstAttachmentPart(response);
		
		// read attachment and write it
		InputStream inputStream = attachment.getRawContent();
		OutputStream outputStream = new FileOutputStream(file);

		byte[] buf = new byte[512];
		int num;
		
		// write file
		while ( (num = inputStream.read(buf) ) != -1) {
			outputStream.write(buf, 0, num);
		}
		
		outputStream.close();
		inputStream.close();
	}
	
	/**
	 * get the first xml attachment in a dom document
	 * @param message
	 * @return
	 * @throws SOAPException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Document getFirstXmlAttachment(SOAPMessage message) 
			throws SOAPException, ParserConfigurationException, SAXException, IOException {
		
		AttachmentPart part = getFirstAttachmentPart(message);
		
		if (part == null)
			return null;
		
		// get the stream
		InputStream stream = part.getRawContent();
		
		// parse the stream and get the document
		Document xml = getDocument(stream);
		
		// close the stream
		stream.close();
		
		return xml;
	}
	
	/**
	 * Create the request message which will be sent to the web service
	 * @param con
	 * @return
	 */
	public abstract SOAPMessage createRequest(SOAPConnection con) throws SOAPException;

	/**
	 * Process the web service response and return something if needed
	 * @param soapResponse the response returned after sending the soap request
	 * @return a processed object. It can be whatever you want, be aware that you
	 * need to cast it to specify its type.
	 */
	public abstract Object processResponse(SOAPMessage soapResponse) throws SOAPException;
}

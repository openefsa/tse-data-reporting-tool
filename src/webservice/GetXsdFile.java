package webservice;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.xerces.xs.XSModel;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class GetXsdFile extends SOAPAction {

	private static final String NAMESPACE = "http://dcf-elect.efsa.europa.eu/";
	private static final String URL = "https://dcf-elect.efsa.europa.eu/elect2";
	
	private String resourceId;
	
	/**
	 * Make a get file request for a specific resource
	 * @param resourceId
	 */
	public GetXsdFile(String resourceId) {
		super(NAMESPACE);
		this.resourceId = resourceId;
	}
	
	/**
	 * Get the xsd file
	 * @return
	 * @throws SOAPException
	 */
	public Document getFile() throws SOAPException {
		Object response = makeRequest(URL);
		if (response == null)
			return null;
		
		return (Document) response;
	}

	@Override
	public SOAPMessage createRequest(SOAPConnection con) throws SOAPException {

		// create the standard structure and get the message
		SOAPMessage soapMsg = createTemplateSOAPMessage ( "dcf" );
		SOAPBody soapBody = soapMsg.getSOAPPart().getEnvelope().getBody();
		SOAPElement soapElem = soapBody.addChildElement( "GetFile", "dcf" );

		// add resource id
		SOAPElement arg = soapElem.addChildElement( "trxResourceId" );
		arg.setTextContent( resourceId );

		// save the changes in the message and return it
		soapMsg.saveChanges();

		return soapMsg;
	}

	@Override
	public Object processResponse(SOAPMessage soapResponse) throws SOAPException {
		
		// get the xsd file
		try {
			Document model = getXsdAttachment(soapResponse);
			return model;
		} catch (ClassCastException | ClassNotFoundException | InstantiationException 
				| IllegalAccessException | IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
		return null;
	}
}

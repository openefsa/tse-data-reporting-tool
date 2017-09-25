package webservice;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Get an xsd file from the dcf
 * @author avonva
 *
 */
public class GetXsdFile extends GetFile {

	public GetXsdFile(String resourceId) {
		super(resourceId);
	}

	/**
	 * Get the xsd file
	 * @return
	 * @throws SOAPException
	 */
	public Document getFile() throws SOAPException {
		Object response = makeRequest(getUrl());
		if (response == null)
			return null;
		
		return (Document) response;
	}

	@Override
	public Object processResponse(SOAPMessage soapResponse) throws SOAPException {
		
		// get the xsd file
		try {
			Document xsd = getXsdAttachment(soapResponse);
			return xsd;
		} catch (ClassCastException | ClassNotFoundException | InstantiationException 
				| IllegalAccessException | IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
		return null;
	}
}

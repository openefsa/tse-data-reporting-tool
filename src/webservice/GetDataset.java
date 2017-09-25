package webservice;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

/**
 * Generic get file request to the dcf.
 * @author avonva
 *
 */
public class GetDataset extends SOAPAction {

	private static final String NAMESPACE = "http://dcf-elect.efsa.europa.eu/";
	private static final String URL = "https://dcf-elect.efsa.europa.eu/elect2";
	
	private String datasetId;
	
	/**
	 * Make a get file request for a specific dataset
	 * @param datasetId
	 */
	public GetDataset(String datasetId) {
		super(NAMESPACE);
		this.datasetId = datasetId;
	}
	
	/**
	 * Get the url for making get file requests
	 * @return
	 */
	public static String getUrl() {
		return URL;
	}
	
	@Override
	public SOAPMessage createRequest(SOAPConnection con) throws SOAPException {

		// create the standard structure and get the message
		SOAPMessage soapMsg = createTemplateSOAPMessage ( "dcf" );
		SOAPBody soapBody = soapMsg.getSOAPPart().getEnvelope().getBody();
		SOAPElement soapElem = soapBody.addChildElement( "getDataset", "dcf" );

		// add resource id
		SOAPElement arg = soapElem.addChildElement( "datasetId" );
		arg.setTextContent( datasetId );

		// save the changes in the message and return it
		soapMsg.saveChanges();

		return soapMsg;
	}

	@Override
	public Object processResponse(SOAPMessage soapResponse) throws SOAPException {
		
		// process the dataset and return it
		return null;
	}
}

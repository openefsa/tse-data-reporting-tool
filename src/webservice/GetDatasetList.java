package webservice;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import app_config.PropertiesReader;
import dataset.DatasetList;
import tse_config.CustomPaths;

/**
 * Get dataset list request for the DCF webservice. It can be used
 * by calling {@link #getlist()} to get all the dataset
 * of the current user. Note that the username and the password
 * of the user are picked from the {@link CustomPaths#SETTINGS_SHEET} table.
 * @author avonva
 *
 */
public class GetDatasetList extends SOAPAction {

	// web service link of the getDatasetList service
	private static final String URL = "https://dcf-elect.efsa.europa.eu/elect2/";
	private static final String LIST_NAMESPACE = "http://dcf-elect.efsa.europa.eu/";
	
	private String dataCollectionCode;  // PropertiesReader.getDataCollectionCode();
	
	/**
	 * Initialize the get dataset list request with the data collection
	 * that is required.
	 * @param dataCollectionCode
	 */
	public GetDatasetList(String dataCollectionCode) {
		super(LIST_NAMESPACE);
		this.dataCollectionCode = dataCollectionCode;
	}
	
	/**
	 * Send the request and get the dataset list
	 * @throws SOAPException
	 */
	public DatasetList getlist() throws SOAPException {
		
		DatasetList datasets = new DatasetList();
		
		Object response = makeRequest(URL);  // get the datasets
		
		// get the list from the response if possible
		if (response != null) {
			datasets = (DatasetList) response;
		}
		
		return datasets;
	}

	@Override
	public SOAPMessage createRequest(SOAPConnection con) throws SOAPException {
		
		// create the standard structure and get the message
		SOAPMessage request = createTemplateSOAPMessage("dcf");

		SOAPBody soapBody = request.getSOAPPart().getEnvelope().getBody();
		
		SOAPElement soapElem = soapBody.addChildElement("getDatasetList", "dcf");

		SOAPElement arg = soapElem.addChildElement("dataCollectionCode");
		arg.setTextContent(this.dataCollectionCode);

		// save the changes in the message and return it
		request.saveChanges();
		
		return request;
	}
	
	@Override
	public Object processResponse(SOAPMessage soapResponse) throws SOAPException {

		// parse the dom document and return the contents
		DatasetListParser parser = new DatasetListParser();
		SOAPBody body = soapResponse.getSOAPPart().getEnvelope().getBody();
		return parser.parseDatasets(body);
	}

	public static void main(String[] args) throws SOAPException {
		GetDatasetList list = new GetDatasetList(PropertiesReader.getDataCollectionCode());
		System.out.println("LIST " + list.getlist());
		
	}
}

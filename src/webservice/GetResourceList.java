package webservice;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import resource.ResourceList;
import resource.ResourceReference;

/**
 * Request to the DCF for getting the resource list
 * @author avonva
 *
 */
public class GetResourceList extends SOAPAction {

	private String dataCollectionCode;
	
	// web service link of the getDatasetList service
	private static final String URL = "https://dcf-elect.efsa.europa.eu/elect2/";
	private static final String NAMESPACE = "http://dcf-elect.efsa.europa.eu/";
	
	public GetResourceList(String dataCollectionCode) {
		super(NAMESPACE);
		this.dataCollectionCode = dataCollectionCode;
	}
	
	/**
	 * get the resource list
	 * @return
	 * @throws SOAPException
	 */
	public ResourceList getList() throws SOAPException {
		
		Object response = makeRequest(URL);
		
		if (response == null)
			return null;
		
		return (ResourceList) response;
	}

	@Override
	public SOAPMessage createRequest(SOAPConnection con) throws SOAPException {
		
		// create the standard structure and get the message
		SOAPMessage request = createTemplateSOAPMessage("dcf");

		SOAPBody soapBody = request.getSOAPPart().getEnvelope().getBody();
		
		SOAPElement soapElem = soapBody.addChildElement("GetResourceList", "dcf");

		SOAPElement arg = soapElem.addChildElement("dataCollection");
		arg.setTextContent(this.dataCollectionCode);

		// save the changes in the message and return it
		request.saveChanges();

		return request;
	}

	@Override
	public Object processResponse(SOAPMessage soapResponse) throws SOAPException {
		
		// get the children of the body
		NodeList returnNodes = soapResponse.getSOAPPart().
				getEnvelope().getBody().getElementsByTagName("return");

		if (returnNodes.getLength() == 0) {
			System.err.println("GetResourceList: general error for data collection " + dataCollectionCode);
			return null;
		}
		
		// get return node
		Node returnNode = returnNodes.item(0);
		
		// return the parsed cdata field
		Document cdata = getCData(returnNode);
		
		if (cdata == null) {
			System.err.println("GetResourceList: No cdata found for data collection " + dataCollectionCode);
			return null;
		}
		
		return getResources(cdata);
	}
	
	/**
	 * get the resources from the document
	 * @param document
	 * @return
	 */
	public ResourceList getResources(Document document) {
		
		ResourceList references = new ResourceList();
		
		NodeList refs = document.getElementsByTagName("resourceReference");
		
		for (int i = 0; i < refs.getLength(); ++i) {
			Node ref = refs.item(i);
			references.add(getResource(ref));
		}
		
		return references;
	}
	
	/**
	 * Get the resource from the node
	 * @param resNode
	 * @return
	 */
	public ResourceReference getResource(Node resNode) {
		
		NodeList fields = resNode.getChildNodes();
		
		ResourceReference reference = new ResourceReference();
		
		for (int i = 0; i < fields.getLength(); ++i) {
			
			Node field = fields.item(i);
			
			String nodeName = field.getNodeName();
			String nodeValue = field.getTextContent();
			
			switch (nodeName) {
			case "resourceType":
				reference.setType(nodeValue);
				break;
			case "resourceId":
				reference.setResourceId(nodeValue);
				break;
			default:
				break;
			}
		}
		
		if (reference.isIncomplete()) {
			System.err.println("Missing reference value for " + reference);
		}
		
		return reference;
	}

	
	public static Document getCData(Node node) {
		
		// get the CDATA field of the 'return' node (data related to the XML) and parse it
		Document cdata;
		try {
			cdata = getDocument(node.getFirstChild().getNodeValue());
		} catch (DOMException | ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			return null;
		}

		return cdata;
	}
}

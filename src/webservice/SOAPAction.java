package webservice;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import app_config.AppPaths;
import database.TableDao;
import table_skeleton.TableRow;
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
					TableDao dao = new TableDao(TableSchema.load(AppPaths.SETTINGS_SHEET));
					TableRow sett = dao.getAll().iterator().next();
					
					String username = sett.get(AppPaths.SETTINGS_USERNAME).getCode();
					String password = sett.get(AppPaths.SETTINGS_PASSWORD).getCode();
					
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

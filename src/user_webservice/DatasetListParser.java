package user_webservice;

import javax.xml.soap.SOAPBody;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import user_objects.Dataset;
import user_objects.DatasetList;
import user_objects.DatasetStatus;

/**
 * Parser for a dom document containing the {@link GetDatasetList}
 * response
 * @author avonva
 *
 */
public class DatasetListParser {

	/**
	 * Get a list of datasets from the soap body
	 * @param body
	 * @return
	 */
	public DatasetList parseDatasets(SOAPBody body) {
		
		DatasetList datasets = new DatasetList();
		
		NodeList datas = body.getElementsByTagName("dataset");

		for (int i = 0; i < datas.getLength(); ++i) {

			// get the current dataset
			Node datasetNode = datas.item(i);

			datasets.add(getDataset(datasetNode));
		}
		
		return datasets;
	}
	
	/**
	 * Get a single dataset from a dataset node
	 * @param datasetNode
	 * @return
	 */
	private Dataset getDataset(Node datasetNode) {
		
		// get the info related to the dataset
		NodeList datasetInfoNode = datasetNode.getChildNodes();
		
		Dataset dataset = new Dataset();
		
		// parse the dataset info
		for (int i = 0; i < datasetInfoNode.getLength(); ++i) {
			
			Node field = datasetInfoNode.item(i);

			String value = field.getTextContent();
			
			switch (field.getNodeName()) {
			case "datasetId":
				dataset.setId(value);
				break;
			case "datasetSenderId":
				dataset.setSenderId(value);
				break;
			case "datasetStatus":
				dataset.setStatus(getStatus(field));
				break;
			default:
				break;
			}
		}

		return dataset;
	}
	
	/**
	 * Get the dataset status object from the status node
	 * @param statusNode
	 * @return
	 */
	private DatasetStatus getStatus(Node statusNode) {
		
		DatasetStatus status = null;
		String step = null;
		
		// get the status info
		NodeList statusInfoNode = statusNode.getChildNodes();
		for (int i = 0; i < statusInfoNode.getLength(); ++i) {
			
			// set properties based on node name
			Node field = statusInfoNode.item(i);

			String value = field.getTextContent();
			
			switch (field.getLocalName()) {
			case "status":
				status = DatasetStatus.fromString(value);
				break;
			case "step":
				step = value;
				break;
			default:
				break;
			}
		}
		
		// set also the step after the status is defined
		if (status != null)
			status.setStep(step);
		
		return status;
	}
}

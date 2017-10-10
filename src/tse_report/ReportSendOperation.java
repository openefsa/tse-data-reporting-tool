package tse_report;

import dataset.Dataset;
import dataset.DatasetStatus;
import message_creator.OperationType;

public class ReportSendOperation {
	
	private Dataset dataset;
	private OperationType opType;

	public ReportSendOperation(Dataset dataset, OperationType opType) {
		this.dataset = dataset;
		this.opType = opType;
	}
	
	public Dataset getDataset() {
		return dataset;
	}
	public OperationType getOpType() {
		return opType;
	}
	public DatasetStatus getStatus() {
		
		if (dataset != null)
			return dataset.getStatus();
		
		return null;
	}
}

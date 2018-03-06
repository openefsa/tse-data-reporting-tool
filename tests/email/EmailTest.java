package email;

import org.junit.Test;

import app_config.PropertiesReader;
import dataset.RCLDatasetStatus;
import tse_report.TseReport;
import user.User;

public class EmailTest {

	@Test
	public void openFatalMailPanel() {
		
		User.getInstance().login("myUsername", "myPass");
		User.getInstance().addData("organization", "DGAV");
		
		TseReport report = new TseReport();
		report.setId("datasetId");
		report.setMessageId("messageId");
		report.setLastMessageId("last message id");
		report.setLastModifyingMessageId("last mod message id");
		report.setLastValidationMessageId("last valid message id");
		report.setCountry("AT");
		report.setSenderId("AT0304.05");
		report.setMonth("4");
		report.setYear("2003");
		report.setStatus(RCLDatasetStatus.VALID);
		report.setVersion("05");
		
		PropertiesReader.openMailPanel(report);
	}
}

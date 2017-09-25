package message_config;

import java.io.IOException;
import java.util.HashMap;

import org.apache.poi.ss.usermodel.Row;

import app_config.AppPaths;
import xlsx_reader.XlsxReader;

/**
 * Read the {@link AppPaths#MESSAGE_CONFIG_SHEET} sheet
 * and save the content in the {@link MessageConfigList} object
 * @author avonva
 *
 */
public class MessageConfigReader extends XlsxReader {

	private MessageConfigList output;
	private HashMap<MessageConfigHeader, String> values;
	
	public MessageConfigReader(String filename) throws IOException {
		super(filename);
		this.values = new HashMap<>();
		this.output = new MessageConfigList();
	}

	public MessageConfigList read() throws IOException {
		super.read(AppPaths.MESSAGE_CONFIG_SHEET);
		return output;
	}
	
	@Override
	public void processCell(String header, String value) {
		
		MessageConfigHeader h = null;
		try {
			h = MessageConfigHeader.fromString(header);
		}
		catch(IllegalArgumentException e) {
			return;
		}

		if(h == null || value == null)
			return;
		
		// save the value with the correct header
		values.put(h, value);
	}

	@Override
	public void startRow(Row row) {
		
		// clear the hashmap values for new rows
		this.values.clear();
	}

	@Override
	public void endRow(Row row) {
		
		// create a new message configuration and save it
		// in the output array
		MessageConfig config = new MessageConfig(this.values);
		this.output.add(config);
	}
}

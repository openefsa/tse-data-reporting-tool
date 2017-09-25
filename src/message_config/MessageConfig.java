package message_config;

import java.util.HashMap;

import app_config.BooleanValue;

public class MessageConfig {

	private HashMap<MessageConfigHeader, String> values;
	
	public MessageConfig() {
		this.values = new HashMap<>();
	}
	
	public MessageConfig(HashMap<MessageConfigHeader, String> values) {
		this();
		for (MessageConfigHeader header : values.keySet()) {
			this.values.put(header, values.get(header));
		}
	}
	
	public void put(MessageConfigHeader header, String value) {
		this.values.put(header, value);
	}
	
	public String get(MessageConfigHeader header) {
		return this.values.get(header);
	}
	
	/**
	 * Check if a field is set to true given its header
	 * @param header
	 * @return
	 */
	public boolean isFieldTrue(MessageConfigHeader header) {
		
		String value = this.values.get(header);
		
		if (value == null)
			return false;
		
		return BooleanValue.isTrue(value);
	}
	
	@Override
	public String toString() {
		return values.toString();
	}
}

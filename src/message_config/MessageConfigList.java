package message_config;

import java.io.IOException;
import java.util.ArrayList;

import app_config.AppPaths;

public class MessageConfigList extends ArrayList<MessageConfig> {
	
	private static MessageConfigList cache;
	private static final long serialVersionUID = 1L;
	
	public static MessageConfigList getAll() throws IOException {
		
		if (cache == null) {
			
			cache = new MessageConfigList();
			
			MessageConfigReader reader = new MessageConfigReader(AppPaths.TABLES_SCHEMA_FILE);
			cache = reader.read();
			reader.close();
		}
		
		return cache;
	}
	
	/**
	 * Get all the configs that are in th header
	 * @return
	 * @throws IOException
	 */
	public static MessageConfigList getHeader() throws IOException {
		return getGroup(MessageConfigHeader.PUT_IN_HEADER);
	}
	
	private static MessageConfigList getGroup(MessageConfigHeader header) throws IOException {
		
		MessageConfigList list = new MessageConfigList();
		
		for (MessageConfig config : getAll()) {
			if (config.isFieldTrue(header))
				list.add(config);
		}
		
		return list;
	}
}

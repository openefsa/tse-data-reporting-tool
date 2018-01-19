package session_manager;

import java.io.File;

import tse_config.CustomStrings;
import window_restorer.RestoreableWindowDao;

public class TSERestoreableWindowDao extends RestoreableWindowDao {

	public static final String WINDOWS_SIZES_FILENAME = CustomStrings.PREFERENCE_FOLDER 
			+ "windows-sizes.json";
	
	@Override
	public File getConfigFile() {
		return new File(WINDOWS_SIZES_FILENAME);
	}

}

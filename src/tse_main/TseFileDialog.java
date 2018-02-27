package tse_main;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import i18n_messages.TSEMessages;

public class TseFileDialog {

	private Shell shell;
	
	public TseFileDialog(Shell shell) {
		this.shell = shell;
	}
	
	/**
	 * Select a file
	 * @param style file dialog style
	 * @param fileFormat which formats should be accepted
	 * @return
	 */
	public File select(int style, String fileFormat, String filename) {
        FileDialog fd = new FileDialog(shell, style);
        fd.setOverwrite(true);
        fd.setText(TSEMessages.get("export.report.button"));
        String[] filterExt = { fileFormat };
        fd.setFilterExtensions(filterExt);
        fd.setFileName(filename);
        
        String file = fd.open();
        if (file == null)
        	return null;
        
        return new File(file);
	}
	
	public File saveXml(String filename) {
		return select(SWT.SAVE, "*.xml", filename);
	}
	
	public File loadXml() {
		return select(SWT.NONE, "*.xml", "");
	}
}

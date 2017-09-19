package html_viewer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JFrame;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class HtmlViewer {

	private Shell parent;
	public HtmlViewer(Shell parent) {
		this.parent = parent;
	}
	
	public void open(String html) {
		
		Shell dialog = new Shell(parent);
		
		Browser browser = new Browser(dialog, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		browser.setBounds(0, 0, 600, 400);
		
		browser.setText(html);
		dialog.pack();
		dialog.open();
		
		while (!dialog.isDisposed())
		      if (!dialog.getDisplay().readAndDispatch())
		    	  dialog.getDisplay().sleep();
	}
	
	public static void main(String[] args) throws MalformedURLException {
		
		Display display = new Display();
		Shell shell = new Shell(display);
		HtmlViewer viewer = new HtmlViewer(shell);

		String html = "<html><head><title>Simple Page</title></head><body bgcolor='#777779'><hr/><font size=50>This is Html content</font><hr/></body></html>";
		viewer.open(html);
	}
}

package table_dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * Additional information provided to the user for the form
 * @author avonva
 *
 */
public class HelpViewer {

	private Composite parent;
	private String title;
	private Label info;
	private Label helpBtn;
	
	public HelpViewer(Composite parent, String title) {
		this.parent = parent;
		this.title = title;
		create();
	}
	
	/**
	 * Add a listener to the help button
	 * @param listener
	 */
	public void setListener(MouseListener listener) {
		this.helpBtn.addMouseListener(listener);
	}
	
	/**
	 * Tooltip text for the image
	 * @param text
	 */
	public void setToolTipText(String text) {
		this.helpBtn.setToolTipText(text);
	}
	
	private void create() {
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2,false));
		
		this.info = new Label(composite, SWT.NONE);
		this.info.setText(title);

		// set the label font to italic and bold
		FontData fontData = Display.getCurrent().getSystemFont().getFontData()[0];

		Font font = new Font(Display.getCurrent(), 
				new FontData(fontData.getName(), fontData.getHeight() + 5, SWT.BOLD));

		this.info.setFont (font);
		

		helpBtn = new Label(composite, SWT.PUSH);
		
		Image image = new Image(Display.getCurrent(), 
				this.getClass().getClassLoader().getResourceAsStream("help.png"));
		
		this.helpBtn.setImage(image);
	}
}

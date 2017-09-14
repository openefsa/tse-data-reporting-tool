package report_interface;

import java.io.IOException;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import app_config.AppPaths;
import database.Relation;
import database.TableDao;
import report.TableColumnValue;
import report.SummarizedInformationSchema;
import report.TableRow;
import report_interface.MonitoringSelector.MonitoringListener;
import xml_reader.Selection;

/**
 * Class which allows adding and editing a report.
 * @author avonva
 *
 */
public class ReportViewer {
	
	private Composite parent;
	private TableRow report;  // the current report we want to edit
	private MonitoringSelector monitorSelector;
	private ReportViewerHelp helpViewer;
	private ReportTable reportTable;
	
	/**
	 * Create the selector and report table
	 * @param parent
	 */
	public ReportViewer(Composite parent) {
		this.parent = parent;
		create();
	}
	
	public void setEnabled(boolean enabled) {
		this.monitorSelector.setEnabled(enabled);
	}
	
	public void setReport(TableRow report) {
		this.report = report;
		this.monitorSelector.setEnabled(true);
	}
	
	/**
	 * Create the panel
	 */
	private void create() {
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1,false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		this.helpViewer = new ReportViewerHelp(composite, "TSEs monitoring data (aggregated level)");
		this.helpViewer.setListener(new MouseListener() {
			
			@Override
			public void mouseUp(MouseEvent arg0) {
				System.out.println("Open help");
			}
			
			@Override
			public void mouseDown(MouseEvent arg0) {}
			
			@Override
			public void mouseDoubleClick(MouseEvent arg0) {}
		});
		
		this.monitorSelector = new MonitoringSelector(composite);
		this.monitorSelector.setEnabled(false);

		this.reportTable = new ReportTable(composite, AppPaths.SUMMARIZED_INFO_SHEET, true);

		addTableMenu();

		this.monitorSelector.addSelectionListener(new MonitoringListener() {

			@Override
			public void selectionConfirmed(Selection selectedItem) {

				// create a new row and
				// put the first cell in the row
				try {
					TableRow row = createNewRow(selectedItem);
					reportTable.add(row);
					
					TableDao dao = new TableDao(reportTable.getSchema());
					dao.add(row);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void selectionChanged(Selection selectedItem) {}
		});

	}
	
	/**
	 * Create a new row with default values
	 * @param element
	 * @return
	 * @throws IOException 
	 */
	private TableRow createNewRow(Selection element) throws IOException {

		TableRow row = new TableRow(reportTable.getSchema());
		row.put(SummarizedInformationSchema.TYPE, new TableColumnValue(element));
		// put also the information related to the foreign key of the report
		for (Relation r : reportTable.getSchema().getRelations()) {
			row.put(r.getForeignKey(), report.get(r.getForeignKey()));
		}

		return row;
	}
	
	/**
	 * Add a menu to the table
	 */
	private void addTableMenu() {
		
		Menu menu = new Menu(parent);
		
		// remove an item
		MenuItem remove = new MenuItem(menu, SWT.PUSH);
		remove.setText("Delete element");
		remove.setEnabled(false);
		reportTable.setMenu(menu);
		
		reportTable.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {
				remove.setEnabled(!reportTable.isEmpty());
			}
		});
		
		remove.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				reportTable.removeSelectedRow();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
	}
}

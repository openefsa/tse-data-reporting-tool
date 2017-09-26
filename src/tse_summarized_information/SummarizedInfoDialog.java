package tse_summarized_information;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.soap.SOAPException;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import acknowledge.Ack;
import message_creator.DatasetXmlCreator;
import table_dialog.RowValidatorLabelProvider;
import table_dialog.TableViewWithHelp.RowCreationMode;
import table_relations.Relation;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import tse_case_report.CaseReportDialog;
import tse_components.TableDialogWithMenu;
import tse_config.CatalogLists;
import tse_config.CustomStrings;
import tse_validator.SummarizedInfoValidator;
import webservice.GetAck;
import xlsx_reader.TableSchema;
import xml_catalog_reader.Selection;

/**
 * Class which allows adding and editing a summarized information report.
 * @author avonva
 *
 */
public class SummarizedInfoDialog extends TableDialogWithMenu {
	
	public SummarizedInfoDialog(Shell parent) {
		
		super(parent, "", "TSEs monitoring data (aggregated level)", 
				true, RowCreationMode.SELECTOR, false, false);
		
		// add 300 px in height
		addDialogHeight(300);
		
		// specify title and list of the selector
		setRowCreatorLabel("Add data related to monitoring of:");
		setSelectorList(CatalogLists.TSE_LIST);
		
		// add the parents of preferences and settings
		try {
			addParentTable(Relation.getGlobalParent(CustomStrings.PREFERENCES_SHEET));
			addParentTable(Relation.getGlobalParent(CustomStrings.SETTINGS_SHEET));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		addExportButton(parent);
		
		addRefreshButton(parent);
		
		// if double clicked an element of the table
		// open the cases
		addTableDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {

				final IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				if (selection == null || selection.isEmpty())
					return;

				final TableRow summInfo = (TableRow) selection.getFirstElement();

				// create a case passing also the report information
				CaseReportDialog dialog = new CaseReportDialog(parent, getParentFilter());
				
				// filter the records by the clicked summarized information
				dialog.setParentFilter(summInfo);
				
				// add as parent also the report of the summarized information
				// which is the parent filter since we have chosen a summarized
				// information from a single report (the summ info were filtered
				// by the report)
				dialog.addParentTable(getParentFilter());
				
				dialog.open();
				
				// refresh the table when cases are changed
				refresh();
			}
		});
	}
	
	private void addExportButton(Composite parent) {
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1,false));
		composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		
		Button exportReport = new Button(composite, SWT.PUSH);
		exportReport.setText("Export report");
		exportReport.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				File file = new File("report.xml");
				
				try {
					
					// instantiate xml creator and inject the required parents
					// of the configuration table (in order to be able to
					// retrieve the data for the message header)
					DatasetXmlCreator creator = new DatasetXmlCreator(file) {
						
						@Override
						public Collection<TableRow> getConfigMessageParents() {
							
							Collection<TableRow> parents = new ArrayList<>();
							
							// add the report data
							parents.add(getParentFilter());
							
							// add the settings data
							try {
								parents.add(Relation.getGlobalParent(CustomStrings.SETTINGS_SHEET));
							} catch (IOException e) {
								e.printStackTrace();
							}
							
							return parents;
						}
					};
					
					// export the report
					try {
						creator.export(getParentFilter());
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
	}
	
	private void addRefreshButton(Composite parent) {
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1,false));
		composite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		
		Button exportReport = new Button(composite, SWT.PUSH);
		exportReport.setText("Refresh state");
		exportReport.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				// if no report opened, stop
				if (getParentFilter() == null)
					return;
				
				Cursor wait = parent.getDisplay().getSystemCursor(SWT.CURSOR_WAIT);
				parent.setCursor(wait);

				try {
					
					Ack ack = getReportAck();
					
					if (ack == null)
						warnUser("Error", "The current report cannot be refreshed, since it was never sent to the dcf.");
					else
						warnUser("Success", "Current report state: " + ack);
					
				} catch (SOAPException e) {
					e.printStackTrace();
					warnUser("Error", "Check your credentials or your internet connection");
				}

				
				Cursor arrow = parent.getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
				parent.setCursor(arrow);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
	}
	
	/**
	 * Get the state of the current report
	 * @return
	 * @throws SOAPException
	 */
	private Ack getReportAck() throws SOAPException {
		
		TableRow report = getParentFilter();
		
		if (report == null) {
			return null;
		}
		
		// get the message id
		TableColumnValue messageIdVal = report.get(CustomStrings.REPORT_MESSAGE_ID);
		
		// if no message id => the report was never sent
		if (messageIdVal.isEmpty()) {
			return null;
		}
		
		// make get ack request
		String messageId = messageIdVal.getCode();
		GetAck req = new GetAck(messageId);
		
		// get state
		Ack ack = req.getAck();
		
		return ack;
	}
	
	@Override
	public void setParentFilter(TableRow parentFilter) {
		// enable/disable the selector when a report is opened/closed
		setRowCreationEnabled(parentFilter != null);
		super.setParentFilter(parentFilter);
	}

	/**
	 * Create a new row with default values
	 * @param element
	 * @return
	 * @throws IOException 
	 */
	@Override
	public TableRow createNewRow(TableSchema schema, Selection element) {

		TableColumnValue value = new TableColumnValue(element);
		
		// create a new row with the type column already set
		TableRow row = new TableRow(schema, SummarizedInformationSchema.TYPE, value);

		return row;
	}

	@Override
	public String getSchemaSheetName() {
		return CustomStrings.SUMMARIZED_INFO_SHEET;
	}

	@Override
	public boolean apply(TableSchema schema, Collection<TableRow> rows, TableRow selectedRow) {
		return true;
	}

	@Override
	public Collection<TableRow> loadInitialRows(TableSchema schema, TableRow parentFilter) {
		return null;
	}

	@Override
	public void processNewRow(TableRow row) {}
	
	@Override
	public RowValidatorLabelProvider getValidator() {
		return new SummarizedInfoValidator();
	}
}

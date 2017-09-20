package export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.JAXBException;

import table_skeleton.TableColumn;
import table_skeleton.TableColumnValue;
import table_skeleton.TableRow;
import user_config.AppPaths;
import xlsx_reader.TableSchema;

/**
 * Class that creates a report in .xml format using the
 * information contained in a collection of {@link TableRow}.
 * @author avonva
 *
 */
public class XmlReportCreator {

	public static void main(String[] args) throws JAXBException, IOException {
		
		/*Dataset d = new Dataset();
		d.setId("12345");
		d.setSenderId("IT1708");
		d.setStatus(DatasetStatus.VALID);
		
		
		File file = new File("report.xml");
		JAXBContext jaxbContext = JAXBContext.newInstance(Dataset.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		// output pretty printed
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		jaxbMarshaller.marshal(d, file);
		jaxbMarshaller.marshal(d, System.out);*/
		
		Collection<TableRow> rows = new ArrayList<>();
		
		TableRow row = new TableRow(TableSchema.load(AppPaths.SUMMARIZED_INFO_SHEET));
		TableColumnValue value = new TableColumnValue();
		value.setCode("AAA");
		value.setLabel("AAA");
		row.put("source", value);
		
		rows.add(row);
		
		row = new TableRow(TableSchema.load(AppPaths.SUMMARIZED_INFO_SHEET));
		row.put("anMethCode", value);
		rows.add(row);
		
		XmlReportCreator c = new XmlReportCreator();
		c.export(rows);
	}
	
	/**
	 * Export an xml starting from a list of rows
	 * @param data
	 */
	public void export(Collection<TableRow> data) {
		
		for (TableRow row : data) {

			for (TableColumn column : row.getSchema()) {
				
				// skip non output columns
				if (!column.isPutInOutput(row))
					continue;
				
				// get the xml tag of the column
				String xmlTag = column.getXmlTag();
				String open = "<" + xmlTag + ">";
				String close = "</" + xmlTag + ">";
				
				// TODO write on string
				System.out.println(open + row.get(column.getId()).getCode() + close);
			}
		}
	}
}

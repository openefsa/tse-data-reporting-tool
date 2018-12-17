package report_converter;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created with Eclipse.
 * 
 * User: shahaal 
 * Date: 28-11-2018
 *
 * Class that parse Excel Spreadsheet to XML.
 *
 */

public class ExcelXmlConverter {
	
	/**
	 * the method convert an excel file into xml format
	 * and save it
	 * @param excelFile
	 * @return {@link Boolean}
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws TransformerException 
	 */
	public File convertXExcelToXml(File excelFile) throws ParserConfigurationException, IOException, TransformerException {
		
		//check if the file exists
		if (excelFile.exists() && excelFile.isFile()) {
			
			 FileInputStream inputStream = new FileInputStream(excelFile);
			Workbook workbook = new XSSFWorkbook(inputStream);
			
			//create the document instance which will have the xml elements
			DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();
			
			
			// create the root of for the xml message
			Element root = document.createElement("message");
			document.appendChild(root);
			
			// create a sub root for header
			//Element header = document.createElement("header");
			//root.appendChild(header);
			// create sub root for payload
			Element payload = document.createElement("payload");
			root.appendChild(payload);
			
			// create dataset node (which contains the sheet info)
			Element dataset = document.createElement("dataset");
			payload.appendChild(dataset);
			
			/*
			 * sheet 0 -> AggregatedData 
			 * sheet 1 -> SampleCases 
			 * sheet 2 -> AnalyticalResults
			 * sheet 3 -> LookUp table (?)
			 */

			// get the total number of sheets
			int sheetNo = workbook.getNumberOfSheets();
			
			// iterate for each sheet
			for (int i = 0; i < sheetNo; i++) {

				// get the sheet at index i
				Sheet sheet = workbook.getSheetAt(i);

				// create the root of the form based on the sheet's name
				//Element result = document.createElement(sheet.getSheetName());
				//dataset.appendChild(result);
				
				// take the header
				Row headerRow = sheet.getRow(0);
				
				int firstRow=sheet.getFirstRowNum()+1;
				int lastRow=sheet.getLastRowNum();
				
				// iterate each row (skip the header
				for (int j=firstRow;j<=lastRow;j++) {
					
					// create a sub root for each row
					Element rowElement = document.createElement("result");
					dataset.appendChild(rowElement);
					
					// set the attribute to the rowElement based on the row position
					//rowElement.setAttribute("id", String.valueOf(j));
					
					// iterate trough cell in the row
					for (Cell cell : sheet.getRow(j)) {

						Element el;
						int cellIndex = cell.getColumnIndex();
						
						switch (cell.getCellType()) {
						
						case STRING:
							// create header element
							el = document.createElement(headerRow.getCell(cellIndex).getStringCellValue());
							// append the cell value
							el.appendChild(document.createTextNode(cell.getStringCellValue()));
							// append to the parent
							rowElement.appendChild(el);
							break;
							
						case BOOLEAN:
							// create header elements
							el = document.createElement(headerRow.getCell(cellIndex).getStringCellValue());
							// append the cell value
							el.appendChild(document.createTextNode(String.valueOf(cell.getBooleanCellValue())));
							// append to the parent
							rowElement.appendChild(el);
							break;
							
						case NUMERIC:
							// create header elements
							el = document.createElement(headerRow.getCell(cellIndex).getStringCellValue());
							// append the cell value
							el.appendChild(document.createTextNode(String.valueOf(cell.getNumericCellValue())));
							// append to the parent
							rowElement.appendChild(el);
							break;
							
						default:
							break;
						}
					}
				}

			}

			// close the buffers
			workbook.close();
			inputStream.close();

			// write the xml to output target file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource domSource = new DOMSource(document);
			
			// for debugging
			//StreamResult streamResult = new StreamResult(System.out);
			
			String xmlPath=excelFile.getAbsolutePath().replace(".xlsx", ".xml");
			//System.out.println("shahaal new xml file in "+xmlPath);
			// write to file
			StreamResult streamResult = new StreamResult(new File(xmlPath));
			
			transformer.transform(domSource, streamResult);
			
			System.out.println("\nConvertion from xlsx to xml completed!");
			
			return new File(xmlPath);
		}
		
		return null;
	}
	/*
	public static void main(String[] args) {
		
		// input excel file
		File excelFile = new File("D:/PortableApps/test.xlsx");
		
		ExcelXmlConverter converter = new ExcelXmlConverter();
		
		try {
			
			if(converter.convertXExcelToXml(excelFile)!=null)
				System.out.println("Excel convertet with success.");
			else
				System.out.println("Error during Excel conversion. Try again!");
			
		} catch (ParserConfigurationException | IOException | TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}*/
}

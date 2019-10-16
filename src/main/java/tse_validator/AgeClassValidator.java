package tse_validator;

import java.io.IOException;

import date_comparator.TseDate;
import tse_config.CatalogLists;
import xml_catalog_reader.Selection;
import xml_catalog_reader.XmlContents;
import xml_catalog_reader.XmlLoader;

/**
 * Validate an age class
 * @author avonva
 * @author shahaal
 *
 */
public class AgeClassValidator {

	private final static String GTE = "gte";
	private final static String LT = "lt";
	private final static String MIN = "min";
	private final static String MAX = "max";
	
	private String ageClassCode;
	private String reportYear;
	private String reportMonth;
	private String birthYear;
	private String birthMonth;
	
	private TseDate reportDate;
	private TseDate birthDate;
	
	public AgeClassValidator(String ageClassCode, String reportYear, 
			String reportMonth, String birthYear, String birthMonth) {
		this.ageClassCode = ageClassCode;
		this.reportYear = reportYear;
		this.reportMonth = reportMonth;
		this.birthYear = birthYear;
		this.birthMonth = birthMonth;
	}
	
	public enum Check {
		REPORT_DATE_EXCEEDED,
		AGE_CLASS_NOT_RESPECTED,
		OK
	}
	
	public Check validate() throws IOException {
		
		XmlContents ages = XmlLoader.getByPicklistKey(CatalogLists.AGE_CLASS_LIST);
		
		if (ages == null) {
			throw new IOException("Cannot validate age without " + CatalogLists.AGE_CLASS_LIST + " picklist.");
		}
		
		Selection selection = ages.getElementByCode(ageClassCode);
		
		if (selection == null) {
			throw new IOException("The age " + ageClassCode 
					+ " was not found in the picklist " + CatalogLists.AGE_CLASS_LIST);
		}
		
		boolean validated = false;
		try {
			
			this.reportDate = new TseDate(reportYear, reportMonth);
			this.birthDate = new TseDate(birthYear, birthMonth);
			
			int months = reportDate.getMonthsDifference(birthDate);
			
			if (months < 0) {
				return Check.REPORT_DATE_EXCEEDED;
			}
			
			Integer gte = selection.getNumData(GTE);
			if(gte != null) {
				validated = months >= gte;
			}
			
			Integer less = selection.getNumData(LT);
			if(less != null) {
				validated = months < less;
			}
			
			Integer min = selection.getNumData(MIN);
			Integer max = selection.getNumData(MAX);
			
			if(min != null && max != null) {
				validated = months >= min && months <= max;
			}
			
		}
		catch(NumberFormatException e) {
			e.printStackTrace();
			throw new IOException("Cannot validate age with wrong year/month data."
					+ " Found Report=" + this.reportYear + " " + this.reportMonth
					+ " Found Birth=" + this.birthYear + " " + this.birthMonth);
		}
		
		if (validated)
			return Check.OK;
		
		return Check.AGE_CLASS_NOT_RESPECTED;
	}
	
	/**
	 * Get the number of months of difference between the report date
	 * and the birth date
	 * @return
	 * @throws NumberFormatException
	 */
	public int getMonthsDifference() {
		return this.reportDate.getMonthsDifference(this.birthDate);
	}
}

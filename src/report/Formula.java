package report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app_config.AppPaths;
import app_config.BooleanValue;
import app_config.SelectionsNames;
import database.Relation;
import xlsx_reader.ReportTableHeaders.XlsxHeader;
import xlsx_reader.SchemaReader;
import xml_reader.Selection;
import xml_reader.XmlLoader;

/**
 * Class which models a generic formula which can be inserted in the
 * .xlsx configuration files for creating ReportTable.
 * The formula can also be solved by invoking {@link #solve()}
 * @author avonva
 *
 */
public class Formula {
	
	// regex components
	private static final String NUMBER = "[0-9]{1,13}(\\.[0-9]*)?";
	private static final String LETTER = "[a-zA-Z]";
	private static final String STRING = "(" + LETTER + ")+";
	
	private String formula;
	private String solvedFormula;
	private String fieldHeader;
	private TableRow row;
	private TableColumn column;
	private int dependenciesCount;
	
	public Formula(TableRow row, TableColumn column, String fieldHeader) {
		this.row = row;
		this.column = column;
		this.fieldHeader = fieldHeader;
		this.formula = column.getFieldByHeader(fieldHeader);
		this.dependenciesCount = 0;
		evalDependencies();
	}
	
	/**
	 * Get the number of dependencies
	 * @return
	 */
	public int getDependenciesCount() {
		return dependenciesCount;
	}
	
	public TableRow getRow() {
		return row;
	}
	
	public TableColumn getColumn() {
		return column;
	}
	
	/**
	 * Get the solved formula. Can be used only after calling
	 * {@link #solve()}, otherwise it returns null.
	 * @return
	 */
	public String getSolvedFormula() {
		return solvedFormula;
	}
	
	public String getFormula() {
		return formula;
	}
	
	/**
	 * Solve the formula. Returns null if the field should be ignored
	 * @return
	 */
	public String solve() {
		
		if (formula == null)
			return "";
		
		String value = formula.replace(" ", "");

		// solve dates
		value = solveDateFormula(value);
		
		// solve special characters
		value = solveKeywords(value);

		// solve columns values
		value = solveColumnsFormula(value);

		//value = solveRelationFormula(value);
		
		// solve additions
		value = solveAdditions(value);
		
		// solve if not null
		value = solveConditions(value);
		
		// solve logical comparisons
		value = solveLogicalOperators(value);

		this.solvedFormula = value;

		return value;
	}
	
	/**
	 * Solve logical operations
	 * @param value
	 * @return
	 */
	private String solveConditions(String value) {
		
		String pattern = "IF_NOT_NULL(.*,.*,.*)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(value.replace(" ", ""));
		
		// if found
		if(m.find()) {

			// get match
			String elements = m.group().replace("IF_NOT_NULL(", "");
			
			String[] split = elements.split(",");
			
			if (split.length != 3) {
				System.err.println("Wrong formula " + value);
				return value;
			}

			String condition = split[0];
			String trueCond = split[1];
			String falseCond = split[2].replace(")", "");
			
			// if we have a not null value
			if(!condition.isEmpty())
				return trueCond;
			else
				return falseCond;
		}
		
		return value;
	}
	
	/**
	 * Solve additions. Syntax SUM(x,y,z,....)
	 * @param value
	 * @return
	 */
	private String solveAdditions(String value) {
		
		String result = value;
		
		String pattern = "SUM\\("  + NUMBER + "," + NUMBER + "(," + NUMBER + ")*\\)";
		
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(value.replace(" ", ""));
		
		// if there is a sum
		if(m.find()) {
			
			String elements = m.group().replace("SUM(", "").replace(")", "");
			StringTokenizer st = new StringTokenizer(elements, ",");
			
			// compute the sum
			double sum = 0;
			while(st.hasMoreTokens()) {
				String next = st.nextToken();
				sum = sum + Double.valueOf(next);
			}
			
			// cast to integer if it is an integer
			if ((sum == Math.floor(sum)) && !Double.isInfinite(sum)) {
				// convert result to string
				result = String.valueOf((int)sum);
			}
			else {
				result = String.valueOf(sum);
			}
		}
		
		return result;
	}
	
	/**
	 * Resolve all columns dependencies (\column_name)
	 * with the columns values
	 * @param value
	 * @return
	 * @throws IOException 
	 */
	private String solveColumnsFormula(String value) {
		
		String command = value;
		
		// replace \columns_names with their values if present
		// otherwise use the default values
		for (TableColumn col : row.getSchema()) {

			String code = "";
			String label = "";

			TableColumnValue colValue = this.row.get(col.getId());
			
			if (colValue != null)
				code = colValue.getCode();
			else
				code = col.getDefaultCode();

			// otherwise use value
			if (colValue != null)
				label = colValue.getLabel();
			else
				label = col.getDefaultValue();

			if (label == null)
				label = "";
			
			if (code == null)
				code = "";

			// replace values
			command = command.replace("\\" + col.getId() + ".code", code);
			command = command.replace("\\" + col.getId() + ".label", label);
		}

		return command;
	}
	
	public static void main(String[] args) throws IOException {
		SchemaReader r = new SchemaReader(AppPaths.CONFIG_FILE);
		r.read(AppPaths.SUMMARIZED_INFO_SHEET);
		TableRow row = new TableRow(r.getSchema());
		Formula f = new Formula(row, r.getSchema().getById("type"), XlsxHeader.DEFAULTVALUE.getHeaderName());
		f.solveColumnsFormula(f.formula);
	}
	
	/**
	 * Solve all the RELATION(parent, field) statements
	 * @param value
	 * @return
	 */
	private String solveRelationFormula(String value) {
		
		String command = value;
		
		Pattern p = Pattern.compile("RELATION\\(.+,.+\\)");
		Matcher m = p.matcher(command);
		
		// found a relation keyword
		if (m.find()) {
			
			// remove useless pieces
			String hit = m.group();
			
			String match = hit.replace("RELATION(", "").replace(")", "");
			
			// get operands by splitting with comma
			String[] split = match.split(",");
			
			if (split.length != 2) {
				System.err.println("Wrong RELATION statement, found " + match);
				return command;
			}
			
			String parentId = split[0];
			String field = split[1];
			
			// field is name.code or name.label
			split = field.split("\\.");
			
			if (split.length != 2) {
				System.err.println("Need .code or .label, found " + match);
				return command;
			}
			
			String fieldName = split[0];
			
			// get the relation with the parent
			Relation r = row.getSchema().getRelationByParentId(parentId);
			
			if (r == null)
				return command;
			
			// get from the child row the foreign key for the parent
			String foreignKey = row.get(r.getForeignKey()).getCode();
			
			// get the row using the foreign key
			TableRow row = r.getParentValue(Integer.valueOf(foreignKey));
			
			if (row == null)
				return command;
			
			// get the required field and put it into the formula
			TableColumnValue parentValue = row.get(field);
			
			// apply keywords
			match = match.replace(fieldName + ".code", parentValue.getCode());
			match = match.replace(fieldName + ".label", parentValue.getLabel());
			
			// replace in the final string
			command = command.replace(hit, match);
		}
		System.out.println("RELATION Command " + command);
		return command;
	}
	
	/**
	 * Solve the logical operators
	 * @param value
	 * @return
	 */
	private String solveLogicalOperators(String value) {
		
		String command = value;
		
		String operand = "(" + STRING + "|" + NUMBER + ")";
		String pattern = operand + "==" + operand;
		
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(value);
		
		if (m.find()) {
			// extract operands from the match
			String match = m.group();
			String[] split = match.split("==");
			String leftOp = split[0];
			String rightOp = split[1];
			
			// set the real value
			String result = leftOp.equalsIgnoreCase(rightOp) ? 
					BooleanValue.getTrueValue() : BooleanValue.getFalseValue();
					
			// replace match with the logical result
			command = value.replace(match, result);
		}

		return command;
	}
	
	/**
	 * Solve all the dates keywords of the formula
	 * @param value
	 * @return
	 */
	private String solveDateFormula(String value) {

		Date today = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(today);  //TODO set REPORT DATE!! occhio che quello dopo dipende da questo
		String year = String.valueOf(cal.get(Calendar.YEAR));
		String month = String.valueOf(cal.get(Calendar.MONTH) + 1); // months start from 0

		String shortYear = year.substring(Math.max(year.length() - 2, 0));
		String shortMonth = month.substring(Math.max(month.length() - 2, 0));

		// add zero for single number months
		if (shortMonth.length() == 1)
			shortMonth = "0" + shortMonth;

		// apply keywords for dates
		String command = value.replace("report.year", year);
		command = command.replace("report.shortyear", shortYear);
		command = command.replace("report.month", month);
		command = command.replace("report.shortmonth", shortMonth);
		
		
		// last month
		cal.add(Calendar.MONTH, -1);
		
		String lastYear = String.valueOf(cal.get(Calendar.YEAR)); // months start from 0
		
		Selection yearSel = XmlLoader.getByPicklistKey(SelectionsNames.YEARS_LIST)
				.getList().getSelectionByCode(lastYear);
		
		command = command.replace("lastMonth.year.code", yearSel.getCode());
		command = command.replace("lastMonth.year.label", yearSel.getDescription());
		
		String lastMonth = String.valueOf(cal.get(Calendar.MONTH) + 1); // months start from 0
		
		Selection monthSel = XmlLoader.getByPicklistKey(SelectionsNames.MONTHS_LIST)
				.getList().getSelectionByCode(lastMonth);

		command = command.replace("lastMonth.code", monthSel.getCode());
		command = command.replace("lastMonth.label", monthSel.getDescription());

		return command;
	}

	/**
	 * Resolve concatenation keywords
	 * @param value
	 * @return
	 */
	private String solveKeywords(String value) {

		String result = value.replace("|", "").replace("null", "");
		
		// concatenation keyword
		return result;
	}
	
	@Override
	public String toString() {
		return "Column " + column.getId() + " formula " + formula + " solved " + solvedFormula;
	}
	
	private boolean isColumnKeyword(TableColumn col, String value) {
		
		if (value == null)
			return false;
		
		return value.contains("\\" + col.getId());
	}
	
	/**
	 * Check dependencies in a recursive manner. This is actually the
	 * computation of the level of the tree of the dependencies.
	 */
	private void evalDependencies() {
		
		ArrayList<Integer> dependencies = new ArrayList<>();
		
		int count = 0;
		
		// Check columns dependencies
		for (TableColumn col : row.getSchema()) {
			
			// evaluate the dependency just for different columns
			// this avoid recursive definitions
			if (!col.equals(column) && isColumnKeyword(col, formula)) {
				
				count++;
				
				Formula child = new Formula(row, col, fieldHeader);
				dependencies.add(child.getDependenciesCount());
			}
		}
		
		// no children, we are in a leaf
		if (count == 0) {
			this.dependenciesCount = 0;
		}
		else {
			
			// conquer, get the max num of dependencies of the children
			// and add the new one for the current level
			this.dependenciesCount = Collections.max(dependencies) + 1;
		}
	}
}

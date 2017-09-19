package table_skeleton;

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
import xml_config_reader.Selection;
import xml_config_reader.XmlLoader;

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
	private static final String INTEGER = "[0-9]+";
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
	 * Get the number of dependencies in terms of \columnname.field
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
		
		if (formula == null || formula.isEmpty())
			return "";

		String value = formula.replace(" ", "");
		
		// solve dates
		value = solveDateFormula(value);
		
		print(value, "DATE");
		
		// solve special characters
		value = solveKeywords(value);
		
		print(value, "KEYWORDS");
		
		// solve columns values
		value = solveColumnsFormula(value);

		print(value, "COLUMNS");
		
		// solve relations formulas
		value = solveRelationFormula(value);
		
		print(value, "RELATIONS");
		
		// solve additions
		value = solveAdditions(value);
		
		print(value, "SUM");
		
		// solve if not null
		value = solveConditions(value);
		
		print(value, "CONDITIONS");
		
		// solve the if statements
		value = solveIf(value);
		
		print(value, "IF");
		
		// solve logical comparisons
		value = solveLogicalOperators(value);
		
		print(value, "LOGIC");
		
		// solve padding
		value = solvePadding(value);
		
		print(value, "PADDING");
		
		// solve trims
		value = solveTrims(value);
		
		print(value, "TRIMS");

		// remove all spaces
		this.solvedFormula = value.replace(" ", "");
		
		return solvedFormula;
	}
	
	private void print(String value, String header) {
		/*if (column.equals("progId"))
			System.out.println(header + " => " + value);*/
	}
	
	private String solveTrims(String value) {
		
		String command = value;
		
		String pattern = "END_TRIM\\(.*?," + INTEGER + "\\)";

		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(value);
		
		while (m.find()) {
			
			String match = m.group();
			
			// get match
			String elements = match.replace("END_TRIM(", "");
			
			String[] split = elements.split(",");
			
			if (split.length != 2) {
				System.err.println("Wrong END_TRIM formula " + match);
				return command;
			}
			
			String string = split[0];
			String charNumStr = split[1].replace(")", "");
			
			try {
				
				int charNum = Integer.valueOf(charNumStr);
				
				if (charNum > string.length()) {
					charNum = string.length();
				}
				
				String replacement = string.substring(string.length() - charNum, string.length());
				
				command = command.replace(match, replacement);
			}
			catch (NumberFormatException e) {
				return command;
			}
		}
		
		return command;
	}
	
	private String solvePadding(String value) {
		
		String command = value;
		
		String pattern = "ZERO_PADDING\\(.*?," + INTEGER + "\\)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(value);
		
		while (m.find()) {
			
			String match = m.group();
			
			// get match
			String elements = match.replace("ZERO_PADDING(", "");
			
			String[] split = elements.split(",");
			
			if (split.length != 2) {
				System.err.println("Wrong ZERO_PADDING formula " + match);
				return command;
			}
			
			String string = split[0];
			String charNumStr = split[1].replace(")", "");
			
			try {
				
				int charNum = Integer.valueOf(charNumStr);
				
				// make zero padding if necessary
				if (charNum > string.length()) {
					
					int paddingCount = charNum - string.length();
					for(int i = 0; i < paddingCount; ++i) {
						string = "0" + string;
					}
				}
				
				command = command.replace(match, string);
			}
			catch (NumberFormatException e) {
				return command;
			}
		}
		
		return command;
	}
	
	/**
	 * Solve logical operations
	 * @param value
	 * @return
	 */
	private String solveConditions(String value) {
		
		String command = value;
		
		String pattern = "IF_NOT_NULL\\(.*?,.*?,.*?\\)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(value);
		
		// if found
		if(m.find()) {

			String match = m.group();
			
			// get match
			String elements = match.replace("IF_NOT_NULL(", "");
			
			String[] split = elements.split(",");
			
			if (split.length != 3) {
				System.err.println("Wrong IF_NOT_NULL formula " + match);
				return value;
			}

			String condition = split[0];
			String trueCond = split[1];
			String falseCond = split[2].replace(")", "");
			
			// if we have a not null value
			if(!condition.isEmpty())
				command = command.replace(match, trueCond);
			else
				command = command.replace(match, falseCond);
		}
		
		return command;
	}
	
	/**
	 * Solve if statements
	 * @param value
	 * @return
	 */
	private String solveIf(String value) {
		
		String command = value;
		
		String pattern = "IF\\(.+?,.*?,.*?\\)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(value);
		
		// if found
		while(m.find()) {

			String match = m.group();
			
			// get match
			String elements = match.replace("IF(", "");
			
			String[] split = elements.split(",");
			
			if (split.length != 3) {
				System.err.println("Wrong IF formula " + match);
				return value;
			}

			String condition = split[0];
			String trueCond = split[1];
			String falseCond = split[2].replace(")", "");
			
			// if we have a true value
			if(BooleanValue.isTrue(condition))
				command = command.replace(match, trueCond);
			else
				command = command.replace(match, falseCond);
		}
		
		return command;
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
	
	/**
	 * Solve all the RELATION(parent, field) statements
	 * @param value
	 * @return
	 */
	private String solveRelationFormula(String value) {
		
		String command = value;
		
		Pattern p = Pattern.compile("RELATION\\{.+?,.+?\\}");
		Matcher m = p.matcher(command);
		
		// found a relation keyword
		while (m.find()) {
			
			// remove useless pieces
			String hit = m.group();
			
			String match = hit.replace("RELATION{", "").replace("}", "");
			
			// get operands by splitting with comma
			String[] split = match.split(",");
			
			if (split.length != 2) {
				System.err.println("Wrong RELATION statement, found " + hit);
				continue;
			}
			
			String parentId = split[0];
			String field = split[1];
			
			// field is name.code or name.label
			split = field.split("\\.");
			
			if (split.length != 2) {
				System.err.println("Need .code or .label, found " + hit);
				continue;
			}
			
			// get the field name of the parent
			String fieldName = split[0].replace(" ", "");
			
			// get the relation with the parent
			Relation r = row.getSchema().getRelationByParentTable(parentId);
			
			if (r == null) {
				System.err.println("No such relation found in the " 
						+ AppPaths.RELATIONS_SHEET + ", relation required: " + parentId);
				continue;
			}
			
			TableColumnValue colVal = row.get(r.getForeignKey());
			
			if (colVal == null) {
				System.err.println("No parent data found for " + r + " in the row " + row);
				continue;
			}
			
			// get from the child row the foreign key for the parent
			String foreignKey = row.get(r.getForeignKey()).getCode();
			
			if (foreignKey == null || foreignKey.isEmpty())
				continue;

			// get the row using the foreign key
			TableRow row = r.getParentValue(Integer.valueOf(foreignKey));

			if (row == null) {
				System.err.println("No relation value found for " + foreignKey + "; relation " + r);
				continue;
			}
			
			// get the required field and put it into the formula
			TableColumnValue parentValue = row.get(fieldName);
			
			if (parentValue == null) {
				System.err.println("No parent data value found for " + fieldName);
				continue;
			}

			// apply keywords
			match = match.replace(fieldName + ".code", parentValue.getCode());
			match = match.replace(fieldName + ".label", parentValue.getLabel());
			
			// remove also useless part
			match = match.replace(parentId + ",", "");
			
			// replace in the final string
			command = command.replace(hit, match);
		}

		return command.replace(" ", "");
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

		String command = value;
		
		Date today = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(today);
		
		// get last month
		cal.add(Calendar.MONTH, -1);
		
		String lastYear = String.valueOf(cal.get(Calendar.YEAR));

		command = command.replace("lastMonth.year.code", lastYear);
		command = command.replace("lastMonth.year.label", lastYear);
		
		// get last month term
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
		
		// replace row id statement with the actual row id
		result = result.replace("{rowId}", String.valueOf(row.getId()));
		
		// concatenation keyword
		return result;
	}
	
	@Override
	public String toString() {
		return "Column " + column.getId() + " formula " + formula + " solved " + solvedFormula;
	}
	
	/**
	 * Check if a column has a dependency with another
	 * column of the same table
	 * @param col
	 * @param value
	 * @return
	 */
	private boolean isDependentBy(TableColumn col, String value) {
		
		if (value == null)
			return false;
		
		return value.contains("\\" + col.getId());
	}
	
	/**
	 * Check dependencies in a recursive manner. This is actually the
	 * computation of the level of the tree of the dependencies.
	 * A column is dependent on the value of another column
	 * if it has in the field a formula with \columnName.code or
	 * \columnName.label
	 */
	private void evalDependencies() {
		
		ArrayList<Integer> dependencies = new ArrayList<>();
		
		int count = 0;
		
		// Check columns dependencies
		for (TableColumn col : row.getSchema()) {
			
			// evaluate the dependency just for different columns
			// this avoid recursive definitions
			if (!col.equals(column) && isDependentBy(col, formula)) {
				
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

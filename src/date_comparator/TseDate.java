package date_comparator;

public class TseDate implements Comparable<TseDate> {

	private int year;
	private int month;
	
	public TseDate(int year, int month) {
		this.year = year;
		this.month = month;
	}
	
	public TseDate(String year, String month) {
		this(Integer.valueOf(year), Integer.valueOf(month));
	}
	
	public int getYear() {
		return year;
	}
	public int getMonth() {
		return month;
	}
	
	public int getMonthsDifference(TseDate other) {
		return (this.year - other.year) * 12 + (this.month - other.month);
	}

	@Override
	public int compareTo(TseDate other) {
		int difference = getMonthsDifference(other);
		
		if (difference == 0)
			return 0;
		else if (difference > 0)
			return 1;
		else
			return -1;
	}
}

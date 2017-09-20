package duplicates_detector;

/**
 * Pair of objects that are considered equal. This object
 * is used to save duplicates records together with {@link DuplicatesDetector}.
 * @author avonva
 *
 * @param <T> the type of the objects
 */
public class Duplicate<T> {

	private T first;
	private T second;
	
	public Duplicate(T first, T second) {
		this.first = first;
		this.second = second;
	}
	
	public T getFirst() {
		return first;
	}
	
	public T getSecond() {
		return second;
	}
	
	@Override
	public String toString() {
		return "DUPLICATES: " + first.toString() + " AND " + second.toString();
	}
}

package duplicates_detector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class to check if in a list there are duplicates or not.
 * It can be used only with {@link Checkable} object which
 * implements the method that is used to check if two
 * record are equal or not (we did not override the equal
 * to avoid problems with other processes)
 * @author avonva
 *
 */
public class DuplicatesDetector {

	/**
	 * Detect the duplicates from a list of objects.
	 * @param list
	 * @return
	 */
	public static Collection<Duplicate<Checkable>> detect(List<?> list) {

		Collection<Duplicate<Checkable>> duplicates = new ArrayList<>();
		
		// for each element in the list but the last
		for (int i = 0; i < list.size() - 1; ++i) {
			
			// get current element
			Checkable first = (Checkable) list.get(i);
			
			// compare with all the others in the order
			for (int j = i + 1; j < list.size(); ++j) {

				Checkable second = (Checkable) list.get(j);
				// if they are equal
				if (first.sameAs(second)) {
					Duplicate<Checkable> dup = new Duplicate<Checkable>(first, second);
					duplicates.add(dup);
				}
			}
		}
		
		return duplicates;
	}
}

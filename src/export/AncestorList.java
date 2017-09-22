package export;

import java.util.ArrayList;

import table_skeleton.TableRow;

public class AncestorList extends ArrayList<TableRow> {

	private static final long serialVersionUID = -4413044120984858369L;

	/**
	 * Add a new ancestor to the list
	 * @param ancestor
	 */
	public void addAncestor(TableRow ancestor) {

		// check if already present and in case override
		int index = indexOf(ancestor);

		if (index != -1) {
			
			// override
			this.set(index, ancestor);
			
			// delete all the ancestors that are after
			// this ancestor, since we have changed branch
			// (they are deeper ancestors, that is, terms
			// that are more detailed)
			for (int i = index + 1; i < size(); ++i) {
				this.remove(i);
			}
		}
		else {
			// otherwise add it
			this.add(ancestor);
		}
	}

	@Override
	public int indexOf(Object o) {
		
		TableRow newAncestor = (TableRow) o;
		
		String tableName = newAncestor.getSchema().getTableIdField();
		
		for (int i = 0; i < this.size(); ++i) {
			
			String oldTableName = this.get(i).getSchema().getTableIdField();
			
			if (oldTableName.equals(tableName))
				return i;
		}
		
		return -1;
	}
}
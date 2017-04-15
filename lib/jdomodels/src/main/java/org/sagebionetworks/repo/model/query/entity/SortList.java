package org.sagebionetworks.repo.model.query.entity;


/**
 * Represents the sort columns of a query.
 *
 */
public class SortList extends SqlElement {
	
	ColumnReference sortColumn;
	boolean isAscending;
	
	/**
	 * Build sort list from input sort.
	 * 
	 * @param startIndex
	 * @param sorts
	 */
	public SortList(int startIndex, String sort, boolean isAscending){
		if(sort != null){
			this.sortColumn = new ColumnReference(sort, startIndex);
		}
		this.isAscending = isAscending;
	}

	@Override
	public void toSql(StringBuilder builder) {
		if(sortColumn != null){
			builder.append(" ORDER BY ");
			sortColumn.toSql(builder);
			if(isAscending){
				builder.append(" ASC");
			}else{
				builder.append(" DESC");
			}
		}
	}

	@Override
	public void bindParameters(Parameters parameters) {
		// nothing to bind
	}

}

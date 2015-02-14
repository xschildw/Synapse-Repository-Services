package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.query.AnnotationCondition;
import org.sagebionetworks.repo.model.entity.query.Condition;
import org.sagebionetworks.repo.model.entity.query.DateValue;
import org.sagebionetworks.repo.model.entity.query.EntityFieldCondition;
import org.sagebionetworks.repo.model.entity.query.EntityFieldName;
import org.sagebionetworks.repo.model.entity.query.EntityQuery;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResult;
import org.sagebionetworks.repo.model.entity.query.EntityQueryResults;
import org.sagebionetworks.repo.model.entity.query.IntegerValue;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.entity.query.StringValue;
import org.sagebionetworks.repo.model.entity.query.Value;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableList;

/**
 * Basic implementation of an entity query.
 * 
 * @author John
 *
 */
public class EntityQueryManagerImpl implements EntityQueryManager {
	
	private static final String NODE_TYPE = "nodeType";
	private static final String DEFAULT_FROM = "entity";
	private static final List<String> selectColumns;
	static{
		ArrayList<String> selectTemp = new ArrayList<String>(EntityFieldName.values().length);
		for(EntityFieldName name: EntityFieldName.values()){
			selectTemp.add(name.name());
		}
		selectTemp.add(NODE_TYPE);
		selectColumns = ImmutableList.copyOf(selectTemp);
	}
	
	@Autowired
	NodeQueryDao nodeQueryDao;

	@Override
	public EntityQueryResults executeQuery(EntityQuery query, UserInfo user) {
		if(query == null){
			throw new IllegalArgumentException("Query cannot be null");
		}
		if(user == null){
			throw new IllegalArgumentException("UserInfo cannot be null");
		}
		BasicQuery translatedQuery = translate(query);
		NodeQueryResults results = nodeQueryDao.executeQuery(translatedQuery, user);
		return translate(results);
	}
	
	/**
	 * Translate from an EntityQuery into a BasicQuery
	 * @param query
	 * @return
	 */
	public BasicQuery translate(EntityQuery query){
		BasicQuery translated = new BasicQuery();
		// Always select the basic entity fields.
		translated.setSelect(selectColumns);
		String from = DEFAULT_FROM;
		if(query.getFilterByType() != null){
			from = query.getFilterByType().name();
		}
		translated.setFrom(from);
		// The filter
		translated.setFilters(translateConditions(query.getConditions()));
		// limit
		if(query.getLimit() != null){
			translated.setLimit(query.getLimit());
		}
		// offset
		if(query.getOffset() != null){
			translated.setOffset(query.getOffset());
		}
		// sort
		if(query.getSort() != null){
			translated.setAscending(SortDirection.ASC.equals(query.getSort().getDirection()));
			translated.setSort(query.getSort().getColumnName());
		}
		return translated;
	}
	
	/**
	 * Translate from conditions to expressions
	 * @param conditions
	 * @return
	 */
	public List<Expression> translateConditions(List<Condition> conditions){
		List<Expression> translated = new ArrayList<Expression>();
		if(conditions != null){
			for(Condition condition: conditions){
				translated.add(translateCondition(condition));
			}
		}
		return translated;
	}
	
	/**
	 * Translate a condition into an expression.
	 * 
	 * @param condition
	 * @return
	 */
	public Expression translateCondition(Condition condition){
		if(condition == null){
			throw new IllegalArgumentException("Condition cannot be null");
		}
		if(condition.getOperator() == null){
			throw new IllegalArgumentException("Condition.operator cannot be null");
		}
		if(condition.getRightHandSide() == null){
			throw new IllegalArgumentException("Condition.rightHandSide() cannot be null");
		}
		Object rhs = translateValue(condition.getRightHandSide());
		Comparator comp = Comparator.valueOf(condition.getOperator().name());
		if(condition instanceof EntityFieldCondition){
			EntityFieldCondition efc = (EntityFieldCondition) condition;
			return new Expression(new CompoundId(null, efc.getLeftHandSide().name()),comp , rhs);
		}else if(condition instanceof AnnotationCondition){
			AnnotationCondition ac = (AnnotationCondition) condition;
			return new Expression(new CompoundId(null, ac.getLeftHandSide()), comp, rhs);
		}else{
			throw new IllegalArgumentException("Unknown condition type: "+condition.getConcreteType());
		}
	}
	
	/**
	 * Translate a list of values.
	 * @param values
	 * @return
	 */
	public Object translateValue(List<Value> values){
		if(values.size() == 1){
			return translateValue(values.get(0));
		}else{
			List<Object> list = new ArrayList<Object>(values.size());
			for(Value v: values){
				list.add(translateValue(v));
			}
			return list;
		}
	}
	
	/**
	 * Translate a single value
	 * @param value
	 * @return
	 */
	public Object translateValue(Value value){
		if(value instanceof StringValue){
			StringValue sv = (StringValue) value;
			return sv.getValue();
		}else if(value instanceof DateValue){
			DateValue dv = (DateValue)value;
			return dv.getValue().getTime();
		}else if(value instanceof IntegerValue){
			IntegerValue iv = (IntegerValue) value;
			return iv.getValue();
		}else{
			throw new IllegalArgumentException("Unknown ValueType: "+value.getConcreteType());
		}
	}
	
	/**
	 * Translate from a NodeQueryResults to EntityQueryResult.
	 * @param result
	 * @return
	 */
	public EntityQueryResults translate(NodeQueryResults result){
		EntityQueryResults translated = new EntityQueryResults();
		// Convert each results
		translated.setEntities(translate(result.getAllSelectedData()));
		translated.setTotalEntityCount(result.getTotalNumberOfResults());
		return translated;
	}
	
	/**
	 * Translate a list of maps to a list of BasicEntity
	 * @param allSelectedData
	 * @return
	 */
	public List<EntityQueryResult> translate(List<Map<String, Object>> allSelectedData){
		List<EntityQueryResult> translated = new ArrayList<EntityQueryResult>(allSelectedData.size());
		for(Map<String, Object> map: allSelectedData){
			EntityQueryResult eqr= translate(map);
			translated.add(eqr);
		}
		return translated;
	}
	
	/**
	 * Translate from the selected data into a basic entity.
	 * @param allSelectedData
	 * @return
	 */
	public EntityQueryResult translate(Map<String, Object> map) {
		EntityQueryResult translated = new EntityQueryResult();
		translated.setId((String) map.get(EntityFieldName.id.name()));
		translated.setName((String) map.get(EntityFieldName.name.name()));
		translated.setParentId((String) map.get(EntityFieldName.parentId.name()));
		translated.setCreatedByPrincipalId((Long) map.get(EntityFieldName.createdByPrincipalId.name()));
		translated.setCreatedOn(new Date((Long) map.get(EntityFieldName.createdOn.name())));
		translated.setModifiedByPrincipalId((Long) map.get(EntityFieldName.modifiedByPrincipalId.name()));
		translated.setModifiedOn(new Date((Long) map.get(EntityFieldName.modifiedOn.name())));
		translated.setEtag((String) map.get(EntityFieldName.eTag.name()));
		translated.setVersionNumber((Long) map.get(EntityFieldName.versionNumber.name()));
		translated.setBenefactorId((Long) map.get(EntityFieldName.benefactorId.name()));
		translated.setProjectId((Long) map.get(EntityFieldName.projectId.name()));
		translated.setActivityId((String) map.get(EntityFieldName.activityId.name()));
		// type is special as it is an index in the database but a string to the user.
		EntityType type =EntityType.getTypeForId(((Integer)map.get(NODE_TYPE)).shortValue());
		translated.setEntityType(type.getEntityType());
		return translated;
	}

}

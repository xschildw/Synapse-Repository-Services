package org.sagebionetworks.cloudwatch;

import java.util.Date;

import com.amazonaws.services.cloudwatch.model.MetricDatum;

/**
 * Data transfer object for latency information.
 * @author ntiedema
 */
public class CloudWatchDatum {
	String namespace;
	MetricDatum datum;
	String name;
	long latency;	//time duration
	String unit;	
	Date timestamp;
	
	/**
	 * Default ProfileData constructor.  Want class to be able to expand, so default
	 * constructor will be only available constructor
	 */
	public CloudWatchDatum(){
		datum = new MetricDatum();
	}
	
	/**
	 * Setter for namespace.
	 * @param namespace
	 * @throws IllegalArgument Exception
	 */
	public void setNamespace(String namespace){
		if (namespace == null){
			throw (new IllegalArgumentException());
		}
		this.namespace = namespace;
	}
	
	/**
	 * Setter for name.
	 * @param name
	 * @throws IllegalArgumentException
	 */
	public void setName(String name){
		if (name == null){
			throw (new IllegalArgumentException());
		}
		this.datum.setMetricName(name);
	}
	
	/**
	 * Setter for latency.
	 * @param latency
	 * @throws IllegalArgumentException
	 */
	public void setValue(Double value){
		//a latency can't be smaller than 0
		if (value < 0.0){
			throw (new IllegalArgumentException());
		}
		this.datum.setValue(value);
	}

	/**
	 * Setter for unit.
	 * @param unit
	 * @throws IllegalArgumentException
	 */
	public void setUnit(String unit){
		if (unit == null){
			throw (new IllegalArgumentException());
		}
		this.datum.setUnit(unit);
	}
	
	/**
	 * Setter for timestamp.
	 * @param timestamp
	 * @throws IllegalArgumentException
	 */
	public void setTimestamp(Date timestamp){
		if (timestamp == null){
			throw (new IllegalArgumentException());
		}
		this.datum.setTimestamp(timestamp);
	}
	
	/**
	 * Getter for namespace.
	 * @return String
	 */
	public String getNamespace(){
		return namespace;
	}
	
	/**
	 * Getter for name.
	 * @return String
	 */
	public String getName(){
		return datum.getMetricName();
	}
	
	/**
	 * Gettr for latency.
	 * @return long
	 */
	public Double getValue(){
		return datum.getValue();
	}
	/**
	 * Getter for unit.
	 * @return String
	 */
	public String getUnit(){
		return datum.getUnit();
	}
	
	/**
	 * Getter for timestamp.
	 * @return Date
	 */
	public Date getTimestamp(){
		return datum.getTimestamp();
	}
	
	public MetricDatum getMetricDatum() {
		return datum;
	}
	
	/**
	 * toString method.
	 * @return String
	 */
	public String toString(){
		String toReturn = this.getNamespace() + ":" + this.getName() + ":" + this.getValue() + 
			":" + this.getUnit() + ":" + this.getTimestamp().toString();
		return toReturn;
	}
}

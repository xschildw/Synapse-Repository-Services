package org.sagebionetworks.cloudwatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;

public class CloudWatchPublisher {
	static private Logger log = LogManager.getLogger(Consumer.class);
	
	public static final int MAX_BATCH_SIZE = 20;

	@Autowired
	AmazonCloudWatchClient cloudWatchClient;
	
	private ConcurrentLinkedQueue<CloudWatchDatum> listCloudWatchDatum = new ConcurrentLinkedQueue<CloudWatchDatum>();

	public CloudWatchPublisher() {
		
	}
	
	public CloudWatchPublisher(AmazonCloudWatchClient client) {
		cloudWatchClient = client;
	}
	
	public AmazonCloudWatchClient getCloudWatchClient() {
		return cloudWatchClient;
	}
	
	public void setCloudWatchClient(AmazonCloudWatchClient client) {
		cloudWatchClient = client;
	}
	
	public void addProfileData(CloudWatchDatum m) {
		listCloudWatchDatum.add(m);
	}

	protected List<CloudWatchDatum> pollListFromQueue(){
		List<CloudWatchDatum> list = new LinkedList<CloudWatchDatum>();
		for(CloudWatchDatum pd = this.listCloudWatchDatum.poll(); pd != null; pd = this.listCloudWatchDatum.poll()){
			list.add(pd);
		}
		return list;
	}
	
	protected Map<String, List<MetricDatum>> getNamespaces(List<CloudWatchDatum> l) {
		Map<String, List<MetricDatum>> m = new HashMap<String, List<MetricDatum>>();
		for (CloudWatchDatum pd : l) {
			List<MetricDatum> listMD = m.get(pd.getNamespace());
			if(listMD == null){
				listMD = new ArrayList<MetricDatum>();
				m.put(pd.getNamespace(), listMD);
			}
			listMD.add(pd.getMetricDatum());
		}
		return m;
	}
	
	protected void sendMetrics(PutMetricDataRequest listForCW, AmazonCloudWatchClient client) {
		try {
			client.putMetricData(listForCW);
		} catch (Exception e1) {
			log.error("failed to send data to CloudWatch ", e1);
			throw new RuntimeException(e1);
		}
	}

	public List<String> publish() {
		try {
			List<CloudWatchDatum> mdToProcess = pollListFromQueue();

			// Partition on namespaces
			Map<String, List<MetricDatum>> namespaces = getNamespaces(mdToProcess);
			
			//need to collect the messages for testing
			List<String> toReturn = new ArrayList<String>();
			// Batch up to MAX_BATCH_SIZE metricDatum per call
			for (String key : namespaces.keySet()){
				List<MetricDatum> fullList = namespaces.get(key);
				PutMetricDataRequest batch = null;
				for(MetricDatum md: fullList){
					if(batch == null){
						batch = new PutMetricDataRequest();
						batch.setNamespace(key);
					}
					batch.getMetricData().add(md);
					if(batch.getMetricData().size() == MAX_BATCH_SIZE){
						sendMetrics(batch, cloudWatchClient);
						batch = null;
					}
				}
				if(batch != null){
					sendMetrics(batch, cloudWatchClient);
				}
			}
			return toReturn;
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}
	}
}

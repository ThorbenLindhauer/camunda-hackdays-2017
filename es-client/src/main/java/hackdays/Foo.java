package hackdays;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

public class Foo {

	public static void main(String[] args) throws IOException {
		TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
		.addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress("127.0.0.1", 9300)));
		
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
			.must(QueryBuilders.termQuery("processDefinitionId", "leadQualification:46:3ce29e5a-1ec3-11e7-8ace-0aa2e56f42b1"))
			.must(QueryBuilders.nestedQuery("events", QueryBuilders.matchQuery("events.activityId", "UserTask_1g1zsp8"), ScoreMode.None));
		
		TimeValue timeout = new TimeValue(60000L);
		SearchResponse response = client.prepareSearch("optimize").setTypes("process-instance")
			.setScroll(timeout)
			.setQuery(queryBuilder)
			.setSize(100)
			.get();

		Map<String, List<Long>> durationMap = new HashMap<>();
		
		FileWriter writer = new FileWriter(new File("./duration_histogram.csv"));
		CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.withDelimiter(','));
		csvPrinter.printRecord("processInstanceId", "activityId", "duration");
		do
		{
			for (SearchHit hit : response.getHits().getHits()) {

				Map<String, Object> instance = hit.getSourceAsMap();
				List<Map<String, Object>> activityInstances = (List<Map<String, Object>>) instance.get("events");
				for (Map<String, Object> activityInstance : activityInstances)
				{
					long duration = ((Number) activityInstance.get("durationInMs")).longValue();
					String activityId = (String) activityInstance.get("activityId");
					
						csvPrinter.printRecord(instance.get("processInstanceId"), activityId, duration);
				}
			}
			
			response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeout).get();
		}
		while (response.getHits().getHits().length != 0);
		
		csvPrinter.flush();
		csvPrinter.close();
		writer.close();
	}
	
	protected static <K, V> void put(Map<K, List<V>> map, K key, V value)
	{
		List<V> list = map.get(key);
		if (list == null)
		{
			list = new ArrayList<V>();
			map.put(key, list);
		}
		
		list.add(value);
		
	}
}

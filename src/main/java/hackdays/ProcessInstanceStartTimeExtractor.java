package hackdays;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

public class ProcessInstanceStartTimeExtractor {

	public static void main(String[] args) throws IOException {
		TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
		.addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress("127.0.0.1", 9300)));
		
//		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
//			.must(QueryBuilders.termQuery("processDefinitionKey", "renewal"));
		
		TimeValue timeout = new TimeValue(60000L);
		SearchResponse response = client.prepareSearch("optimize").setTypes("process-instance")
			.setScroll(timeout)
			.setQuery(QueryBuilders.matchAllQuery())
			.setSize(100)
			.get();

		Map<String, List<Long>> durationMap = new HashMap<>();
		
		FileWriter writer = new FileWriter(new File("C:\\camunda\\hackdays\\2017\\scripts\\instance-start-dates.csv"));
		CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.withDelimiter(','));
		csvPrinter.printRecord("processDefinitionKey", "processInstanceId", "startTime");
		do
		{
			for (SearchHit hit : response.getHits().getHits()) {

				Map<String, Object> instance = hit.getSourceAsMap();
				csvPrinter.printRecord(instance.get("processDefinitionKey"), instance.get("processInstanceId"), instance.get("startDate"));
			}
			
			response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeout).get();
		}
		while (response.getHits().getHits().length != 0);
		
//		int i = 0;
//		
//		
//		for (Map.Entry<String, List<Long>> activity : durationMap.entrySet())
//		{
//			for (Long duration : activity.getValue())
//			{
//			}
//		}
//		
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

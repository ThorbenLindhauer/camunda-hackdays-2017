package hackdays;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.FileSystems;
import java.util.ArrayList;
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

public class HistogramTransformer {


  private static final String[] VARIABLE_TYPES = {"stringVariables", "integerVariables", "longVariables", "shortVariables", "doubleVariables", "dateVariables", "booleanVariables"};

  private static final String DURATION_HISTOGRAM_FILE_PATH = "." + FileSystems.getDefault().getSeparator() + "duration_histogram.csv";
  private static final String VARIABLE_HISTOGRAM_FILE_PATH = "." + FileSystems.getDefault().getSeparator() + "variable_histogram.csv";

  public static void main(String[] args) throws IOException {
		TransportClient client = new PreBuiltTransportClient(Settings.EMPTY)
		.addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress("localhost", 9300)));

		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
			.must(QueryBuilders.termQuery("processDefinitionId", "leadQualification:46:3ce29e5a-1ec3-11e7-8ace-0aa2e56f42b1"));

		TimeValue timeout = new TimeValue(60000L);
		SearchResponse response = client.prepareSearch("optimize").setTypes("process-instance")
			.setScroll(timeout)
			.setQuery(queryBuilder)
			.setSize(1000)
			.get();

		FileWriter durationWriter = new FileWriter(new File(DURATION_HISTOGRAM_FILE_PATH));
		CSVPrinter durationCsvPrinter = new CSVPrinter(durationWriter, CSVFormat.EXCEL.withDelimiter(','));
		durationCsvPrinter.printRecord("processInstanceId", "activityId", "duration");

		FileWriter variableWriter = new FileWriter(new File(VARIABLE_HISTOGRAM_FILE_PATH));
		CSVPrinter variableCsvPrinter = new CSVPrinter(variableWriter, CSVFormat.EXCEL.withDelimiter(','));
		variableCsvPrinter.printRecord("processInstanceId", "variableName", "value");

		do
		{
			for (SearchHit hit : response.getHits().getHits()) {

				Map<String, Object> instance = hit.getSourceAsMap();
				extractActivityDuration(durationCsvPrinter, instance);

				for (String variableType : VARIABLE_TYPES) {
					extractVariableValues(variableCsvPrinter, instance, variableType);
				}

			}

			response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeout).get();
		}
		while (response.getHits().getHits().length != 0);

		durationCsvPrinter.flush();
		durationCsvPrinter.close();
		durationWriter.close();

		variableCsvPrinter.flush();
		variableCsvPrinter.close();
		variableWriter.close();
	}

	private static void extractActivityDuration(CSVPrinter durationCsvPrinter, Map<String, Object> instance) throws IOException {
		List<Map<String, Object>> activityInstances = (List<Map<String, Object>>) instance.get("events");
		for (Map<String, Object> activityInstance : activityInstances)
    {
      long duration = ((Number) activityInstance.get("durationInMs")).longValue();
      String activityId = (String) activityInstance.get("activityId");
      durationCsvPrinter.printRecord(instance.get("processInstanceId"), activityId, duration);
    }
	}

	private static void extractVariableValues(CSVPrinter variableCsvPrinter, Map<String, Object> instance, String variableType) throws IOException {
		List<Map<String, Object>> stringVariables = (List<Map<String, Object>>) instance.get(variableType);
		for (Map<String, Object> stringVariable : stringVariables) {
      String variableName = (String) stringVariable.get("name");
      Object variableValue = stringVariable.get("value");

      variableCsvPrinter.printRecord(instance.get("processInstanceId"), variableName, variableValue);
    }
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
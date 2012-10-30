package org.elasticsearch.rest.action.views;

import fr.tlrx.elasticsearch.test.annotations.ElasticsearchBulkRequest;
import fr.tlrx.elasticsearch.test.annotations.ElasticsearchClient;
import fr.tlrx.elasticsearch.test.annotations.ElasticsearchIndex;
import fr.tlrx.elasticsearch.test.annotations.ElasticsearchNode;
import fr.tlrx.elasticsearch.test.support.junit.runners.ElasticsearchRunner;
import org.elasticsearch.client.Client;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ElasticsearchRunner.class)
@ElasticsearchNode
public class ViewsActionTest {

	@ElasticsearchClient
	Client client;
	
	@Test
	@ElasticsearchIndex(indexName = "library")
	@ElasticsearchBulkRequest(dataFile = "books.json")
	public void test(){
		
		// Creates a new view
		client.prepareIndex("views", "library", "book")
				.setSource("{\n" +
							"	\"views\" : {\n" +
							"		\"view_1\" : {\n" +
							"			\"language\" : \"mvel\"," +
							"			\"content_type\" : \"text/html\"," +
							"			\"script\" : \"<html><body>nom is {{_source.nom}}</body></html>\"" +
							"		}\n" +
							"	}\n" +
							"}")
				.execute()
				.actionGet();
		
		System.out.println("stop");
	}
}

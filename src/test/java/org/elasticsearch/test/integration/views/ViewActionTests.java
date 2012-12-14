/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.test.integration.views;


import com.github.tlrx.elasticsearch.test.EsSetup;
import org.elasticsearch.action.view.ViewAction;
import org.elasticsearch.action.view.ViewRequest;
import org.elasticsearch.action.view.ViewResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.github.tlrx.elasticsearch.test.EsSetup.*;
import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;

public class ViewActionTests {

    EsSetup esSetup;

    @Before
    public void setUp() {

        // Instantiates a local node & client with few templates in config dir
        esSetup = new EsSetup(ImmutableSettings
                                .settingsBuilder()
                                    .put("path.conf", "./target/test-classes/org/elasticsearch/test/integration/views/config/")
                                    .build());

        // Clean all and create test org.elasticsearch.test.integration.views.mappings.data
        esSetup.execute(

                deleteAll(),

                createIndex("catalog")
                        .withMapping("product", fromClassPath("org/elasticsearch/test/integration/views/mappings/product.json"))
                        .withData(fromClassPath("org/elasticsearch/test/integration/views/data/products.json")),

                createIndex("manufacturers")
                        .withMapping("brand", fromClassPath("org/elasticsearch/test/integration/views/mappings/brand.json"))
                        .withData(fromClassPath("org/elasticsearch/test/integration/views/data/brands.json"))
        );
    }

    @Test
    public void testDefaultView() throws Exception {
        ViewResponse response = esSetup.client().execute(ViewAction.INSTANCE, new ViewRequest("catalog", "product", "1")).get();
        assertEquals("Rendering the document #1 in version 1 of type product from index catalog", new String(response.content(), "UTF-8"));
    }

    @Test
    public void testFullView() throws Exception {
        ViewResponse response = esSetup.client().execute(ViewAction.INSTANCE, new ViewRequest("catalog", "product", "2").format("full")).get();
        assertEquals("<div id=\"product-2\"><h2>Detail of 1952 ALPINE RENAULT 1300</h2><p>Year: 1952, price: 98.58€</p><p>Turnable front wheels; steering function; detailed interior; detailed engine; opening hood; opening trunk; opening doors; and detailed chassis.</p><p>© Copyright Renault</p></div>", new String(response.content(), "UTF-8"));
    }

    @Test
    public void testBinaryView() throws Exception {
        ViewResponse response = esSetup.client().execute(ViewAction.INSTANCE, new ViewRequest("catalog", "product", "1").format("logo")).get();
        assertNotNull(response.content());
        assertEquals(0, response.content().length);

        response = esSetup.client().execute(ViewAction.INSTANCE, new ViewRequest("catalog", "product", "2").format("logo")).get();
        assertNotNull(response.content());
        assertEquals(61752, response.content().length);
    }

    @Test
    public void testUndefinedView() throws Exception {
        try {
            ViewResponse response = esSetup.client().execute(ViewAction.INSTANCE, new ViewRequest("catalog", "product", "1").format("undefined")).get();
            fail("Exception expected!");
        } catch (Exception e) {
            assertEquals("org.elasticsearch.view.exception.ElasticSearchViewNotFoundException: No view [undefined] found for document type [product]", e.getMessage());
        }
    }

    @Test
    public void testCustomView() throws Exception {

        // Index a custom view
        esSetup.execute(
            index("catalog", "list-of-products-by-size", "1:10")
                .withSource("{\n" +
                        "    \"views\":{\n" +
                        "        \"default\":{\n" +
                        "            \"view_lang\": \"mvel\",\n" +
                        "            \"queries\": {\n" +
                        "                \"products_with_size_1_10\": {\n" +
                        "                    \"indices\": \"catalog\",\n" +
                        "                    \"types\": [\"product\"],\n" +
                        "                    \"query\" : {\n" +
                        "                          \"constant_score\" : {\n" +
                        "                              \"filter\" : {\n" +
                        "                                  \"term\" : { \"scale\" : \"1:10\"}\n" +
                        "                              }\n" +
                        "                          }\n" +
                        "                    }\n" +
                        "                }\n" +
                        "            },\n" +
                        "            \"view\" : \"@includeNamed{'list-of-products'; title='List of products'}\"\n" +
                        "        }\n" +
                        "    }\n" +
                        "}")
        );

        ViewResponse response = esSetup.client().execute(ViewAction.INSTANCE, new ViewRequest("catalog", "list-of-products-by-size", "1:10")).get();
        assertEquals(fromClassPath("org/elasticsearch/test/integration/views/config/views/list-of-products.html").toString(), new String(response.content(), "UTF-8"));
    }

    @Test
    public void testCustomViewWithMultipleQueries() throws Exception {

        // Index a custom view
        esSetup.execute(
                index("web", "pages", "home")
                        .withSource("{\n" +
                                "    \"views\": {\n" +
                                "        \"default\": {\n" +
                                "            \"view_lang\": \"mvel\",\n" +
                                "            \"queries\": [\n" +
                                "                {\n" +
                                "                    \"products_with_price_lower_than_50\": {\n" +
                                "                        \"indices\": \"catalog\",\n" +
                                "                        \"types\": [\"product\"],\n" +
                                "                        \"query\" : {\n" +
                                "                              \"constant_score\" : {\n" +
                                "                                  \"filter\" : {\n" +
                                "                                      \"range\" : { \"price\" : { \"to\": 50, \"include_upper\": true } }\n" +
                                "                                  }\n" +
                                "                              }\n" +
                                "                        }\n" +
                                "                    }\n" +
                                "                },\n" +
                                "                {\n" +
                                "                    \"brands\": {\n" +
                                "                        \"indices\": \"manufacturers\",\n" +
                                "                        \"types\": [\"brand\"],\n" +
                                "                        \"query\" : {\n" +
                                "                              \"match_all\" : {}\n" +
                                "                        },\n" +
                                "                        \"sort\" : [\n" +
                                "                              { \"name\" : \"asc\" },\n"+
                                "                              { \"country\" : \"asc\" }\n"+
                                "                        ],\n"+
                                "                        \"fields\" : [ \"name\" ]\n"+
                                "                    }\n" +
                                "                }\n" +
                                "            ],\n" +
                                "            \"view\" : \"@includeNamed{'list-of-products-with-brands'; title='Welcome'}\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}")
        );

        ViewResponse response = esSetup.client().execute(ViewAction.INSTANCE, new ViewRequest("web", "pages", "home")).get();
        assertEquals(fromClassPath("org/elasticsearch/test/integration/views/config/views/list-of-products-with-brands.html").toString(), new String(response.content(), "UTF-8"));
    }


    @After
    public void tearDown() throws Exception {
        esSetup.terminate();
    }
}

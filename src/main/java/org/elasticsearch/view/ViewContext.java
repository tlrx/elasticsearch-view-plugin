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
package org.elasticsearch.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;

public class ViewContext {

    private String lang;
    private String contentType;
    private String view;
    private String index;
    private String type;
    private String id;
    private Long version;
    private Map<String, Object> source;
    private Map<String, SearchHits> queries;

    public ViewContext(String lang, String view, String contentType) {
        this.lang = lang;
        this.view = view;
        this.contentType = contentType;
    }

    public String lang() {
        return lang;
    }

    public String contentType() {
        return contentType;
    }

    public String view() {
        return view;
    }

    public Map<String, SearchHits> queries() {
        return queries;
    }

    public ViewContext index(String index) {
        this.index = index;
        return this;
    }

    public ViewContext type(String type) {
        this.type = type;
        return this;
    }

    public ViewContext id(String id) {
        this.id = id;
        return this;
    }

    public ViewContext version(Long version) {
        this.version = version;
        return this;
    }

    public ViewContext source(Map<String, Object> source) {
        this.source = source;
        return this;
    }

    public ViewContext queriesAndHits(String queryName, SearchHits hits) {
        if (this.queries == null) {
            this.queries = ConcurrentCollections.newConcurrentMap();
        }
        this.queries.put(queryName, hits);
        return this;
    }

    public Map<String, Object> varsAsMap() {
        Map<String, Object> builder = new HashMap<String, Object>();
        if (this.index != null) {
            builder.put("_index", this.index);
        }
        if (this.type != null) {
            builder.put("_type", this.type);
        }
        if (this.id != null) {
            builder.put("_id", this.id);
        }
        if (this.version != null) {
            builder.put("_version", this.version.toString());
        }
        if (this.source != null) {
            ImmutableMap.Builder<String, Object> sourceAsMap = ImmutableMap.builder();
            sourceAsMap.putAll(this.source);
            builder.put("_source", sourceAsMap.build());
        }

        if (this.queries() != null) {
            ImmutableMap.Builder<String, Object> queryHitsAsMap = ImmutableMap.builder();
            for (String query : this.queries().keySet()) {

                SearchHits searchHits = this.queries().get(query);
                List<Map<String, Object>> hits = new ArrayList<Map<String, Object>>(searchHits.hits().length);
                for (SearchHit hit : searchHits.hits()) {
                    ImmutableMap.Builder<String, Object> hitProperties = ImmutableMap.builder();
                    hitProperties.put("_index", hit.index());
                    hitProperties.put("_type", hit.type());
                    hitProperties.put("_id", hit.id());
                    hitProperties.put("_version", hit.version());

                    Map<String, Object> sourceAsMap = hit.sourceAsMap();
                    if(sourceAsMap != null){
                        hitProperties.put("_source", hit.sourceAsMap());
                    }

                    Map<String, SearchHitField> fields = hit.fields();
                    if (fields != null) {
                        Map<String, Object> fieldsMap = new HashMap<String, Object>();
                        for (SearchHitField field : hit.fields().values()) {
                            fieldsMap.put(field.name(), field.value());
                        }
                        hitProperties.put("fields", fieldsMap);
                    }
                    hits.add(hitProperties.build());
                }

                queryHitsAsMap.put(query, hits);
            }
            builder.put("_queries", queryHitsAsMap.build());
        }

        return builder;
    }
}

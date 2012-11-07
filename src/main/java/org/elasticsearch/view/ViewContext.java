package org.elasticsearch.view;

import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ViewContext {

    private String lang;
    private String view;
    private String index;
    private String type;
    private String id;
    private Long version;
    private Map<String, Object> source;
    private Map<String, SearchHits> queries;

    public ViewContext(String lang, String view) {
        this.lang = lang;
        this.view = view;
    }

    public String lang() {
        return lang;
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
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
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
                    hitProperties.put("_source", hit.sourceAsMap());
                    hits.add(hitProperties.build());
                }

                queryHitsAsMap.put(query, hits);
            }
            builder.put("_queries", queryHitsAsMap.build());
        }

        return builder.build();
    }
}

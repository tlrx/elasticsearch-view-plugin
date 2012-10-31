package org.elasticsearch.view;

import org.elasticsearch.common.collect.ImmutableMap;

import java.util.Map;

public class ViewContext {

    private String lang;
    private String view;
    private String index;
    private String type;
    private String id;
    private Long version;
    private Map<String, Object> source;

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

        return builder.build();
    }
}

package org.elasticsearch.action.view;

import org.elasticsearch.action.support.single.shard.SingleShardOperationRequest;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Required;

public class ViewRequest extends SingleShardOperationRequest<ViewRequest> {

    private String type;
    private String id;
    private String format;

    ViewRequest() {
        type = "_all";
        format = "_default_";
    }

    /**
     * Constructs a new view request against the specified index with the type and id.
     *
     * @param index The index to get the document from
     * @param type  The type of the document
     * @param id    The id of the document
     */
    public ViewRequest(String index, String type, String id) {
        super(index);
        this.type = type;
        this.id = id;
    }

    /**
     * Sets the type of the document to fetch.
     */
    public ViewRequest type(@Nullable String type) {
        if (type == null) {
            type = "_all";
        }
        this.type = type;
        return this;
    }

    /**
     * Sets the id of the document to fetch.
     */
    @Required
    public ViewRequest id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the format of the document
     */
    public ViewRequest format(String format) {
        this.format = format;
        return this;
    }

    public String type() {
        return type;
    }

    public String id() {
        return id;
    }

    public String format() {
        return format;
    }

}

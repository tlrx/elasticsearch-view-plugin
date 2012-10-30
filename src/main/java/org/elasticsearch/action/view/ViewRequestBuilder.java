package org.elasticsearch.action.view;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.single.shard.SingleShardOperationRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalClient;

public class ViewRequestBuilder extends SingleShardOperationRequestBuilder<ViewRequest, ViewResponse, ViewRequestBuilder> {

    public ViewRequestBuilder(Client client) {
        super((InternalClient) client, new ViewRequest());
    }

    public ViewRequestBuilder(Client client, String index, String type, String id) {
        super((InternalClient) client, new ViewRequest(index, type, id));
    }

    /**
     * Sets the type of the document to fetch. If set to <tt>null</tt>, will use just the id to fetch the
     * first document matching it.
     */
    public ViewRequestBuilder setType(String type) {
        request.type(type);
        return this;
    }

    /**
     * Sets the id of the document to fetch.
     */
    public ViewRequestBuilder setId(String id) {
        request.id(id);
        return this;
    }

    @Override
    protected void doExecute(ActionListener<ViewResponse> listener) {
        ((Client) client).execute(ViewAction.INSTANCE, request, listener);
    }
}

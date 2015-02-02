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
package org.elasticsearch.action.view;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.single.shard.SingleShardOperationRequestBuilder;
import org.elasticsearch.client.Client;


public class ViewRequestBuilder extends SingleShardOperationRequestBuilder<ViewRequest, ViewResponse, ViewRequestBuilder> {

    public ViewRequestBuilder(Client client) {
        super(client, new ViewRequest());
    }

    public ViewRequestBuilder(Client client, String index, String type, String id) {
        super(client, new ViewRequest(index, type, id));
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

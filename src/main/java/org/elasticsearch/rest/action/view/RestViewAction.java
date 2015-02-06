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
package org.elasticsearch.rest.action.view;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.view.ViewAction;
import org.elasticsearch.action.view.ViewRequest;
import org.elasticsearch.action.view.ViewResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;
import org.elasticsearch.view.exception.ElasticSearchViewNotFoundException;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestStatus.NOT_FOUND;

public class RestViewAction extends BaseRestHandler {

    @Inject
    public RestViewAction(Settings settings, Client client, RestController controller) {
        super(settings, controller, client);
        controller.registerHandler(GET, "/_view/{index}/{type}/{id}", this);
        controller.registerHandler(GET, "/_view/{index}/{type}/{id}/{format}", this);
    }

    public void handleRequest(final RestRequest request, final RestChannel channel, Client client) {
        ViewRequest viewRequest = new ViewRequest(request.param("index"), request.param("type"), request.param("id"));
        if (request.hasParam("format")) {
            viewRequest.format(request.param("format"));
        }

        // we just send a response, no need to fork
        viewRequest.listenerThreaded(false);
        // we don't spawn, then fork if local
        viewRequest.operationThreaded(true);

        client.execute(ViewAction.INSTANCE, viewRequest, new ActionListener<ViewResponse>() {

            public void onResponse(ViewResponse response) {
                try {
                    channel.sendResponse(new BytesRestResponse(RestStatus.OK, response.contentType(), response.content()));
                } catch (Exception e) {
                    onFailure(e);
                }
            }

            public void onFailure(Throwable e) {
              
                    if (e instanceof ElasticSearchViewNotFoundException) {
                        channel.sendResponse(new BytesRestResponse(NOT_FOUND, e.toString()));
                    } else {
                        channel.sendResponse(new BytesRestResponse(RestStatus.BAD_REQUEST, e.toString()));
                    }
            }
        });
    }
}

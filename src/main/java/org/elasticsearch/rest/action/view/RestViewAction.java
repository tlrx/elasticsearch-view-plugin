package org.elasticsearch.rest.action.view;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.view.ViewAction;
import org.elasticsearch.action.view.ViewRequest;
import org.elasticsearch.action.view.ViewResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.*;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestStatus.OK;

public class RestViewAction extends BaseRestHandler {

    @Inject
    public RestViewAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(GET, "/{index}/{type}/{id}/_view", this);
        controller.registerHandler(GET, "/_view/{index}/{type}/{id}/{format}", this);
    }

    public void handleRequest(final RestRequest request, final RestChannel channel) {
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
                    channel.sendResponse(new StringRestResponse(OK, response.content()));
                } catch (Exception e) {
                    onFailure(e);
                }
            }

            public void onFailure(Throwable e) {
                try {
                    channel.sendResponse(new XContentThrowableRestResponse(request, e));
                } catch (IOException e1) {
                    logger.error("Failed to send failure response", e1);
                }
            }
        });
    }
}
